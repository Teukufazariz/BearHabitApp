package com.example.bearhabitapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bearhabitapp.R
import java.util.*

class MonthAdapter(
    private val daysInMonth: List<String>,
    private val onDayClick: (String) -> Unit
) : RecyclerView.Adapter<MonthAdapter.DayViewHolder>() {

    private var selectedDayPosition = RecyclerView.NO_POSITION

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewDay: TextView = itemView.findViewById(R.id.textViewDay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_monthly, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = daysInMonth[position]
        holder.textViewDay.text = day

        holder.itemView.setOnClickListener {
            val oldSelectedPosition = selectedDayPosition
            selectedDayPosition = position
            notifyItemChanged(oldSelectedPosition)
            notifyItemChanged(position)
            onDayClick(day)
        }
    }

    override fun getItemCount() = daysInMonth.size
}