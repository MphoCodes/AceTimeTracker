package com.example.acetimetracker

import android.os.Bundle
import com.example.acetimetracker.account.AuthEntryActivity

class Splash : AuthEntryActivity() {
    override val startup = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }
}
