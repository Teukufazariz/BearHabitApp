package com.example.bearhabitapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bearhabitapp.R
import java.text.SimpleDateFormat
import java.util.*

class DaysAdapter(
    private val days: List<String>,
    private val onDayClick: (String) -> Unit
) : RecyclerView.Adapter<DaysAdapter.DayViewHolder>() {

    private var selectedDayPosition = 0

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewDay: TextView = itemView.findViewById(R.id.textViewDay)
        val textViewDayName: TextView = itemView.findViewById(R.id.textViewDayName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        val calendar = Calendar.getInstance()
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())

        // Set day number
        holder.textViewDay.text = (position + 1).toString()

        // Set day name
        holder.textViewDayName.text = dayNameFormat.format(calendar.time).uppercase()

        // Highlight logic remains the same as previous implementation
        holder.itemView.setOnClickListener {
            val oldSelectedPosition = selectedDayPosition
            selectedDayPosition = position
            notifyItemChanged(oldSelectedPosition)
            notifyItemChanged(position)
            onDayClick(day)
        }
    }

    override fun getItemCount() = days.size
}
