package com.example.acetimetracker.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val documentId: String = "",
    val name: String = "",
    val description: String = "",
    val minGoal: Float? = null,
    val maxGoal: Float? = null,
    val goal: Float? = null
) : Parcelable
