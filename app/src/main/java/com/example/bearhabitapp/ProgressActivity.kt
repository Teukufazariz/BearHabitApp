package com.example.bearhabitapp

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bearhabitapp.Adapter.HabitProgressAdapter
import com.example.bearhabitapp.Model.Habit
import com.example.bearhabitapp.Model.HabitProgress
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class ProgressActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressAdapter: HabitProgressAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        firestore = FirebaseFirestore.getInstance()

        // Setup RecyclerView
        recyclerView = findViewById(R.id.rvHabitProgress)
        progressAdapter = HabitProgressAdapter { habitProgress ->
            showDeleteConfirmationDialog(habitProgress)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = progressAdapter

        // Setup back button
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        loadWeeklyProgress()
    }

    private fun showDeleteConfirmationDialog(habitProgress: HabitProgress) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Habit")
            .setMessage("Apakah Anda yakin ingin menghapus habit ini?")
            .setPositiveButton("Ya") { dialog, _ ->
                deleteHabit(habitProgress)
                dialog.dismiss()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun deleteHabit(habitProgress: HabitProgress) {
        // Dapatkan userId dari FirebaseAuth
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User tidak terautentikasi", Toast.LENGTH_SHORT).show()
            return
        }

        // Cari Habit berdasarkan habitName dan userId
        firestore.collection("habits")
            .whereEqualTo("habitName", habitProgress.habitName)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Asumsikan hanya satu document yang sesuai
                    val habitDocument = querySnapshot.documents[0]
                    habitDocument.reference.delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Habit berhasil dihapus", Toast.LENGTH_SHORT).show()
                            // Muat ulang data setelah penghapusan
                            loadWeeklyProgress()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal menghapus habit: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Habit tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadWeeklyProgress() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        if (currentUserId == null || currentUserEmail == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val currentWeekStart = getStartOfWeek()
        val currentWeekEnd = getEndOfWeek()

        firestore.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDocument ->
                val userEmail = userDocument.getString("email") ?: ""

                // Fetch user's habits
                firestore.collection("habits")
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .addOnSuccessListener { userHabits ->
                        // Fetch friend's habits (habits that user participates in competitively)
                        firestore.collection("habits")
                            .whereEqualTo("friendEmail", userEmail)
                            .get()
                            .addOnSuccessListener { friendHabits ->
                                val combinedHabits = mutableListOf<Habit>()
                                val habitIds = mutableSetOf<String>()

                                // Add user's habits
                                userHabits.forEach { document ->
                                    val habit = document.toObject(Habit::class.java).apply {
                                        id = document.id
                                    }
                                    if (habit.id != null && habit.id !in habitIds) {
                                        combinedHabits.add(habit)
                                        habitIds.add(habit.id!!)
                                    }
                                }

                                // Add friend's habits if not already added
                                friendHabits.forEach { document ->
                                    val habit = document.toObject(Habit::class.java).apply {
                                        id = document.id
                                    }
                                    if (habit.id != null && habit.id !in habitIds) {
                                        combinedHabits.add(habit)
                                        habitIds.add(habit.id!!)
                                    }
                                }

                                val habitProgressList = mutableListOf<HabitProgress>()
                                val uniqueProgressSet = mutableSetOf<String>() // To track unique HabitProgress entries

                                // Separate non-competitive and competitive habits
                                val competitiveHabits = combinedHabits.filter { it.competitive && (it.friendEmail?.isNotEmpty() ?: false) }
                                val nonCompetitiveHabits = combinedHabits.filter { !it.competitive }

                                // Handle Non-Competitive Habits (Collaborative or single user)
                                nonCompetitiveHabits.forEach { habit ->
                                    if (habit.collaborative) {
                                        // Collaborative habit: progress shared
                                        val totalUsers = 2 // Assuming two users collaborating
                                        val totalTasksInWeek = habit.days.size * totalUsers
                                        var completedTasks = 0

                                        habit.completedDates.forEach { (date, users) ->
                                            if (isDateInCurrentWeek(date, currentWeekStart, currentWeekEnd)) {
                                                completedTasks += users.size
                                            }
                                        }

                                        val progress = if (totalTasksInWeek > 0) {
                                            (completedTasks.toFloat() / totalTasksInWeek.toFloat()) * 100
                                        } else 0f

                                        val progressKey = "${habit.id}_All"
                                        if (progressKey !in uniqueProgressSet) {
                                            habitProgressList.add(
                                                HabitProgress(
                                                    habitName = habit.habitName,
                                                    userEmail = "All",
                                                    progress = progress,
                                                    totalTasks = totalTasksInWeek,
                                                    completedTasks = completedTasks
                                                )
                                            )
                                            uniqueProgressSet.add(progressKey)
                                        }
                                    } else {
                                        // Single-user habit: progress for the current user
                                        val userProgress = calculateUserProgress(
                                            habit,
                                            currentUserId,
                                            currentWeekStart,
                                            currentWeekEnd
                                        )

                                        val progressKey = "${habit.id}_$currentUserId"
                                        if (progressKey !in uniqueProgressSet) {
                                            habitProgressList.add(
                                                HabitProgress(
                                                    habitName = habit.habitName,
                                                    userEmail = currentUserEmail,
                                                    progress = userProgress.second,
                                                    totalTasks = userProgress.first,
                                                    completedTasks = userProgress.third
                                                )
                                            )
                                            uniqueProgressSet.add(progressKey)
                                        }
                                    }
                                }

                                // Handle Competitive Habits (Separate progress for current user and friend)
                                if (competitiveHabits.isNotEmpty()) {
                                    var habitsProcessed = 0
                                    competitiveHabits.forEach { habit ->
                                        val friendEmail = habit.friendEmail!!
                                        // Fetch friend's userId based on friendEmail
                                        firestore.collection("users")
                                            .whereEqualTo("email", friendEmail)
                                            .get()
                                            .addOnSuccessListener { friendDocuments ->
                                                if (!friendDocuments.isEmpty) {
                                                    val friendDoc = friendDocuments.documents[0]
                                                    val friendUserId = friendDoc.id
                                                    val friendUserEmail = friendDoc.getString("email") ?: friendEmail

                                                    // Calculate progress for current user
                                                    val currentUserProgress = calculateUserProgress(
                                                        habit,
                                                        currentUserId,
                                                        currentWeekStart,
                                                        currentWeekEnd
                                                    )

                                                    // Calculate progress for friend user
                                                    val friendProgress = calculateUserProgress(
                                                        habit,
                                                        friendUserId,
                                                        currentWeekStart,
                                                        currentWeekEnd
                                                    )

                                                    // Add progress for current user
                                                    val userProgressKey = "${habit.id}_$currentUserId"
                                                    if (userProgressKey !in uniqueProgressSet) {
                                                        habitProgressList.add(
                                                            HabitProgress(
                                                                habitName = habit.habitName,
                                                                userEmail = currentUserEmail,
                                                                progress = currentUserProgress.second,
                                                                totalTasks = currentUserProgress.first,
                                                                completedTasks = currentUserProgress.third
                                                            )
                                                        )
                                                        uniqueProgressSet.add(userProgressKey)
                                                    }

                                                    // Add progress for friend user
                                                    val friendProgressKey = "${habit.id}_$friendUserId"
                                                    if (friendProgressKey !in uniqueProgressSet) {
                                                        habitProgressList.add(
                                                            HabitProgress(
                                                                habitName = habit.habitName,
                                                                userEmail = friendUserEmail,
                                                                progress = friendProgress.second,
                                                                totalTasks = friendProgress.first,
                                                                completedTasks = friendProgress.third
                                                            )
                                                        )
                                                        uniqueProgressSet.add(friendProgressKey)
                                                    }
                                                } else {
                                                    Log.e("ProgressActivity", "Friend with email $friendEmail not found.")
                                                    Toast.makeText(
                                                        this,
                                                        "Friend with email $friendEmail not found.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }

                                                habitsProcessed++
                                                if (habitsProcessed == competitiveHabits.size) {
                                                    // All competitive habits processed, submit the list
                                                    progressAdapter.submitList(habitProgressList)
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("ProgressActivity", "Error fetching friend userId: ", e)
                                                Toast.makeText(
                                                    this,
                                                    "Error fetching friend data: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                habitsProcessed++
                                                if (habitsProcessed == competitiveHabits.size) {
                                                    // Even if some fail, submit the list
                                                    progressAdapter.submitList(habitProgressList)
                                                }
                                            }
                                    }
                                } else {
                                    // If no competitive habits, submit the list
                                    progressAdapter.submitList(habitProgressList)
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to load friends' habits: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to load user's habits: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateUserProgress(
        habit: Habit,
        userId: String,
        weekStart: Calendar,
        weekEnd: Calendar
    ): Triple<Int, Float, Int> {
        val totalTasksInWeek = habit.days.size * 1 // Each user has their own tasks
        var completedTasks = 0

        habit.completedDates.forEach { (date, users) ->
            if (isDateInCurrentWeek(date, weekStart, weekEnd)) {
                if (users.contains(userId)) {
                    completedTasks += 1
                }
            }
        }

        val progress = if (totalTasksInWeek > 0) {
            (completedTasks.toFloat() / totalTasksInWeek.toFloat()) * 100
        } else 0f

        return Triple(totalTasksInWeek, progress, completedTasks)
    }

    private fun getStartOfWeek(): Calendar {
        return Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun getEndOfWeek(): Calendar {
        return Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
    }

    private fun isDateInCurrentWeek(date: String, weekStart: Calendar, weekEnd: Calendar): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateCalendar = Calendar.getInstance()
        try {
            val parsedDate = sdf.parse(date)
            if (parsedDate != null) {
                dateCalendar.time = parsedDate
                return !dateCalendar.before(weekStart) && !dateCalendar.after(weekEnd)
            }
        } catch (e: Exception) {
            Log.e("ProgressActivity", "Error parsing date: $date", e)
        }
        return false
    }
}