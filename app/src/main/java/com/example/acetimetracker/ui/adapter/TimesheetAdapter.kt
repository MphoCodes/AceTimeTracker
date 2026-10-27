package com.example.acetimetracker.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.acetimetracker.R
import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.util.TimeUtils

class TimesheetAdapter : RecyclerView.Adapter<TimesheetAdapter.TimesheetViewHolder>() {

    private val timesheets = mutableListOf<TimesheetEntry>()
    private var filteredTimesheets = mutableListOf<TimesheetEntry>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimesheetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timesheet_entry, parent, false)
        return TimesheetViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimesheetViewHolder, position: Int) {
        holder.bind(filteredTimesheets[position])
    }

    override fun getItemCount(): Int = filteredTimesheets.size

    fun submitList(newTimesheets: List<TimesheetEntry>) {
        timesheets.clear()
        timesheets.addAll(newTimesheets)
        filteredTimesheets = ArrayList(timesheets)
        notifyDataSetChanged()
    }

    fun filterByCategory(category: String) {
        filteredTimesheets = if (category == "All" || category.isBlank()) {
            ArrayList(timesheets)
        } else {
            timesheets.filter { it.category == category }.toMutableList()
        }
        notifyDataSetChanged()
    }

    inner class TimesheetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(timesheet: TimesheetEntry) {
            categoryTextView.text = timesheet.category
            timeTextView.text = TimeUtils.formatDuration(timesheet.timeSpent)

            if (timesheet.imageUrl.isNotEmpty()) {
                imageView.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(timesheet.imageUrl)
                    .into(imageView)
            } else {
                imageView.visibility = View.GONE
            }
        }
    }
}
