package com.example.acetimetracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(private var categoryList: List<Category>,
                      private val onDeleteListener: (Category) -> Unit) :
    RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    fun updateData(newList: List<Category>) {
        categoryList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        holder.bind(category)
        holder.itemView.setOnLongClickListener {
            // Pass the category WITH the documentId
            onDeleteListener.invoke(category)
            true
        }
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryNameTextView: TextView = itemView.findViewById(R.id.textViewCategoryName)
        private val categoryDescriptionTextView: TextView = itemView.findViewById(R.id.textViewCategoryDescription)
        private val categoryMinGoalTextView: TextView = itemView.findViewById(R.id.textViewMinGoal) // Corrected ID
        private val categoryMaxGoalTextView: TextView = itemView.findViewById(R.id.textViewMaxGoal) // Corrected ID

        fun bind(category: Category) {
            categoryNameTextView.text = category.name
            categoryDescriptionTextView.text = category.description
            categoryMinGoalTextView.text = "Min Goal: ${category.minGoal ?: "Not set"}"
            categoryMaxGoalTextView.text = "Max Goal: ${category.maxGoal ?: "Not set"}"
            itemView.tag = category.documentId
        }
    }
}