package com.example.acetimetracker

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Categories : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewCategories)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val buttonAddCategory = findViewById<Button>(R.id.buttonAddCategory)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoryAdapter(emptyList(),
            onDeleteListener = { category ->
                deleteCategory(category)
            }
        )
        recyclerView.adapter = categoryAdapter

        // Fetch data
        fetchDataFromFirestore()

        // Back button listener
        btnBack.setOnClickListener {
            onBackPressed()
        }

        // Add Category button listener
        buttonAddCategory.setOnClickListener {
            addCategoryToFirestore()
        }
    }

    private fun fetchDataFromFirestore() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users")
                .document(userId)
                .collection("categories")
                .get()
                .addOnSuccessListener { result ->
                    val categories = mutableListOf<Category>()
                    for (document in result) {
                        val name = document.getString("name") ?: ""
                        val description = document.getString("description") ?: ""
                        val minGoal = document.getDouble("minGoal")?.toFloat()
                        val maxGoal = document.getDouble("maxGoal")?.toFloat()
                        val goal = document.getDouble("goal")?.toFloat()
                        val documentId = document.id // Get the document ID

                        // Add category with documentId
                        categories.add(
                            Category(
                                documentId = documentId, // Include documentId
                                name = name,
                                description = description,
                                minGoal = minGoal,
                                maxGoal = maxGoal,
                                goal = goal
                            )
                        )
                    }
                    categoryAdapter.updateData(categories)
                }
        }
    }

    private fun addCategoryToFirestore() {
        val editTextNewCategory = findViewById<EditText>(R.id.editTextNewCategory)
        val editTextNewCategoryDescription = findViewById<EditText>(R.id.editTextNewCategoryDescription)
        val editTextGoal = findViewById<EditText>(R.id.editTextGoal)
        val editTextMinGoal = findViewById<EditText>(R.id.editTextMinGoal)
        val editTextMaxGoal = findViewById<EditText>(R.id.editTextMaxGoal)

        val categoryName = editTextNewCategory.text.toString().trim()
        val categoryDescription = editTextNewCategoryDescription.text.toString().trim()
        val goal = editTextGoal.text.toString().toFloatOrNull()
        val minGoal = editTextMinGoal.text.toString().toFloatOrNull()
        val maxGoal = editTextMaxGoal.text.toString().toFloatOrNull()

        if (categoryName.isNotEmpty() && categoryDescription.isNotEmpty() && goal != null && minGoal != null && maxGoal != null) {
            val categoryData = hashMapOf(
                "name" to categoryName,
                "description" to categoryDescription,
                "goal" to goal,
                "minGoal" to minGoal,
                "maxGoal" to maxGoal,
                "timeSpent" to 0L // Consider if you need this field
            )

            val userId = auth.currentUser?.uid
            if (userId != null) {
                firestore.collection("users")
                    .document(userId)
                    .collection("categories")
                    .add(categoryData)
                    .addOnSuccessListener { documentReference ->
                        Log.d("Categories", "Category added with ID: ${documentReference.id}")
                        Toast.makeText(this, "Category added: $categoryName", Toast.LENGTH_SHORT).show()
                        // Clear input fields
                        editTextNewCategory.text.clear()
                        editTextNewCategoryDescription.text.clear()
                        editTextGoal.text.clear()
                        editTextMinGoal.text.clear()
                        editTextMaxGoal.text.clear()
                        fetchDataFromFirestore()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Categories", "Error adding category", e)
                        Toast.makeText(this, "Failed to add category. Please try again.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Handle case where user is not logged in
                Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            }

        } else {
            Toast.makeText(this, "Please enter all category details", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to delete a category (add this inside the Categories activity)
    private fun deleteCategory(category: Category) {
        val userId = auth.currentUser?.uid
        if (userId != null) {

            firestore.collection("users")
                .document(userId)
                .collection("categories")
                .document(category.documentId) // Use category.documentId
                .delete()
                .addOnSuccessListener {
                    Log.d("Categories", "Category deleted with ID: ${category.documentId}")
                    Toast.makeText(this, "Category deleted: ${category.name}", Toast.LENGTH_SHORT).show()
                    fetchDataFromFirestore()
                }
                .addOnFailureListener { e ->
                    Log.e("Categories", "Error deleting category", e)
                    Toast.makeText(this, "Failed to delete category: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }
}