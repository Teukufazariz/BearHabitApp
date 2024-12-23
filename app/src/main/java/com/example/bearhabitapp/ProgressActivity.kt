package com.example.bearhabitapp

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bearhabitapp.Adapter.HabitProgressAdapter
import com.example.bearhabitapp.Model.Habit
import com.example.bearhabitapp.Model.HabitProgress
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
        recyclerView.layoutManager = LinearLayoutManager(this)
        progressAdapter = HabitProgressAdapter { habitProgress ->
            showDeleteConfirmationDialog(habitProgress)
        }
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
            .setMessage("Apakah Anda yakin ingin menghapus habit '${habitProgress.habitName}' secara permanen?")
            .setPositiveButton("Ya") { dialog, _ ->
                deleteHabit(habitProgress.habitId)
                dialog.dismiss()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun deleteHabit(habitId: String) {
        // Menghapus habit dari koleksi "habits"
        firestore.collection("habits").document(habitId)
            .delete()
            .addOnSuccessListener {
                // Menghapus group chat terkait dari koleksi "group_chats"
                firestore.collection("group_chats").document(habitId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Habit dan group chat terkait berhasil dihapus", Toast.LENGTH_SHORT).show()
                        // Menghapus habit dari daftar adapter
                        val currentList = progressAdapter.currentList.toMutableList()
                        val habitToRemove = currentList.find { it.habitId == habitId }
                        if (habitToRemove != null) {
                            currentList.remove(habitToRemove)
                            progressAdapter.submitList(currentList)
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Habit berhasil dihapus, tetapi gagal menghapus group chat: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus habit: ${e.message}", Toast.LENGTH_SHORT).show()
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
                val userEmailFetched = userDocument.getString("email") ?: ""

                // Fetch user's habits
                firestore.collection("habits")
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .addOnSuccessListener { userHabits ->
                        // Fetch friend's habits (habits that user participates in competitively)
                        firestore.collection("habits")
                            .whereEqualTo("friendEmail", userEmailFetched)
                            .get()
                            .addOnSuccessListener { friendHabits ->
                                val habitProgressList = mutableListOf<HabitProgress>()
                                val uniqueProgressSet = mutableSetOf<String>() // To track unique HabitProgress entries

                                // Process User's Habits
                                val userHabitsProcessed = userHabits.size()
                                var userHabitsCompleted = 0

                                if (userHabits.isEmpty) {
                                    // No user habits to process
                                    processFriendHabits(
                                        friendHabits,
                                        habitProgressList,
                                        uniqueProgressSet,
                                        currentUserId,
                                        currentUserEmail,
                                        currentWeekStart,
                                        currentWeekEnd
                                    )
                                } else {
                                    userHabits.forEach { document ->
                                        val habit = document.toObject(Habit::class.java).apply {
                                            id = document.id
                                        }

                                        if (habit.competitive && !habit.friendEmail.isNullOrEmpty()) {
                                            // Competitive Habit: Add progress for user and friend
                                            createCompetitiveProgress(
                                                habit,
                                                currentUserId,
                                                currentUserEmail,
                                                currentWeekStart,
                                                currentWeekEnd,
                                                habitProgressList,
                                                uniqueProgressSet
                                            ) {
                                                userHabitsCompleted++
                                                if (userHabitsCompleted == userHabitsProcessed) {
                                                    processFriendHabits(
                                                        friendHabits,
                                                        habitProgressList,
                                                        uniqueProgressSet,
                                                        currentUserId,
                                                        currentUserEmail,
                                                        currentWeekStart,
                                                        currentWeekEnd
                                                    )
                                                }
                                            }
                                        } else {
                                            // Non-Competitive Habit
                                            createNonCompetitiveProgress(
                                                habit,
                                                currentUserId,
                                                currentUserEmail,
                                                currentWeekStart,
                                                currentWeekEnd,
                                                habitProgressList,
                                                uniqueProgressSet
                                            ) {
                                                userHabitsCompleted++
                                                if (userHabitsCompleted == userHabitsProcessed) {
                                                    processFriendHabits(
                                                        friendHabits,
                                                        habitProgressList,
                                                        uniqueProgressSet,
                                                        currentUserId,
                                                        currentUserEmail,
                                                        currentWeekStart,
                                                        currentWeekEnd
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Failed to load friends' habits: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Failed to load user's habits: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to fetch user data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun processFriendHabits(
        friendHabits: com.google.firebase.firestore.QuerySnapshot,
        habitProgressList: MutableList<HabitProgress>,
        uniqueProgressSet: MutableSet<String>,
        currentUserId: String,
        currentUserEmail: String?,
        currentWeekStart: Calendar,
        currentWeekEnd: Calendar
    ) {
        val friendHabitsProcessed = friendHabits.size()
        var friendHabitsCompleted = 0

        if (friendHabits.isEmpty) {
            // No friend habits to process
            progressAdapter.submitList(habitProgressList)
            return
        }

        friendHabits.forEach { document ->
            val habit = document.toObject(Habit::class.java).apply {
                id = document.id
            }

            if (habit.competitive && !habit.friendEmail.isNullOrEmpty()) {
                // Competitive Habit: Only add progress for friend to avoid duplication
                createFriendCompetitiveProgress(
                    habit,
                    currentUserId,
                    currentUserEmail,
                    currentWeekStart,
                    currentWeekEnd,
                    habitProgressList,
                    uniqueProgressSet
                ) {
                    friendHabitsCompleted++
                    if (friendHabitsCompleted == friendHabitsProcessed) {
                        progressAdapter.submitList(habitProgressList)
                    }
                }
            } else {
                // Non-Competitive Habit
                createNonCompetitiveProgress(
                    habit,
                    currentUserId,
                    currentUserEmail,
                    currentWeekStart,
                    currentWeekEnd,
                    habitProgressList,
                    uniqueProgressSet
                ) {
                    friendHabitsCompleted++
                    if (friendHabitsCompleted == friendHabitsProcessed) {
                        progressAdapter.submitList(habitProgressList)
                    }
                }
            }
        }
    }

    private fun createCompetitiveProgress(
        habit: Habit,
        currentUserId: String,
        currentUserEmail: String?,
        weekStart: Calendar,
        weekEnd: Calendar,
        habitProgressList: MutableList<HabitProgress>,
        uniqueProgressSet: MutableSet<String>,
        completionCallback: () -> Unit
    ) {
        // Add progress for current user
        val userProgress = calculateUserProgress(habit, currentUserId, weekStart, weekEnd)
        val userProgressKey = "${habit.id}_$currentUserId"
        if (userProgressKey !in uniqueProgressSet) {
            habitProgressList.add(
                HabitProgress(
                    habitId = habit.id!!, // Menambahkan habitId
                    habitName = habit.habitName,
                    userEmail = currentUserEmail ?: "N/A",
                    progress = userProgress.second,
                    totalTasks = userProgress.first,
                    completedTasks = userProgress.third
                )
            )
            uniqueProgressSet.add(userProgressKey)
        }

        // Add progress for friend
        val friendEmail = habit.friendEmail!!
        checkFriendEmailExists(friendEmail) { exists, friendUserId, friendUserEmail ->
            if (exists && friendUserId != null && friendUserEmail != null) {
                val friendProgress = calculateUserProgress(habit, friendUserId, weekStart, weekEnd)
                val friendProgressKey = "${habit.id}_$friendUserId"
                if (friendProgressKey !in uniqueProgressSet) {
                    habitProgressList.add(
                        HabitProgress(
                            habitId = habit.id!!, // Menambahkan habitId
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
                Toast.makeText(this, "Friend's email is not registered", Toast.LENGTH_SHORT).show()
            }
            completionCallback()
        }
    }

    private fun createFriendCompetitiveProgress(
        habit: Habit,
        currentUserId: String,
        currentUserEmail: String?,
        weekStart: Calendar,
        weekEnd: Calendar,
        habitProgressList: MutableList<HabitProgress>,
        uniqueProgressSet: MutableSet<String>,
        completionCallback: () -> Unit
    ) {
        // Only add progress for the friend
        val friendEmail = habit.friendEmail!!
        checkFriendEmailExists(friendEmail) { exists, friendUserId, friendUserEmail ->
            if (exists && friendUserId != null && friendUserEmail != null) {
                // Avoid adding progress for the current user again
                if (friendUserId != currentUserId) {
                    val friendProgress = calculateUserProgress(habit, friendUserId, weekStart, weekEnd)
                    val friendProgressKey = "${habit.id}_$friendUserId"
                    if (friendProgressKey !in uniqueProgressSet) {
                        habitProgressList.add(
                            HabitProgress(
                                habitId = habit.id!!, // Menambahkan habitId
                                habitName = habit.habitName,
                                userEmail = friendUserEmail,
                                progress = friendProgress.second,
                                totalTasks = friendProgress.first,
                                completedTasks = friendProgress.third
                            )
                        )
                        uniqueProgressSet.add(friendProgressKey)
                    }
                }
            } else {
                Toast.makeText(this, "Friend's email is not registered", Toast.LENGTH_SHORT).show()
            }
            completionCallback()
        }
    }

    private fun createNonCompetitiveProgress(
        habit: Habit,
        currentUserId: String,
        currentUserEmail: String?,
        weekStart: Calendar,
        weekEnd: Calendar,
        habitProgressList: MutableList<HabitProgress>,
        uniqueProgressSet: MutableSet<String>,
        completionCallback: () -> Unit
    ) {
        if (habit.collaborative) {
            // Collaborative habit: progress shared
            val totalUsers = 2 // Assuming two users collaborating
            val totalTasksInWeek = habit.days.size * totalUsers
            var completedTasks = 0

            habit.completedDates.forEach { (date, users) ->
                if (isDateInCurrentWeek(date, weekStart, weekEnd)) {
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
                        habitId = habit.id!!, // Menambahkan habitId
                        habitName = habit.habitName,
                        userEmail = "All",
                        progress = progress,
                        totalTasks = totalTasksInWeek,
                        completedTasks = completedTasks
                    )
                )
                uniqueProgressSet.add(progressKey)
            }
            completionCallback()
        } else {
            val userProgress = calculateUserProgress(
                habit,
                currentUserId,
                weekStart,
                weekEnd
            )

            val progressKey = "${habit.id}_$currentUserId"
            if (progressKey !in uniqueProgressSet) {
                habitProgressList.add(
                    HabitProgress(
                        habitId = habit.id!!, // Menambahkan habitId
                        habitName = habit.habitName,
                        userEmail = currentUserEmail ?: "N/A",
                        progress = userProgress.second,
                        totalTasks = userProgress.first,
                        completedTasks = userProgress.third
                    )
                )
                uniqueProgressSet.add(progressKey)
            }
            completionCallback()
        }
    }

    private fun checkFriendEmailExists(
        email: String,
        callback: (exists: Boolean, friendUserId: String?, friendUserEmail: String?) -> Unit
    ) {
        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val friendDoc = querySnapshot.documents[0]
                    val friendUserId = friendDoc.id
                    val friendUserEmail = friendDoc.getString("email")
                    callback(true, friendUserId, friendUserEmail)
                } else {
                    callback(false, null, null)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error checking email: ${exception.message}", Toast.LENGTH_SHORT).show()
                callback(false, null, null)
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

        habit.completedDates.forEach { (dateStr, users) ->
            if (isDateInCurrentWeek(dateStr, weekStart, weekEnd)) {
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

    private fun isDateInCurrentWeek(dateStr: String, weekStart: Calendar, weekEnd: Calendar): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date: Date? = sdf.parse(dateStr)
        if (date == null) return false
        val calendar = Calendar.getInstance()
        calendar.time = date
        return !calendar.before(weekStart) && !calendar.after(weekEnd)
    }

    private fun getStartOfWeek(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }

    private fun getEndOfWeek(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar
    }
}