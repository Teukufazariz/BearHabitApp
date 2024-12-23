package com.example.bearhabitapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvFullName: TextView
    private lateinit var tvPhoneNumber: TextView
    private lateinit var tvCountry: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnBack: ImageView

    private lateinit var firestore: FirebaseFirestore

    // User ID akan diambil dari Firestore
    private var userId: String? = null

    companion object {
        private const val REQUEST_CODE_EDIT = 1
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_EDIT && resultCode == Activity.RESULT_OK) {
            val fieldToEdit = data?.getStringExtra("fieldToEdit")
            fieldToEdit?.let {
                showEditDialog(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Inisialisasi TextView dan Firestore
        tvFullName = findViewById(R.id.tvFullName)
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber)
        tvCountry = findViewById(R.id.tvCountry)
        tvEmail = findViewById(R.id.tvEmail)

        // Tambahkan inisialisasi untuk tombol Logout
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        firestore = FirebaseFirestore.getInstance()

        // Ambil userId dari SharedPreferences
        val sharedPreferences = getSharedPreferences("User Prefs", MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", null)

        // Muat data pengguna jika userId tidak null
        if (userId != null) {
            loadUserData() // Memuat data pengguna setelah mendapatkan userId
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
        }

        // Button navigation setup
        findViewById<View>(R.id.iv_add).setOnClickListener {
            startActivity(Intent(this, AddHabitActivity::class.java))
        }

        findViewById<View>(R.id.iv_home).setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
        }

        findViewById<View>(R.id.iv_chat).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        // Listener untuk mengedit data pengguna
        tvFullName.setOnClickListener {
            showEditDialog("fullName")
        }
        tvPhoneNumber.setOnClickListener {
            showEditDialog("phoneNumber")
        }
        tvCountry.setOnClickListener {
            showEditDialog("country")
        }

        // Setup back button
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        // Listener untuk tombol Logout
        btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun loadUserData() {
        if (userId == null) {
            Toast.makeText(this, "User ID not available", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("ProfileActivity", "Loading data for userId: $userId")

        firestore.collection("users").document(userId!!).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d("FirestoreData", "Full Name: ${document.getString("fullName")}")
                    tvFullName.text = document.getString("fullName") ?: "N/A"
                    tvPhoneNumber.text = document.getString("phoneNumber") ?: "N/A"
                    tvCountry.text = document.getString("country") ?: "N/A"
                    tvEmail.text = document.getString("email") ?: "N/A"
                } else {
                    Toast.makeText(this, "No such user", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load user data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditDialog(fieldToEdit: String) {
        val editText = EditText(this)
        editText.hint = when (fieldToEdit) {
            "fullName" -> "Nama Lengkap Baru"
            "phoneNumber" -> "Nomor Telepon Baru"
            "country" -> "Negara Baru"
            else -> ""
        }

        AlertDialog.Builder(this)
            .setTitle("Update $fieldToEdit")
            .setView(editText)
            .setPositiveButton("Update") { _, _ ->
                val newValue = editText.text.toString().trim()
                if (newValue.isNotEmpty()) {
                    if (fieldToEdit == "phoneNumber" && !isValidPhoneNumber(newValue)) {
                        Toast.makeText(this, "Nomor Telepon tidak valid", Toast.LENGTH_SHORT).show()
                    } else {
                        updateFieldInFirestore(fieldToEdit, newValue)
                    }
                } else {
                    Toast.makeText(this, "Field cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.length >= 10 && phone.all { it.isDigit() }
    }

    private fun updateFieldInFirestore(fieldToEdit: String, newValue: String) {
        if (userId == null) {
            Toast.makeText(this, "User ID not available", Toast.LENGTH_SHORT).show()
            return
        }

        val userMap: MutableMap<String, Any> = hashMapOf(fieldToEdit to newValue)

        firestore.collection("users").document(userId!!).update(userMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "$fieldToEdit updated successfully", Toast.LENGTH_SHORT).show()
                    loadUserData() // Memuat ulang data pengguna
                } else {
                    Toast.makeText(this, "Failed to update $fieldToEdit", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Metode untuk logout pengguna
    private fun logoutUser() {
        val auth = FirebaseAuth.getInstance()
        auth.signOut()

        // Hapus data user dari SharedPreferences
        val sharedPreferences = getSharedPreferences("User Prefs", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        // Kembali ke halaman login
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

}
