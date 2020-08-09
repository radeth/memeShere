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
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_profile.*
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@RequiresApi(Build.VERSION_CODES.P)
class ProfileActivity : AppCompatActivity() {
    companion object {
        const val RC_PROFILE_IMG = 0
        const val RC_IMG = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sign_out_button.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
        }

        selectphoto_button_register.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, RC_PROFILE_IMG)
        }
        add_image.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, RC_IMG)
        }

        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/")
        val userListener = object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user: User? = dataSnapshot.getValue(User::class.java)
                val nick = user?.username
                nickname.text = "Hello $nick"
                if (user != null) {
                    if (user.profileImg.isNotEmpty()) {
                        reciveProfileImageFromFirbaseAndSetImage(user.uid, user.profileImg)
                    }
                    if (user.memes.isNotEmpty()) {
                        recevieMemesFromFirebaseAndDisplayInGrid(user.uid, user.memes)
                    }
                }
            }


            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("t", "loadPost:onCancelled", databaseError.toException())
            }
        }
        ref.addValueEventListener(userListener)

    }

    private fun recevieMemesFromFirebaseAndDisplayInGrid(uid: String, memes: HashMap<String, String>) {
        val storage = Firebase.storage
        val memesStorageReference =
            storage.getReferenceFromUrl("gs://meme-shere.appspot.com/images").child(uid)
        val memesImages: List<Bitmap> = emptyList()
        for ((key, value) in memes) {
            val downloadSizeLimit: Long = 5242880
            val memeImageRef = memesStorageReference.child(value)
            memeImageRef.getBytes(downloadSizeLimit).addOnSuccessListener { bytes ->
                memesImages.plus(bytes)
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load image: ${it.message}", Toast.LENGTH_SHORT)
            }
        }
        println(memesImages)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_PROFILE_IMG && resultCode == Activity.RESULT_OK && data != null) {
            val selectedPhotoUri: Uri? = data.data
            val source = ImageDecoder.createSource(contentResolver, selectedPhotoUri!!)
            val bitmap = ImageDecoder.decodeBitmap(source)
            setImage(bitmap)
            saveProfileImageToStorage(bitmap)
        }
        if (requestCode == RC_IMG && resultCode == Activity.RESULT_OK && data != null) {
            val selectedPhotoUri: Uri? = data.data
            val source = ImageDecoder.createSource(contentResolver, selectedPhotoUri!!)
            val bitmap = ImageDecoder.decodeBitmap(source)
            saveImageToStorage(bitmap)
        }
    }


    @SuppressLint("ShowToast")
    private fun reciveProfileImageFromFirbaseAndSetImage(uid: String, fileName: String) {
        val storage = Firebase.storage
        val profileImgReference = storage.getReferenceFromUrl("gs://meme-shere.appspot.com/images").child(uid)
                .child(fileName)
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

    private fun saveProfileImageToStorage(bitmap: Bitmap) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().reference
        val profileImgRef = ref.child("images").child(uid).child(filename)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        profileImgRef.putBytes(data).addOnSuccessListener {
            assignImageToUser(filename, uid)
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to save image: ${it.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun saveImageToStorage(bitmap: Bitmap) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().reference
        val imagesRef = ref.child("images").child(uid).child(filename)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        imagesRef.putBytes(data).addOnSuccessListener {
            assignMemeToUser(filename, uid)
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to save image: ${it.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun assignMemeToUser(filename: String, uid: String) {
        val ref = FirebaseDatabase.getInstance().reference
        val memesRef = ref.child("users").child(uid).child("memes")
        memesRef.push().setValue(filename).addOnSuccessListener {
            Toast.makeText(this, "Successful to save image", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to save image: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun assignImageToUser(filename: String, uid: String) {
        val ref = FirebaseDatabase.getInstance().reference
        val usersRef = ref.child("users").child(uid).child("profileImg")
        usersRef.setValue(filename).addOnSuccessListener {
            Toast.makeText(this, "Successful to save profile image", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to save profile image: ${it.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }
}

