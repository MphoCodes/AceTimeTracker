package com.example.acetimetracker

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

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

        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(view)
            .setTitle("Category Details")
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }

        return builder.create()
    }
}
