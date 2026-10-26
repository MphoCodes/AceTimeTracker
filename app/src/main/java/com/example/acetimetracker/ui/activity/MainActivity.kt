package com.example.acetimetracker.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.acetimetracker.R
import com.example.acetimetracker.data.model.UserProfile
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.ui.viewmodel.ProfileViewModel
import com.example.acetimetracker.util.Resource
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var loadingAnimation: LottieAnimationView
    private val profileViewModel: ProfileViewModel by viewModels()
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        loadingAnimation = findViewById(R.id.profilePictureLoadingAnimation)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupNavigation()

        fab.setOnClickListener {
            startActivity(Intent(this, CategoriesActivity::class.java))
        }

        loadingAnimation.repeatCount = LottieDrawable.INFINITE
        loadingAnimation.repeatMode = LottieDrawable.RESTART
        loadingAnimation.playAnimation()

        observeProfile()
        profileViewModel.loadUserProfile()
    }

    private fun setupNavigation() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                }
                R.id.nav_categories -> {
                    startActivity(Intent(this, CategoriesActivity::class.java))
                }
                R.id.nav_timesheet -> {
                    startActivity(Intent(this, TimesheetActivity::class.java))
                }
                R.id.nav_timer -> {
                    startActivity(Intent(this, TimerActivity::class.java))
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.nav_myprogress -> {
                    startActivity(Intent(this, MyProgressActivity::class.java))
                }
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                }
                R.id.nav_graph -> {
                    startActivity(Intent(this, GraphActivity::class.java))
                }
                R.id.nav_logout -> {
                    authRepository.logout()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            true
        }
    }

    private fun observeProfile() {
        profileViewModel.userProfileState.observe(this) { resource ->
            if (resource is Resource.Success) {
                updateNavDrawerHeader(resource.data)
            }
        }
    }

    private fun updateNavDrawerHeader(profile: UserProfile) {
        val headerView = navigationView.getHeaderView(0)
        val imageView = headerView.findViewById<ImageView>(R.id.imageView)
        val nameTextView = headerView.findViewById<TextView>(R.id.nav_name_textview)
        val emailTextView = headerView.findViewById<TextView>(R.id.nav_email_textview)

        nameTextView.text = if (profile.displayName.isNotEmpty()) profile.displayName else "Ace! Time Tracker"
        emailTextView.text = if (profile.email.isNotEmpty()) profile.email else "user@acetimetracker.com"

        if (profile.profilePictureUrl.isNotEmpty()) {
            Glide.with(this)
                .load(profile.profilePictureUrl)
                .transform(CircleCrop())
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_account_circle)
        }
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
