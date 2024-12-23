package com.example.bearhabitapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bearhabitapp.Model.HabitProgress
import com.example.bearhabitapp.R

class HabitProgressAdapter(
    private val onDeleteClick: (HabitProgress) -> Unit // Callback untuk penghapusan
) : ListAdapter<HabitProgress, HabitProgressAdapter.ProgressViewHolder>(ProgressDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit_progress, parent, false)
        return ProgressViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHabitName: TextView = itemView.findViewById(R.id.tvHabitName)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete) // Tombol Delete

        fun bind(habitProgress: HabitProgress) {
            tvHabitName.text = habitProgress.habitName
            tvUserEmail.text = habitProgress.userEmail
            progressBar.progress = habitProgress.progress.toInt()
            tvProgress.text = "${habitProgress.progress.toInt()}% " +
                    "(${habitProgress.completedTasks}/${habitProgress.totalTasks})"

            btnDelete.setOnClickListener {
                onDeleteClick(habitProgress) // Memanggil callback saat tombol Delete diklik
            }
        }
    }

    class ProgressDiffCallback : DiffUtil.ItemCallback<HabitProgress>() {
        override fun areItemsTheSame(oldItem: HabitProgress, newItem: HabitProgress): Boolean {
            return oldItem.habitId == newItem.habitId && oldItem.userEmail == newItem.userEmail
        }

        override fun areContentsTheSame(oldItem: HabitProgress, newItem: HabitProgress): Boolean {
            return oldItem == newItem
        }
    }
}