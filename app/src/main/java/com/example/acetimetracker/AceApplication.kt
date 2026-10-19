package com.example.acetimetracker

import android.app.Application
import com.example.acetimetracker.account.AccountServices
import com.example.acetimetracker.account.firebaseAccountServices

open class AceApplication : Application() {
    open val accounts: AccountServices by lazy { firebaseAccountServices() }
}
