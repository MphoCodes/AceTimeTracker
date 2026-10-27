package com.example.acetimetracker.data.repository

import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class CategoryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private val mockCategories = mutableListOf(
            Category("mock_cat_1", "Development", "Android App development and refactoring", 2f, 6f, 4f),
            Category("mock_cat_2", "Design", "UI/UX wireframing and prototyping", 1f, 4f, 2f),
            Category("mock_cat_3", "Testing", "Unit testing and QA verification", 1f, 3f, 2f),
            Category("mock_cat_4", "Documentation", "Architecture and user documentation", 1f, 2f, 1.5f)
        )
    }

    fun fetchCategories(userId: String, onResult: (Resource<List<Category>>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(Resource.Error("User not authenticated"))
            return
        }

        // Offline test mode bypass
        if (userId == AuthRepository.TEST_USER_ID) {
            onResult(Resource.Success(mockCategories.toList()))
            return
        }

        onResult(Resource.Loading)
        firestore.collection("users")
            .document(userId)
            .collection("categories")
            .get()
            .addOnSuccessListener { snapshot ->
                val categories = snapshot.documents.map { doc ->
                    Category(
                        documentId = doc.id,
                        name = doc.getString("name").orEmpty(),
                        description = doc.getString("description").orEmpty(),
                        minGoal = doc.getDouble("minGoal")?.toFloat(),
                        maxGoal = doc.getDouble("maxGoal")?.toFloat(),
                        goal = doc.getDouble("goal")?.toFloat()
                    )
                }
                onResult(Resource.Success(categories))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Failed to fetch categories", exception))
            }
    }

    fun addCategory(userId: String, category: Category, onResult: (Resource<String>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(Resource.Error("User not authenticated"))
            return
        }

        // Offline test mode bypass
        if (userId == AuthRepository.TEST_USER_ID) {
            val generatedId = "mock_cat_${UUID.randomUUID().toString().take(6)}"
            val created = category.copy(documentId = generatedId)
            mockCategories.add(created)
            onResult(Resource.Success(generatedId))
            return
        }

        onResult(Resource.Loading)
        val categoryData = hashMapOf(
            "name" to category.name,
            "description" to category.description,
            "goal" to category.goal,
            "minGoal" to category.minGoal,
            "maxGoal" to category.maxGoal,
            "timeSpent" to 0L
        )

        val userCatRef = firestore.collection("users")
            .document(userId)
            .collection("categories")

        userCatRef.add(categoryData)
            .addOnSuccessListener { docRef ->
                firestore.collection("Category")
                    .document(category.name)
                    .set(hashMapOf("name" to category.name, "totalTime" to 0L))

                onResult(Resource.Success(docRef.id))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Failed to add category", exception))
            }
    }

    fun deleteCategory(userId: String, categoryId: String, onResult: (Resource<Unit>) -> Unit) {
        if (userId.isEmpty() || categoryId.isEmpty()) {
            onResult(Resource.Error("Invalid request"))
            return
        }

        // Offline test mode bypass
        if (userId == AuthRepository.TEST_USER_ID) {
            mockCategories.removeAll { it.documentId == categoryId }
            onResult(Resource.Success(Unit))
            return
        }

        onResult(Resource.Loading)
        firestore.collection("users")
            .document(userId)
            .collection("categories")
            .document(categoryId)
            .delete()
            .addOnSuccessListener {
                onResult(Resource.Success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Failed to delete category", exception))
            }
    }
}
