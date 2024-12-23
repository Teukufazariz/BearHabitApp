package com.example.bearhabitapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bearhabitapp.Adapter.HabitAdapter
import com.example.bearhabitapp.Model.Habit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class HomePageActivity : AppCompatActivity() {

    private lateinit var recyclerViewHabits: RecyclerView
    private lateinit var calendarView: CalendarView
    private lateinit var habitAdapter: HabitAdapter
    private lateinit var firestore: FirebaseFirestore
    private val habitList = mutableListOf<Habit>()
    private var selectedDay: String = getCurrentDay()
    private var selectedDate: String = getCurrentDate()

    // UI elements for date and profile
    private lateinit var dateTextView: TextView
    private lateinit var profileIcon: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Get current user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please login to view your habits.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Setup Habit RecyclerView
        recyclerViewHabits = findViewById(R.id.recyclerViewHabits)
        habitAdapter = HabitAdapter(habitList, selectedDate, userId) // Pass userId here
        recyclerViewHabits.layoutManager = LinearLayoutManager(this)
        recyclerViewHabits.adapter = habitAdapter

        // Set Locale ke English
        Locale.setDefault(Locale.ENGLISH)
        val config = resources.configuration
        config.setLocale(Locale.ENGLISH)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Setup CalendarView
        calendarView = findViewById(R.id.calendarView)
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDay = getFormattedDay(year, month, dayOfMonth)
            selectedDate = getFormattedDate(year, month, dayOfMonth)

            // Reset highlight when a new date is selected
            resetHighlight()

            // Update adapter with new date
            habitAdapter = HabitAdapter(habitList, selectedDate, userId) // Pass userId here
            recyclerViewHabits.adapter = habitAdapter

            // Load habits for the selected date
            loadHabits()
        }

        // Navigation Button Setup
        findViewById<View>(R.id.iv_add).setOnClickListener {
            startActivity(Intent(this, AddHabitActivity::class.java))
        }

        findViewById<View>(R.id.iv_chat).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        // Set up profile icon
        profileIcon = findViewById(R.id.iv_profile)
        profileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Set up the date text
        dateTextView = findViewById(R.id.tv_date)

        // Load initial data from Firestore
        loadHabits()

        // Update current date
        updateDateText()
    }

    private fun getCurrentDay(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getFormattedDay(year: Int, month: Int, day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        val dateFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun getFormattedDate(year: Int, month: Int, day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun loadHabits() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        if (userId == null || userEmail == null) {
            Toast.makeText(this, "Please login to view your habits.", Toast.LENGTH_SHORT).show()
            return
        }
        // Clear existing habits
        habitList.clear()
        // First, load habits where user is the creator
        firestore.collection("habits")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { userHabits ->
                for (document in userHabits) {
                    val habit = document.toObject(Habit::class.java).apply {
                        id = document.id
                    }
                    // Only add habits for the selected day that haven't been completed
                    val isCompletedByUser =
                        habit.completedDates[selectedDate]?.contains(userId) == true
                    if ((selectedDay.isEmpty() || habit.days.contains(selectedDay)) && !isCompletedByUser) {
                        habitList.add(habit)
                    }
                }
                // Then, load collaborative habits where user is a participant
                firestore.collection("habits")
                    .whereEqualTo("friendEmail", userEmail)
                    .whereEqualTo("collaborative", true)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { collaborativeHabits ->
                        for (document in collaborativeHabits) {
                            val habit = document.toObject(Habit::class.java).apply {
                                id = document.id
                            }
                            // Only add habits for the selected day that haven't been completed
                            val isCompletedByUser =
                                habit.completedDates[selectedDate]?.contains(userId) == true
                            if ((selectedDay.isEmpty() || habit.days.contains(selectedDay)) && !isCompletedByUser) {
                                habitList.add(habit)
                            }
                        }
                        // Finally, load competitive habits where user is a participant
                        firestore.collection("habits")
                            .whereEqualTo("friendEmail", userEmail)
                            .whereEqualTo("competitive", true)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener { competitiveHabits ->
                                for (document in competitiveHabits) {
                                    val habit = document.toObject(Habit::class.java).apply {
                                        id = document.id
                                    }
                                    // Only add habits for the selected day that haven't been completed
                                    val isCompletedByUser =
                                        habit.completedDates[selectedDate]?.contains(userId) == true
                                    if ((selectedDay.isEmpty() || habit.days.contains(selectedDay)) && !isCompletedByUser) {
                                        habitList.add(habit)
                                    }
                                }
                                // Update the adapter with all habits
                                habitAdapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Failed to load competitive habits: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Failed to load collaborative habits: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load habits: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun fetchFriendHabits(userId: String) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        if (currentUserEmail == null) {
            habitAdapter.notifyDataSetChanged()
            return
        }

        firestore.collection("habits")
            .whereEqualTo("friendEmail", currentUserEmail)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val friendHabitList = mutableListOf<Habit>()
                for (document in documents) {
                    val habit = document.toObject(Habit::class.java).apply {
                        id = document.id
                    }
                    val isCompletedByUser = habit.completedDates[selectedDate]?.contains(userId) == true
                    if ((selectedDay.isEmpty() || habit.days.contains(selectedDay)) && !isCompletedByUser) {
                        friendHabitList.add(habit)
                    }
                }
                habitList.addAll(friendHabitList)
                habitAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load friends' habits: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDateText() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        dateTextView.text = dateFormat.format(calendar.time)
    }

    override fun onResume() {
        super.onResume()
        resetHighlight() // Reset highlight ketika kembali ke HomePage
        setHighlight(R.id.tv_today_habit)

        // Reset the calendar to today's date
        val today = Calendar.getInstance().timeInMillis
        calendarView.setDate(today, true, true)

        selectedDay = getCurrentDay() // Update selectedDay to today
        selectedDate = getCurrentDate() // Update selectedDate to today

        // Get current user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Update adapter with current date and userId
            habitAdapter = HabitAdapter(habitList, selectedDate, userId)
            recyclerViewHabits.adapter = habitAdapter

            loadHabits() // Load habits for the selected day
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    fun onAllHabitClick(view: View) {
        setHighlight(R.id.tv_all_habit) // Highlight "All Habit"
        selectedDay = "" // Clear selected day to show all habits
        loadHabits()
    }

    fun onTodayHabitClick(view: View) {
        setHighlight(R.id.tv_today_habit) // Highlight "Today Habit"

        // Reset the calendar to today's date
        val today = Calendar.getInstance().timeInMillis
        calendarView.setDate(today, true, true)

        selectedDay = getCurrentDay() // Update selectedDay to today
        selectedDate = getCurrentDate() // Update selectedDate to today

        // Get current user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Update adapter with current date and userId
            habitAdapter = HabitAdapter(habitList, selectedDate, userId)
            recyclerViewHabits.adapter = habitAdapter

            loadHabits() // Load habits for the selected day
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    fun onProgressHabitClick(view: View) {
        setHighlight(R.id.tv_progress)

        // Navigasi ke ProgressActivity
        val intent = Intent(this, ProgressActivity::class.java)
        startActivity(intent)
    }

    private fun setHighlight(viewId: Int) {
        // Reset colors for all options
        val defaultBackground = resources.getDrawable(android.R.color.transparent, null)
        val defaultTextColor = resources.getColor(R.color.default_color, null)

        findViewById<TextView>(R.id.tv_all_habit).background = defaultBackground
        findViewById<TextView>(R.id.tv_today_habit).background = defaultBackground

        findViewById<TextView>(R.id.tv_all_habit).setTextColor(defaultTextColor)
        findViewById<TextView>(R.id.tv_today_habit).setTextColor(defaultTextColor)

        // Highlight the selected view
        val selectedView = findViewById<TextView>(viewId)
        selectedView.background = resources.getDrawable(R.drawable.highlight_background, null)
        selectedView.setTextColor(resources.getColor(android.R.color.white, null))
    }

    private fun resetHighlight() {
        val defaultBackground = resources.getDrawable(android.R.color.transparent, null)
        val defaultTextColor = resources.getColor(R.color.default_color, null)

        findViewById<TextView>(R.id.tv_all_habit).background = defaultBackground
        findViewById<TextView>(R.id.tv_today_habit).background = defaultBackground
        findViewById<TextView>(R.id.tv_progress).background = defaultBackground // Reset untuk progress

        findViewById<TextView>(R.id.tv_all_habit).setTextColor(defaultTextColor)
        findViewById<TextView>(R.id.tv_today_habit).setTextColor(defaultTextColor)
        findViewById<TextView>(R.id.tv_progress).setTextColor(defaultTextColor) // Reset warna untuk progress
    }
}