package com.example.acetimetracker

import android.os.Parcel
import android.os.Parcelable

data class Category(
    val documentId: String = "",
    val name: String = "",
    val description: String = "",
    val minGoal: Float? = null,
    val maxGoal: Float? = null,
    val goal: Float? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        documentId = parcel.readString()!!, // Read documentId from Parcel
        name = parcel.readString()!!,
        description = parcel.readString()!!,
        minGoal = parcel.readValue(Float::class.java.classLoader) as? Float,
        maxGoal = parcel.readValue(Float::class.java.classLoader) as? Float,
        goal = parcel.readValue(Float::class.java.classLoader) as? Float
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(documentId) // Write documentId to Parcel
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeValue(minGoal)
        parcel.writeValue(maxGoal)
        parcel.writeValue(goal)
    }


    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Category> {
        override fun createFromParcel(parcel: Parcel): Category {
            return Category(parcel)
        }

        override fun newArray(size: Int): Array<Category?> {
            return arrayOfNulls(size)
        }
    }
}