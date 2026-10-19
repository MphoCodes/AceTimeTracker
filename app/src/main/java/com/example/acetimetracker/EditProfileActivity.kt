package com.example.acetimetracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.acetimetracker.account.*

class EditProfileActivity : AccountActivity() {
    private val model by lazy { ViewModelProvider(this, AccountViewModelFactory(accounts))[ProfileViewModel::class.java] }
    private var displayedPhoto: String? = null
    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && currentSessionUid() != null) result.data?.data?.let(model::upload)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionReady) return
        setContentView(R.layout.activity_edit_profile)
        val name = findViewById<EditText>(R.id.editDisplayNameEditText)
        findViewById<EditText>(R.id.editEmailEditText).apply { setText(sessionAccount.email.orEmpty()); isEnabled = false }
        name.doAfterTextChanged { model.editName(it.toString()) }
        findViewById<Button>(R.id.saveProfileChangesButton).setOnClickListener {
            if (!model.state.value!!.loaded) model.reload() else model.save()
        }
        findViewById<Button>(R.id.uploadProfilePictureButton).setOnClickListener {
            picker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) })
        }
        model.state.observe(this) { state ->
            if (currentSessionUid() == null) return@observe
            if (name.text.toString() != state.name) name.setText(state.name)
            name.isEnabled = state.loaded && !state.saving
            val busy = state.loading || state.saving || state.uploading
            findViewById<Button>(R.id.saveProfileChangesButton).apply {
                isEnabled = !busy
                setText(if (!state.loaded) R.string.account_retry else R.string.account_save)
            }
            findViewById<Button>(R.id.uploadProfilePictureButton).isEnabled = state.loaded && !busy
            findViewById<View>(R.id.profilePictureLoadingAnimation).visibility = if (busy) View.VISIBLE else View.INVISIBLE
            findViewById<TextView>(R.id.profileStatusTextView).text = when {
                state.saving -> getString(R.string.account_saving)
                busy -> getString(R.string.account_working)
                else -> state.problem?.let { getString(it.messageResource()) }.orEmpty()
            }
            if (displayedPhoto != state.photo) {
                displayedPhoto = state.photo
                val image = findViewById<ImageView>(R.id.profileImageView)
                if (state.photo.isBlank()) image.setImageResource(R.drawable.ic_account_circle)
                else Glide.with(this).load(state.photo).transform(CircleCrop()).into(image)
            }
            if (state.saved) finish()
        }
        model.load(sessionAccount)
    }
}
