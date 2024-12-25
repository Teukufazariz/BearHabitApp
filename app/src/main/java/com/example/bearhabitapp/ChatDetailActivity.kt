package com.example.bearhabitapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bearhabitapp.Adapter.MessageAdapter
import com.example.bearhabitapp.Model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ChatDetailActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var ivSendMessage: ImageView
    private lateinit var ivSendPhoto: ImageView
    private lateinit var habitId: String
    private lateinit var tvGroupName: TextView

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_detail)

        // Initialize Firebase components
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize views
        recyclerView = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        ivSendMessage = findViewById(R.id.ivSendMessage)
        ivSendPhoto = findViewById(R.id.ivSendPhoto)
        tvGroupName = findViewById(R.id.tvGroupName)

        // Get habitId from intent
        habitId = intent.getStringExtra("HABIT_ID") ?: ""
        if (habitId.isEmpty()) {
            Toast.makeText(this, "Error: Habit ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messageAdapter = MessageAdapter()
        recyclerView.adapter = messageAdapter

        // Load existing messages and group name
        loadMessages()
        loadGroupName()

        // Setup click listeners
        ivSendMessage.setOnClickListener { sendMessage() }
        ivSendPhoto.setOnClickListener { checkCameraPermission() }

        // Setup back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            val filePath = saveBitmapToFile(imageBitmap)
            uploadImageToFirebaseStorage(filePath)
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)

        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        return imageFile.absolutePath
    }

    private fun uploadImageToFirebaseStorage(imagePath: String) {
        val file = File(imagePath)
        val imageRef = storage.reference.child("chat_images/${System.currentTimeMillis()}_${file.name}")

        val uploadTask = imageRef.putFile(Uri.fromFile(file))

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                sendImageMessage(uri.toString())
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendImageMessage(imageUrl: String) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Anonymous"
        val timestamp = System.currentTimeMillis()

        val message = hashMapOf(
            "sender" to userEmail,
            "text" to "[Image]",
            "imageUrl" to imageUrl,
            "timestamp" to timestamp,
            "type" to "image"
        )

        firestore.collection("group_chats")
            .document(habitId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error sending image: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMessages() {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        firestore.collection("group_chats")
            .document(habitId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Toast.makeText(this, "Error loading messages: ${exception.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.map { document ->
                    val sender = document.getString("sender") ?: "Unknown"
                    Message(
                        sender = sender,
                        text = document.getString("text") ?: "",
                        timestamp = document.getLong("timestamp") ?: System.currentTimeMillis(),
                        imageUrl = document.getString("imageUrl"),
                        type = document.getString("type") ?: "text",
                        isSender = sender == currentUserEmail
                    )
                } ?: emptyList()

                messageAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun loadGroupName() {
        firestore.collection("group_chats")
            .document(habitId)
            .get()
            .addOnSuccessListener { document ->
                val groupName = document.getString("habitName") ?: "Unknown Group"
                tvGroupName.text = groupName
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading group name: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendMessage() {
        val messageText = etMessage.text.toString().trim()
        if (messageText.isNotEmpty()) {
            val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Anonymous"
            val timestamp = System.currentTimeMillis()

            val message = hashMapOf(
                "sender" to userEmail,
                "text" to messageText,
                "timestamp" to timestamp,
                "type" to "text"
            )

            firestore.collection("group_chats")
                .document(habitId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener {
                    etMessage.text.clear()
                    recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error sending message: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }
}
