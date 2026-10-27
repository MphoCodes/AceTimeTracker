package com.example.acetimetracker.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.acetimetracker.R
import com.example.acetimetracker.data.model.Category

class CategoryDetailsDialog : DialogFragment() {

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: Category): CategoryDetailsDialog {
            val args = Bundle().apply {
                putParcelable(ARG_CATEGORY, category)
            }
            return CategoryDetailsDialog().apply {
                arguments = args
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val category = arguments?.getParcelable<Category>(ARG_CATEGORY)

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_category_details, null)

        val nameTextView = view.findViewById<TextView>(R.id.textViewCategoryName)
        val descriptionTextView = view.findViewById<TextView>(R.id.textViewCategoryDescription)

        nameTextView.text = category?.name
        descriptionTextView.text = category?.description

        return AlertDialog.Builder(requireActivity())
            .setView(view)
            .setTitle("Category Details")
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }
}
