package com.example.acetimetracker.ui.activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.acetimetracker.R
import com.example.acetimetracker.ui.adapter.CategoryAdapter
import com.example.acetimetracker.ui.dialog.CategoryDetailsDialog
import com.example.acetimetracker.ui.viewmodel.CategoryViewModel
import com.example.acetimetracker.util.Resource

class CategoriesActivity : AppCompatActivity() {

    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewCategories)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val buttonAddCategory = findViewById<Button>(R.id.buttonAddCategory)

        val editTextNewCategory = findViewById<EditText>(R.id.editTextNewCategory)
        val editTextNewCategoryDescription = findViewById<EditText>(R.id.editTextNewCategoryDescription)
        val editTextGoal = findViewById<EditText>(R.id.editTextGoal)
        val editTextMinGoal = findViewById<EditText>(R.id.editTextMinGoal)
        val editTextMaxGoal = findViewById<EditText>(R.id.editTextMaxGoal)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        categoryAdapter = CategoryAdapter(
            onItemClick = { category ->
                CategoryDetailsDialog.newInstance(category)
                    .show(supportFragmentManager, "CategoryDetailsDialog")
            },
            onDeleteListener = { category ->
                viewModel.deleteCategory(category)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = categoryAdapter

        buttonAddCategory.setOnClickListener {
            val name = editTextNewCategory.text.toString()
            val desc = editTextNewCategoryDescription.text.toString()
            val goal = editTextGoal.text.toString().toFloatOrNull()
            val minGoal = editTextMinGoal.text.toString().toFloatOrNull()
            val maxGoal = editTextMaxGoal.text.toString().toFloatOrNull()

            viewModel.addCategory(name, desc, goal, minGoal, maxGoal)
        }

        observeViewModel(
            editTextNewCategory,
            editTextNewCategoryDescription,
            editTextGoal,
            editTextMinGoal,
            editTextMaxGoal
        )

        viewModel.loadCategories()
    }

    private fun observeViewModel(
        nameEdit: EditText,
        descEdit: EditText,
        goalEdit: EditText,
        minGoalEdit: EditText,
        maxGoalEdit: EditText
    ) {
        viewModel.categoriesState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    categoryAdapter.updateData(resource.data)
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.addCategoryState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Category added successfully!", Toast.LENGTH_SHORT).show()
                    nameEdit.text.clear()
                    descEdit.text.clear()
                    goalEdit.text.clear()
                    minGoalEdit.text.clear()
                    maxGoalEdit.text.clear()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.deleteCategoryState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
