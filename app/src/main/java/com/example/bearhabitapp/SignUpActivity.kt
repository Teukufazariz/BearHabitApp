package com.example.bearhabitapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        val etCountry = findViewById<EditText>(R.id.etCountry)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnUploadPicture = findViewById<Button>(R.id.btnUploadPicture)
        val ivProfilePicture = findViewById<ImageView>(R.id.ivProfilePicture)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        // Pilih gambar
        btnUploadPicture.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 100)
        }

        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val fullName = etFullName.text.toString()
            val phoneNumber = etPhoneNumber.text.toString()
            val country = etCountry.text.toString()

            // Validasi Phone Number harus 10 digit
            if (phoneNumber.length < 12) {
                Toast.makeText(this, "Phone Number harus 12 digit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isNotEmpty() && password.isNotEmpty() && fullName.isNotEmpty() && phoneNumber.isNotEmpty() && country.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = task.result.user?.uid
                            val currentUserEmail = task.result.user?.email
                            val storageRef = storage.reference.child("profilePictures/$userId.jpg")
                            storageRef.putFile(imageUri!!)
                                .addOnSuccessListener { uploadTask ->
                                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                                        // Simpan informasi pengguna di Firestore
                                        val userMap = hashMapOf(
                                            "fullName" to fullName,
                                            "phoneNumber" to phoneNumber,
                                            "country" to country,
                                            "email" to email,
                                            "profilePicture" to uri.toString()
                                        )

                                        val currentUser = auth.currentUser
                                        currentUser?.let {
                                            // Simpan userId ke SharedPreferences
                                            val sharedPreferences = getSharedPreferences("User Prefs", MODE_PRIVATE)
                                            sharedPreferences.edit().putString("userId", it.uid).apply()

                                            // Simpan data pengguna ke Firestore
                                            firestore.collection("users").document(it.uid).set(userMap)
                                                .addOnSuccessListener {
                                                    // Navigasi ke ProfileActivity setelah berhasil mendaftar
                                                    startActivity(Intent(this, ProfileActivity::class.java))
                                                    finish()
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }

                        } else {
                            Toast.makeText(this, "Sign Up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            val ivProfilePicture = findViewById<ImageView>(R.id.ivProfilePicture)
            ivProfilePicture.setImageURI(imageUri)
        }
    }
}

