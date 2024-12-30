package com.example.bearhabitapp.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.bearhabitapp.Model.Habit
import com.example.bearhabitapp.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HabitAdapter(
    private val habits: MutableList<Habit>,
    private val currentDate: String = getCurrentDate(),
    private val currentUserId: String
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private fun getCurrentDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]

        // Set the CardView background color
        val backgroundColor = habit.iconColor
        holder.cardHabit.setCardBackgroundColor(Color.parseColor(backgroundColor))

        // Bind habit data
        holder.bind(habit)

        // Setup checkbox listener
        holder.checkBoxDelete.setOnCheckedChangeListener(null) // Prevents unwanted triggers during recycling
        holder.checkBoxDelete.isChecked = habit.completedDates[currentDate]?.contains(currentUserId) == true
        holder.checkBoxDelete.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                markHabitAsCompleted(holder, position)
            }
        }
    }

    override fun getItemCount(): Int = habits.size

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHabitName: TextView = itemView.findViewById(R.id.tvHabitName)
        val checkBoxDelete: CheckBox = itemView.findViewById(R.id.checkBoxDelete)
        val cardHabit: CardView = itemView.findViewById(R.id.cardHabit)
        val tvRepeatCount: TextView = itemView.findViewById(R.id.tvRepeatCount)
        val tvDays: TextView = itemView.findViewById(R.id.tvDays)

        fun bind(habit: Habit) {
            tvHabitName.text = habit.habitName
            tvRepeatCount.text = "Repeat: ${habit.repeatCount} times"
            tvDays.text = "Days: ${habit.days.joinToString(", ")}"
        }
    }

    private fun markHabitAsCompleted(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        habit.id?.let { habitId ->
            val updates = hashMapOf<String, Any>(
                "completedDates.$currentDate" to FieldValue.arrayUnion(currentUserId)
            )

            firestore.collection("habits")
                .document(habitId)
                .update(updates)
                .addOnSuccessListener {
                    // Update local data
                    if (habit.completedDates[currentDate] == null) {
                        habit.completedDates[currentDate] = mutableListOf()
                    }
                    if (!habit.completedDates[currentDate]!!.contains(currentUserId)) {
                        habit.completedDates[currentDate]!!.add(currentUserId)
                    }

                    Toast.makeText(
                        holder.itemView.context,
                        "Habit selesai untuk hari ini",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Remove habit from list if completed
                    if (habit.completedDates[currentDate]?.contains(currentUserId) == true) {
                        habits.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, habits.size)
                    } else {
                        notifyItemChanged(position)
                    }
                }
                .addOnFailureListener { e ->
                    // Revert checkbox state on failure
                    holder.checkBoxDelete.isChecked = false
                    Toast.makeText(
                        holder.itemView.context,
                        "Gagal menandai habit: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    // Additional methods if any...
}