package com.example.bearhabitapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi Firebase
        FirebaseApp.initializeApp(this)

        // Uji Firestore
        val db = FirebaseFirestore.getInstance()
        val testDoc = hashMapOf("test" to "Firebase Connected!")
        db.collection("test").add(testDoc)
            .addOnSuccessListener { Log.d("FirebaseTest", "Document successfully written!") }
            .addOnFailureListener { e -> Log.w("FirebaseTest", "Error writing document", e) }
    }


}
