package com.example.acetimetracker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null
    private lateinit var profilePictureUrl: String
    private lateinit var profileImageView: ImageView
    private lateinit var profilePictureLoadingAnimation: LottieAnimationView

    // Use an ActivityResultLauncher for the image picker
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                uploadImageToFirebaseStorage(selectedImageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val editDisplayNameEditText = findViewById<EditText>(R.id.editDisplayNameEditText)
        val editEmailEditText = findViewById<EditText>(R.id.editEmailEditText)
        val uploadProfilePictureButton = findViewById<Button>(R.id.uploadProfilePictureButton)
        val saveProfileChangesButton = findViewById<Button>(R.id.saveProfileChangesButton)

        // Initialize profileImageView, profilePictureLoadingAnimation, and loadingTextView in onCreate()
        profileImageView = findViewById(R.id.profileImageView)
        profilePictureLoadingAnimation = findViewById(R.id.profilePictureLoadingAnimation)

        fetchUserData()

        uploadProfilePictureButton.setOnClickListener {
            openImagePicker()
        }

        saveProfileChangesButton.setOnClickListener {
            val newDisplayName = editDisplayNameEditText.text.toString()
            val newEmail = editEmailEditText.text.toString()

            updateUserProfile(newDisplayName, newEmail, profilePictureUrl)

            finish()
        }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: ""
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val displayName = document.getString("displayName")
                val email = document.getString("email")
                val profilePictureUrl = document.getString("profilePictureUrl")

                val editDisplayNameEditText = findViewById<EditText>(R.id.editDisplayNameEditText)
                val editEmailEditText = findViewById<EditText>(R.id.editEmailEditText)

                editDisplayNameEditText.setText(displayName)
                editEmailEditText.setText(email)

                // Load the profile picture using Glide (if available)
                if (profilePictureUrl != null) {
                    Glide.with(this)
                        .load(profilePictureUrl)
                        .transform(CircleCrop())
                        .into(profileImageView)
                } else {
                    // Handle the case where there's no profile picture
                    profileImageView.setImageResource(R.drawable.ic_account_circle) // Or set a default image
                }
            }
            .addOnFailureListener { exception ->
                Log.e("EditProfileActivity", "Error fetching user data: ${exception.message}", exception)
            }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startForResult.launch(intent)
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri?) {
        if (imageUri != null) {
            val storageRef = storage.reference
            val imageName = UUID.randomUUID().toString()
            val imageRef = storageRef.child("profile_pictures/$imageName")

            // Show the loading animation and disable interaction
            profilePictureLoadingAnimation.visibility = View.VISIBLE
            profilePictureLoadingAnimation.repeatCount = LottieDrawable.INFINITE
            profilePictureLoadingAnimation.repeatMode = LottieDrawable.RESTART
            profilePictureLoadingAnimation.playAnimation()


            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )

            imageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Update profilePictureUrl after successful upload
                        profilePictureUrl = uri.toString()
                        Log.d("EditProfileActivity", "Image uploaded successfully: $profilePictureUrl")
                        Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()

                        // Stop the animation and re-enable interaction
                        profilePictureLoadingAnimation.cancelAnimation()
                        profilePictureLoadingAnimation.visibility = View.INVISIBLE
                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("EditProfileActivity", "Error uploading image: ${exception.message}", exception)
                    Toast.makeText(this, "Error uploading image", Toast.LENGTH_SHORT).show()

                    // Stop the animation and re-enable interaction
                    profilePictureLoadingAnimation.cancelAnimation()
                    profilePictureLoadingAnimation.visibility = View.INVISIBLE
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                }
        } else {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserProfile(displayName: String, email: String, profilePictureUrl: String) {
        val userId = auth.currentUser?.uid ?: ""
        val userProfile = hashMapOf(
            "displayName" to displayName,
            "email" to email,
            "profilePictureUrl" to profilePictureUrl
        )
        val userProfileMap: Map<String, Any> = userProfile as Map<String, Any>

        firestore.collection("users").document(userId)
            .set(userProfileMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("EditProfileActivity", "User profile updated successfully")
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("EditProfileActivity", "Error updating user profile: ${exception.message}", exception)
                Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show()
            }
    }
}