package com.example.acetimetracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var profilePictureLoadingAnimation: LottieAnimationView // Initialize here

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, profile::class.java)
                    startActivity(intent)
                }
                R.id.nav_categories -> {
                    Toast.makeText(this, "Categories clicked", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Categories::class.java)
                    startActivity(intent)
                }
                R.id.nav_timesheet -> {
                    Toast.makeText(this, "Timesheet clicked", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Timesheet::class.java)
                    startActivity(intent)
                }
                R.id.nav_timer -> {
                    Toast.makeText(this, "Timer clicked", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Timer::class.java)
                    startActivity(intent)
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, settings::class.java)
                    startActivity(intent)
                }
                R.id.nav_myprogress -> {
                    Toast.makeText(this, "My Progress clicked", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Myprogress::class.java)
                    startActivity(intent)
                }
                R.id.nav_about -> {
                    Toast.makeText(this, "About clicked", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, About::class.java)
                    startActivity(intent)
                }
                R.id.nav_graph -> {
                    Toast.makeText(this, "Graph clicked", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, GraphActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            true
        }

        fab.setOnClickListener {
            val intent = Intent(this, Categories::class.java)
            startActivity(intent)
        }

        // Get reference to the LottieAnimationView
        profilePictureLoadingAnimation = findViewById(R.id.profilePictureLoadingAnimation)

        // Start the Lottie animation
        profilePictureLoadingAnimation.repeatCount = LottieDrawable.INFINITE
        profilePictureLoadingAnimation.repeatMode = LottieDrawable.RESTART
        profilePictureLoadingAnimation.playAnimation()

        fetchUserData()
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: ""
        if (userId.isNotEmpty()) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val displayName = document.getString("displayName")
                    val email = document.getString("email")
                    val profilePictureUrl = document.getString("profilePictureUrl")

                    // Update the Navigation Drawer header
                    updateNavDrawerHeader(displayName, email, profilePictureUrl)
                }
                .addOnFailureListener { exception ->
                    Log.e("MainActivity", "Error fetching user data: ${exception.message}", exception)
                }
        }
    }

    private fun updateNavDrawerHeader(displayName: String?, email: String?, profilePictureUrl: String?) {
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navView.getHeaderView(0)

        val imageView = headerView.findViewById<ImageView>(R.id.imageView)
        val nameTextView = headerView.findViewById<TextView>(R.id.nav_name_textview)
        val emailTextView = headerView.findViewById<TextView>(R.id.nav_email_textview)

        // Load the profile picture (if available)
        if (profilePictureUrl != null) {
            Glide.with(this)
                .load(profilePictureUrl)
                .transform(CircleCrop())
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_account_circle) // Default profile picture
        }

        // Set the display name and email
        nameTextView.text = displayName ?: "Ace! Time Tracker"
        emailTextView.text = email ?: "st10203065@vcconnect.edu.za"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView)
        } else {
            super.onBackPressed()
        }
    }
}