package com.example.acetimetracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.acetimetracker.account.*

class profile : AccountActivity() {
    private val model by lazy { ViewModelProvider(this, AccountViewModelFactory(accounts))[ProfileViewModel::class.java] }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionReady) return
        setContentView(R.layout.activity_profile)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        findViewById<Button>(R.id.editProfileButton).setOnClickListener { startActivity(Intent(this, EditProfileActivity::class.java)) }
        findViewById<TextView>(R.id.emailTextView).text = sessionAccount.email.orEmpty()
        model.state.observe(this) { state ->
            if (currentSessionUid() == null) return@observe
            findViewById<TextView>(R.id.displayNameTextView).text = state.name.ifBlank { sessionAccount.displayName.orEmpty() }
            val image = findViewById<ImageView>(R.id.profileImageView)
            if (state.photo.isBlank()) image.setImageResource(R.drawable.ic_account_circle)
            else Glide.with(this).load(state.photo).transform(CircleCrop()).into(image)
        }
        model.load(sessionAccount)
    }
    override fun onResume() {
        super.onResume()
        if (sessionReady && model.state.value?.loaded == true) model.reload()
    }
}
