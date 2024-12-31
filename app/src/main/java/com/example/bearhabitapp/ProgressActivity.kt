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

                // Initialize lists for competitive and non-competitive habits
                val habitProgressList = mutableListOf<HabitProgress>()
                val uniqueProgressSet = mutableSetOf<String>()

                // Fetch Competitive Habits where user is creator or friend
                firestore.collection("habits")
                    .whereEqualTo("competitive", true)
                    .get()
                    .addOnSuccessListener { competitiveHabitsSnapshot ->
                        val competitiveHabits = competitiveHabitsSnapshot.documents.filter { doc ->
                            val habit = doc.toObject(Habit::class.java)
                            habit?.userId == currentUserId || habit?.friendEmail == currentUserEmail
                        }

                        // Process Competitive Habits
                        var competitiveHabitsProcessed = 0
                        if (competitiveHabits.isNotEmpty()) {
                            competitiveHabits.forEach { document ->
                                val habit = document.toObject(Habit::class.java)
                                if (habit != null) {
                                    // Calculate progress for creator
                                    val creatorProgress = calculateUserProgress(
                                        habit,
                                        habit.userId,
                                        currentWeekStart,
                                        currentWeekEnd
                                    )

                                    // Get creator's email
                                    firestore.collection("users").document(habit.userId).get()
                                        .addOnSuccessListener { creatorDoc ->
                                            val creatorEmail = creatorDoc.getString("email") ?: ""

                                            // Add creator's progress
                                            val creatorProgressKey = "${habit.id}_${habit.userId}"
                                            if (creatorProgressKey !in uniqueProgressSet) {
                                                habitProgressList.add(
                                                    HabitProgress(
                                                        habitName = habit.habitName,
                                                        userEmail = creatorEmail,
                                                        progress = creatorProgress.second,
                                                        totalTasks = creatorProgress.first,
                                                        completedTasks = creatorProgress.third
                                                    )
                                                )
                                                uniqueProgressSet.add(creatorProgressKey)
                                            }

                                            // Get friend's userId and calculate their progress
                                            firestore.collection("users")
                                                .whereEqualTo("email", habit.friendEmail)
                                                .get()
                                                .addOnSuccessListener { friendDocs ->
                                                    if (!friendDocs.isEmpty) {
                                                        val friendDoc = friendDocs.documents[0]
                                                        val friendUserId = friendDoc.id
                                                        val friendProgress = calculateUserProgress(
                                                            habit,
                                                            friendUserId,
                                                            currentWeekStart,
                                                            currentWeekEnd
                                                        )

                                                        // Add friend's progress
                                                        val friendProgressKey = "${habit.id}_$friendUserId"
                                                        if (friendProgressKey !in uniqueProgressSet) {
                                                            habitProgressList.add(
                                                                HabitProgress(
                                                                    habitName = habit.habitName,
                                                                    userEmail = habit.friendEmail!!,
                                                                    progress = friendProgress.second,
                                                                    totalTasks = friendProgress.first,
                                                                    completedTasks = friendProgress.third
                                                                )
                                                            )
                                                            uniqueProgressSet.add(friendProgressKey)
                                                        }
                                                    }

                                                    competitiveHabitsProcessed++
                                                    if (competitiveHabitsProcessed == competitiveHabits.size) {
                                                        // After processing competitive habits, proceed to fetch non-competitive habits
                                                        fetchNonCompetitiveHabits(
                                                            currentUserId,
                                                            currentUserEmail,
                                                            currentWeekStart,
                                                            currentWeekEnd,
                                                            habitProgressList,
                                                            uniqueProgressSet
                                                        )
                                                    }
                                                }
                                        }
                                } else {
                                    competitiveHabitsProcessed++
                                    if (competitiveHabitsProcessed == competitiveHabits.size) {
                                        // After processing competitive habits, proceed to fetch non-competitive habits
                                        fetchNonCompetitiveHabits(
                                            currentUserId,
                                            currentUserEmail,
                                            currentWeekStart,
                                            currentWeekEnd,
                                            habitProgressList,
                                            uniqueProgressSet
                                        )
                                    }
                                }
                            }
                        } else {
                            // If no competitive habits, proceed to fetch non-competitive habits
                            fetchNonCompetitiveHabits(
                                currentUserId,
                                currentUserEmail,
                                currentWeekStart,
                                currentWeekEnd,
                                habitProgressList,
                                uniqueProgressSet
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to fetch competitive habits: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchNonCompetitiveHabits(
        currentUserId: String,
        currentUserEmail: String,
        currentWeekStart: Calendar,
        currentWeekEnd: Calendar,
        habitProgressList: MutableList<HabitProgress>,
        uniqueProgressSet: MutableSet<String>
    ) {
        // Fetch non-competitive habits where user is creator
        firestore.collection("habits")
            .whereEqualTo("competitive", false)
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { nonCompetitiveHabitsUserSnapshot ->
                // Fetch non-competitive habits where user is a friend (collaborative)
                firestore.collection("habits")
                    .whereEqualTo("competitive", false)
                    .whereEqualTo("friendEmail", currentUserEmail)
                    .get()
                    .addOnSuccessListener { nonCompetitiveHabitsFriendSnapshot ->
                        val nonCompetitiveHabits = nonCompetitiveHabitsUserSnapshot.documents + nonCompetitiveHabitsFriendSnapshot.documents

                        // Process Non-Competitive Habits
                        nonCompetitiveHabits.forEach { document ->
                            val habit = document.toObject(Habit::class.java)?.apply {
                                id = document.id
                            }
                            if (habit != null) {
                                if (habit.collaborative) {
                                    // Collaborative habit logic
                                    val totalUsers = 2
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
                                    // Single-user habit logic
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
                        }

                        // After processing all non-competitive habits, submit the list to adapter
                        progressAdapter.submitList(habitProgressList)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to fetch collaborative habits: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch non-competitive habits: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isDateInCurrentWeek(dateStr: String, start: Calendar, end: Calendar): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val date = sdf.parse(dateStr) ?: return false
            !date.before(start.time) && !date.after(end.time)
        } catch (e: Exception) {
            false
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
}