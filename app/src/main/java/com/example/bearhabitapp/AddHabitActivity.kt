package com.example.bearhabitapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.bearhabitapp.Model.GroupChat
import com.example.bearhabitapp.Model.Habit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AddHabitActivity : AppCompatActivity() {

    private lateinit var selectedColor: String
    private lateinit var colorViews: List<ImageView>
    private lateinit var firestore: FirebaseFirestore

    // Checkbox untuk hari
    private lateinit var cbMonday: CheckBox
    private lateinit var cbTuesday: CheckBox
    private lateinit var cbWednesday: CheckBox
    private lateinit var cbThursday: CheckBox
    private lateinit var cbFriday: CheckBox
    private lateinit var cbSaturday: CheckBox
    private lateinit var cbSunday: CheckBox

    // Field untuk email teman
    private lateinit var etFriendEmail: EditText
    private lateinit var btnBack: ImageView

    // RadioButtons untuk jenis habit
    private lateinit var rbCollaborative: RadioButton
    private lateinit var rbCompetitive: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)

        firestore = FirebaseFirestore.getInstance()

        val ivRed = findViewById<ImageView>(R.id.ivRed)
        val ivGreen = findViewById<ImageView>(R.id.ivGreen)
        val ivBlue = findViewById<ImageView>(R.id.ivBlue)
        val ivYellow = findViewById<ImageView>(R.id.ivYellow)
        val ivPurple = findViewById<ImageView>(R.id.ivPurple)

        colorViews = listOf(ivRed, ivGreen, ivBlue, ivYellow, ivPurple)

        // Default color
        selectedColor = "#F4A5B7"
        selectColor(selectedColor, ivRed) // Set default selection

        // Set click listeners untuk warna
        ivRed.setOnClickListener { selectColor("#F4A5B7", it) }
        ivGreen.setOnClickListener { selectColor("#A5F4CE", it) }
        ivBlue.setOnClickListener { selectColor("#8BC8FF", it) }
        ivYellow.setOnClickListener { selectColor("#F2FF93", it) }
        ivPurple.setOnClickListener { selectColor("#ACB7F5", it) }

        // Initialize checkboxes untuk hari
        cbMonday = findViewById(R.id.cbMonday)
        cbTuesday = findViewById(R.id.cbTuesday)
        cbWednesday = findViewById(R.id.cbWednesday)
        cbThursday = findViewById(R.id.cbThursday)
        cbFriday = findViewById(R.id.cbFriday)
        cbSaturday = findViewById(R.id.cbSaturday)
        cbSunday = findViewById(R.id.cbSunday)

        // Friend email input field
        etFriendEmail = findViewById(R.id.etFriendEmail)

        // RadioButtons untuk jenis habit
        rbCollaborative = findViewById(R.id.rbCollaborative)
        rbCompetitive = findViewById(R.id.rbCompetitive)

        // Setup back button
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        findViewById<View>(R.id.btnAddHabit).setOnClickListener {
            addHabit()
        }
    }

    private fun selectColor(color: String, selectedView: View) {
        selectedColor = color

        // Reset all colors to default alpha
        colorViews.forEach { it.alpha = 0.5f }

        // Highlight selected color
        selectedView.alpha = 1.0f
    }

    private fun addHabit() {
        val habitName = findViewById<EditText>(R.id.etHabitName).text.toString().trim()
        val repeatCount = findViewById<EditText>(R.id.etRepeatCount).text.toString().toIntOrNull() ?: 0

        if (habitName.isEmpty() || repeatCount <= 0) {
            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show()
            return
        }

        // Get selected days
        val selectedDays = getSelectedDays()
        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show()
            return
        }

        // Get friend's email
        val friendEmail = findViewById<EditText>(R.id.etFriendEmail).text.toString().trim()

        // Determine jenis habit
        val isCollaborative = rbCollaborative.isChecked
        val isCompetitive = rbCompetitive.isChecked

        when {
            isCollaborative && friendEmail.isEmpty() -> {
                Toast.makeText(this, "Please provide friend's email for collaborative habit", Toast.LENGTH_SHORT).show()
                return
            }
            isCompetitive && friendEmail.isEmpty() -> {
                Toast.makeText(this, "Please provide friend's email for competitive habit", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (isCompetitive && friendEmail.isNotEmpty()) {
            // Check if the friend email exists in the database
            checkFriendEmailExists(friendEmail) { exists ->
                if (exists) {
                    createCompetitiveHabit(habitName, selectedColor, repeatCount, selectedDays, friendEmail)
                } else {
                    Toast.makeText(this, "Friend's email is not registered", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (isCollaborative && friendEmail.isNotEmpty()) {
            // Check if the friend email exists in the database
            checkFriendEmailExists(friendEmail) { exists ->
                if (exists) {
                    saveCollaborativeHabit(habitName, selectedColor, repeatCount, selectedDays, friendEmail, isCollaborative)
                } else {
                    Toast.makeText(this, "Friend's email is not registered", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Non-collaborative and non-competitive habit
            saveHabitToFirestore(habitName, selectedColor, repeatCount, selectedDays, null, false, false)
        }
    }

    private fun checkFriendEmailExists(email: String, callback: (Boolean) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Jika hasil query tidak kosong, berarti email terdaftar
                callback(!querySnapshot.isEmpty)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error checking email: ${exception.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    private fun getSelectedDays(): List<String> {
        val days = mutableListOf<String>()
        if (cbMonday.isChecked) days.add("Monday")
        if (cbTuesday.isChecked) days.add("Tuesday")
        if (cbWednesday.isChecked) days.add("Wednesday")
        if (cbThursday.isChecked) days.add("Thursday")
        if (cbFriday.isChecked) days.add("Friday")
        if (cbSaturday.isChecked) days.add("Saturday")
        if (cbSunday.isChecked) days.add("Sunday")
        return days
    }

    private fun createGroupChat(habitId: String, habitName: String, friendEmail: String?) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        if (userEmail == null) {
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show()
            return
        }

        val members = mutableListOf<String>()
        members.add(userEmail)
        friendEmail?.let {
            if (it.isNotEmpty()) {
                members.add(it)
            }
        }

        val groupChat = GroupChat(
            habitId = habitId,
            habitName = habitName,
            members = members
        )

        FirebaseFirestore.getInstance().collection("group_chats")
            .document(habitId)
            .set(groupChat)
            .addOnSuccessListener {
                Toast.makeText(this, "Group chat created successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to create group chat: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveHabitToFirestore(
        habitName: String,
        iconColor: String,
        repeatCount: Int,
        days: List<String>,
        friendEmail: String?,
        collaborative: Boolean,
        competitive: Boolean
    ) {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val userEmail = FirebaseAuth.getInstance().currentUser!!.email!!

        val habit = Habit(
            userId = userId,
            habitName = habitName,
            iconColor = iconColor,
            repeatCount = repeatCount,
            timestamp = System.currentTimeMillis(),
            days = days,
            friendEmail = friendEmail,
            collaborative = collaborative,
            competitive = competitive
        )

        firestore.collection("habits")
            .add(habit)
            .addOnSuccessListener { documentRef ->
                createGroupChat(documentRef.id, habitName, friendEmail)
                if (competitive && friendEmail != null) {
                    // Membuat habit untuk friend
                    firestore.collection("users")
                        .whereEqualTo("email", friendEmail)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                val friendDoc = querySnapshot.documents[0]
                                val friendUserId = friendDoc.id
                                val friendUserEmail = friendDoc.getString("email") ?: friendEmail

                                val friendHabit = Habit(
                                    userId = friendUserId,
                                    habitName = habitName,
                                    iconColor = iconColor,
                                    repeatCount = repeatCount,
                                    timestamp = System.currentTimeMillis(),
                                    days = days,
                                    friendEmail = userEmail, // Menghubungkan kembali ke user A
                                    collaborative = false,
                                    competitive = true
                                )

                                firestore.collection("habits")
                                    .add(friendHabit)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Competitive Habit added successfully", Toast.LENGTH_SHORT).show()

                                        // Navigate to HomePageActivity
                                        val intent = Intent(this, HomePageActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish() // Close AddHabitActivity
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(this, "Failed to add friend's habit: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this, "Friend with email $friendEmail not found.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error fetching friend userId: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Habit Non-Kolaboratif dan Non-Kompetitif
                    Toast.makeText(this, "Habit added successfully", Toast.LENGTH_SHORT).show()

                    // Navigate to HomePageActivity
                    val intent = Intent(this, HomePageActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish() // Close AddHabitActivity
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to add habit: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveCollaborativeHabit(
        habitName: String,
        iconColor: String,
        repeatCount: Int,
        days: List<String>,
        friendEmail: String,
        collaborative: Boolean
    ) {
        // Implementasikan jika perlu khusus untuk kolaborasi
        // Misalnya, membuat group chat atau lain-lain
        // Pada contoh ini, kita akan menggunakan metode saveHabitToFirestore secara umum
        saveHabitToFirestore(habitName, iconColor, repeatCount, days, friendEmail, collaborative, false)
    }

    private fun createCompetitiveHabit(
        habitName: String,
        iconColor: String,
        repeatCount: Int,
        days: List<String>,
        friendEmail: String
    ) {
        // Tandai habit ini sebagai kompetitif
        saveHabitToFirestore(habitName, iconColor, repeatCount, days, friendEmail, false, true)
    }

    private fun saveHabitForFriend(
        habitName: String,
        iconColor: String,
        repeatCount: Int,
        days: List<String>,
        friendUserId: String,
        friendUserEmail: String
    ) {
        // Tidak digunakan karena sudah di-handle dalam saveHabitToFirestore()
    }
}