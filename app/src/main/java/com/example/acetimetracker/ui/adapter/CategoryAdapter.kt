package com.example.acetimetracker.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.acetimetracker.R
import com.example.acetimetracker.data.model.Category

class CategoryAdapter(
    private var categoryList: List<Category> = emptyList(),
    private val onItemClick: ((Category) -> Unit)? = null,
    private val onDeleteListener: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

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
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(category)
        }
        holder.itemView.setOnLongClickListener {
            onDeleteListener(category)
            true
        }
    }

    override fun getItemCount(): Int = categoryList.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewCategoryName)
        private val descTextView: TextView = itemView.findViewById(R.id.textViewCategoryDescription)
        private val minGoalTextView: TextView = itemView.findViewById(R.id.textViewMinGoal)
        private val maxGoalTextView: TextView = itemView.findViewById(R.id.textViewMaxGoal)

        fun bind(category: Category) {
            nameTextView.text = category.name
            descTextView.text = category.description
            minGoalTextView.text = "Min Goal: ${category.minGoal ?: "Not set"}"
            maxGoalTextView.text = "Max Goal: ${category.maxGoal ?: "Not set"}"
            itemView.tag = category.documentId
        }
    }
}
