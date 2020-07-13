package com.example.memeshere

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.memeshere.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_profile.*
import java.io.ByteArrayOutputStream
import java.util.*

@RequiresApi(Build.VERSION_CODES.P)
class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sign_out_button.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
        }

        selectphoto_button_register.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
        add_image.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/")
        val userListener = object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                val user: User? = dataSnapshot.getValue(User::class.java)

                val nick = user?.username
                nickname.text = "Hello $nick"
                user?.profileImg?.let { reciveImageFromFirbaseAndSetImage(it) }
            }


            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("t", "loadPost:onCancelled", databaseError.toException())
            }
        }
        ref.addValueEventListener(userListener)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            val selectedPhotoUri: Uri? = data.data
            val source = ImageDecoder.createSource(contentResolver, selectedPhotoUri!!)
            val bitmap = ImageDecoder.decodeBitmap(source)
            setImage(bitmap)
            saveImageToStorage(bitmap)
        }
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            val selectedPhotoUri: Uri? = data.data
            val source = ImageDecoder.createSource(contentResolver, selectedPhotoUri!!)
            val bitmap = ImageDecoder.decodeBitmap(source)
            setImage(bitmap)
            saveImageToStorage(bitmap)
        }
    }


    @SuppressLint("ShowToast")
    private fun reciveImageFromFirbaseAndSetImage(fileName: String) {
        val storage = Firebase.storage
        val profileImgReference =
            storage.getReferenceFromUrl("gs://meme-shere.appspot.com/images").child(fileName)
        val downloadSizeLimit: Long = 5242880
        profileImgReference.getBytes(downloadSizeLimit).addOnSuccessListener { bytes ->
            val profileImgBitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            setImage(profileImgBitmap)
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load image: ${it.message}", Toast.LENGTH_SHORT)
        }
    }


    @RequiresApi(Build.VERSION_CODES.P)
    private fun setImage(bitmap: Bitmap) {

        selectphoto_imageview_register.setImageBitmap(bitmap)

        selectphoto_button_register.alpha = 0f
    }

    private fun saveImageToStorage(bitmap: Bitmap) {
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().reference
        val imagesRef = ref.child("images").child(filename)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        imagesRef.putBytes(data).addOnSuccessListener {
            assignImageToUser(filename)
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to save image: ${it.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun assignImageToUser(fileName: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().reference
        val usersRef = ref.child("users").child(uid).child("profileImg")
        usersRef.setValue(fileName).addOnSuccessListener {
            Toast.makeText(this, "Successful to save image", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to save image: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

