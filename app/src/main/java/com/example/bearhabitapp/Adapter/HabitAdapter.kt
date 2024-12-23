package com.example.bearhabitapp.Adapter

import android.graphics.Color
import android.util.Log
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
import com.google.firebase.firestore.QuerySnapshot
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

        fun bind(habit: Habit) {
            tvHabitName.text = habit.habitName
        }
    }

    private fun markHabitAsCompleted(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        habit.id?.let { habitId ->
            // Tambahkan currentUserId ini ke completedDates untuk tanggal ini menggunakan arrayUnion
            val updates = hashMapOf<String, Any>(
                "completedDates.$currentDate" to FieldValue.arrayUnion(currentUserId)
            )

            firestore.collection("habits")
                .document(habitId)
                .update(updates)
                .addOnSuccessListener {
                    // Update lokal data
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

                    // Jika habit adalah kompetitif, tambahkan juga ke dokumen teman
                    if (habit.competitive && !habit.friendEmail.isNullOrEmpty()) {
                        updateFriendHabitCompletion(
                            habit.friendEmail,
                            habit.habitName,
                            habitId
                        )
                    }

                    // Hapus habit dari daftar jika pengguna saat ini telah menyelesaikannya
                    if (habit.completedDates[currentDate]?.contains(currentUserId) == true &&
                        !habit.competitive // Hanya hapus jika non kompetitif
                    ) {
                        habits.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, habits.size)
                    } else {
                        notifyItemChanged(position)
                    }
                }
                .addOnFailureListener { e ->
                    // Kembalikan checkbox ke status sebelumnya jika gagal
                    holder.checkBoxDelete.isChecked = false
                    Toast.makeText(
                        holder.itemView.context,
                        "Gagal menandai habit: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    /**
     * Fungsi untuk memperbarui dokumen habit teman saat user menyelesaikan habit kompetitif.
     *
     * @param friendEmail Email teman yang terkait dengan habit kompetitif.
     * @param habitName Nama habit yang diselesaikan.
     * @param userHabitId ID dokumen habit user saat ini.
     */
    private fun updateFriendHabitCompletion(friendEmail: String, habitName: String, userHabitId: String) {
        // Cari dokumen teman berdasarkan email dan nama habit
        firestore.collection("users")
            .whereEqualTo("email", friendEmail)
            .get()
            .addOnSuccessListener { userSnapshot ->
                if (!userSnapshot.isEmpty) {
                    val friendDoc = userSnapshot.documents[0]
                    val friendUserId = friendDoc.id

                    // Cari dokumen habit teman berdasarkan userId dan nama habit
                    firestore.collection("habits")
                        .whereEqualTo("userId", friendUserId)
                        .whereEqualTo("habitName", habitName)
                        .whereEqualTo("competitive", true)
                        .get()
                        .addOnSuccessListener { habitSnapshot ->
                            if (!habitSnapshot.isEmpty) {
                                val friendHabitDoc = habitSnapshot.documents[0]
                                val friendHabitId = friendHabitDoc.id

                                // Update completedDates di dokumen teman
                                val friendUpdates = hashMapOf<String, Any>(
                                    "completedDates.$currentDate" to FieldValue.arrayUnion(currentUserId)
                                )

                                firestore.collection("habits")
                                    .document(friendHabitId)
                                    .update(friendUpdates)
                                    .addOnSuccessListener {
                                        Log.d(
                                            "HabitAdapter",
                                            "Successfully updated friend's habit completion."
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(
                                            "HabitAdapter",
                                            "Failed to update friend's habit: ${e.message}"
                                        )
                                    }
                            } else {
                                Log.e(
                                    "HabitAdapter",
                                    "Friend's habit not found for habitName: $habitName"
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(
                                "HabitAdapter",
                                "Error fetching friend's habits: ${e.message}"
                            )
                        }
                } else {
                    Log.e("HabitAdapter", "Friend with email $friendEmail not found.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("HabitAdapter", "Error fetching friend by email: ${e.message}")
            }
    }
}