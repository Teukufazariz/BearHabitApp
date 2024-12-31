package com.example.bearhabitapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bearhabitapp.Adapter.GroupChatAdapter
import com.example.bearhabitapp.Model.GroupChat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var groupChatAdapter: GroupChatAdapter
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        firestore = FirebaseFirestore.getInstance()

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.rvGroupChats)
        recyclerView.layoutManager = LinearLayoutManager(this)

        groupChatAdapter = GroupChatAdapter(
            onGroupChatClick = { groupChat ->
                val intent = Intent(this, ChatDetailActivity::class.java)
                intent.putExtra("HABIT_ID", groupChat.habitId)
                startActivity(intent)
            },
            onDeleteClick = { groupChat ->
                showDeleteConfirmationDialog(groupChat)
            }
        )
        recyclerView.adapter = groupChatAdapter

        // Load group chats for the logged-in user
        loadGroupChats()

        // Setup back button
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }


        // Navigation to other activities
        findViewById<View>(R.id.iv_add).setOnClickListener {
            startActivity(Intent(this, AddHabitActivity::class.java))
        }
        findViewById<View>(R.id.iv_home).setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
        }
    }

    private fun loadGroupChats() {
        val userEmail = FirebaseAuth.getInstance().currentUser!!.email!!

        firestore.collection("group_chats")
            .whereArrayContains("members", userEmail)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val groupChats = querySnapshot.documents.map { document ->
                    GroupChat(
                        habitId = document.getString("habitId")!!,
                        habitName = document.getString("habitName")!!, // Ambil habitName dari Firestore
                        members = document.get("members") as List<String>
                    )
                }

                groupChatAdapter.submitList(groupChats)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load group chats: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog(groupChat: GroupChat) {
        AlertDialog.Builder(this)
            .setTitle("Delete Chat")
            .setMessage("Are you sure you want to delete this chat?")
            .setPositiveButton("Yes") { dialog, _ ->
                deleteGroupChat(groupChat)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun deleteGroupChat(groupChat: GroupChat) {
        // Delete all messages in the group chat
        firestore.collection("group_chats")
            .document(groupChat.habitId)
            .collection("messages")
            .get()
            .addOnSuccessListener { messages ->
                // Delete each message
                messages.forEach { message ->
                    message.reference.delete()
                }

                // After deleting messages, delete the group chat document
                firestore.collection("group_chats")
                    .document(groupChat.habitId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Chat deleted successfully", Toast.LENGTH_SHORT).show()
                        loadGroupChats() // Refresh the list
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to delete chat: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

}