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
import java.io.File
import java.util.*

class ProfileActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
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

        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/")
        val userListener = object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                val user: User? = dataSnapshot.getValue(User::class.java)
                val nick = user?.username
                nickname.text = "Hello $nick"
                val file = Uri.fromFile(File(user?.profileImg))
//                setImage(file)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("t", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        }
        ref.addValueEventListener(userListener)
        val storage = Firebase.storage
        val storageRef = storage.reference
        val profileImgReference = storageRef.child("images/test.jpeg")
        val ONE_MEGABYTE: Long = 1024 * 1024
        profileImgReference.getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes ->
            val profileImgBitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            setImage(profileImgBitmap)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            val selectedPhotoUri: Uri? = data.data
            val source = ImageDecoder.createSource(contentResolver, selectedPhotoUri!!)
            val bitmap = ImageDecoder.decodeBitmap(source)
            setImage(bitmap)
            saveImageToStorage(selectedPhotoUri)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setImage(bitmap: Bitmap) {

        selectphoto_imageview_register.setImageBitmap(bitmap)

        selectphoto_button_register.alpha = 0f
    }

    private fun saveImageToStorage(selectedPhotoUri: Uri?) {
        // Todo przerobić zapisywanie scieżeki do storage na  taką val profileImgReference = storageRef.child("images/test.jpeg")
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener { it ->
                ref.downloadUrl.addOnSuccessListener {
                    assignImageToUser(it.toString())
                }
            }
            .addOnFailureListener {

            }
    }

    private fun assignImageToUser(fileLocation: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/profileImg")

        ref.setValue(fileLocation)
            .addOnSuccessListener {
                Toast.makeText(this, "Successful to save image", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save image: ${it.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}

