package com.example.acetimetracker

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class TimesheetAdapter(private val activity: AppCompatActivity) :
    RecyclerView.Adapter<TimesheetAdapter.TimesheetViewHolder>() {

    private var timesheets = mutableListOf<TimesheetEntry>()
    private var filteredTimesheets = mutableListOf<TimesheetEntry>() // Add a filtered list

    companion object {
        private const val REQUEST_EXTERNAL_STORAGE_PERMISSION_CODE = 101
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimesheetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timesheet_entry, parent, false)
        return TimesheetViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimesheetViewHolder, position: Int) {
        val timesheet = filteredTimesheets[position] // Use filtered list
        holder.bind(timesheet)
    }

    override fun getItemCount(): Int {
        return filteredTimesheets.size // Use filtered list size
    }

    fun submitList(newTimesheets: List<TimesheetEntry>) {
        timesheets.clear()
        timesheets.addAll(newTimesheets)
        filteredTimesheets = timesheets // Initialize filtered list with all timesheets
        notifyDataSetChanged()
    }

    fun filterByCategory(category: String) {
        Log.d(TAG, "Filtering by category: $category")
        filteredTimesheets = if (category == "All") {
            timesheets.toMutableList()
        } else {
            timesheets.filter { it.category == category }.toMutableList()
        }
        Log.d(TAG, "Filtered timesheets: $filteredTimesheets")
        notifyDataSetChanged()
    }

    inner class TimesheetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(timesheet: TimesheetEntry) {
            categoryTextView.text = timesheet.category
            timeTextView.text = formatTime(timesheet.timeSpent)

            // Check if the permission is already granted
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_EXTERNAL_STORAGE_PERMISSION_CODE
                )
            } else {
                // Permission is already granted, proceed with loading the image
                loadImage(timesheet)
            }
        }

        private fun loadImage(timesheet: TimesheetEntry) {
            // Load and display the image using Glide
            if (timesheet.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(timesheet.imageUrl)
                    .into(imageView)
            } else {
                // Optionally, set a placeholder image or hide the ImageView if no image URL is provided
                imageView.visibility = View.GONE
            }
        }

        private fun formatTime(timeInMillis: Long): String {
            val hours = timeInMillis / (1000 * 60 * 60)
            val minutes = (timeInMillis % (1000 * 60 * 60)) / (1000 * 60)
            val seconds = (timeInMillis % (1000 * 60)) / 1000

            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }
}