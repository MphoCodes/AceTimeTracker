package com.example.acetimetracker.data.repository

import com.example.acetimetracker.data.model.Category
import com.example.acetimetracker.util.Resource
import com.google.firebase.firestore.FirebaseFirestore

class CategoryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun fetchCategories(userId: String, onResult: (Resource<List<Category>>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(Resource.Error("User not authenticated"))
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
                // Also mirror to global Category collection for timer compatibility
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
