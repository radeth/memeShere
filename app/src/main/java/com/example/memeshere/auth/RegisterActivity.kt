package com.example.memeshere.auth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.memeshere.ProfileActivity
import com.example.memeshere.R
import com.example.memeshere.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        register_button.setOnClickListener {
            performRegister()
        }
        member_button.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

    }

    private fun performRegister() {
        val email = SignUpMail.text.toString()
        val password = SignUpPass.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter text in email/pw", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(LoginActivity.TAG, "Attempting to create user with email: $email")
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener

                // else if successful
                Log.d(LoginActivity.TAG, "Successfully created user with uid: ${it.result?.user?.uid}")
                creteUserWithNickName()
            }
            .addOnFailureListener{
                Log.d(LoginActivity.TAG, "Failed to create user: ${it.message}")
                Toast.makeText(this, "Failed to create user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun creteUserWithNickName() {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        val user = User(uid, nickname.text.toString())

        ref.setValue(user)
            .addOnSuccessListener {
                Log.d(LoginActivity.TAG, "Finally we saved the user to Firebase Database")

                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)

            }
            .addOnFailureListener {
                Log.d(LoginActivity.TAG, "Failed to set value to database: ${it.message}")
            }
    }
}