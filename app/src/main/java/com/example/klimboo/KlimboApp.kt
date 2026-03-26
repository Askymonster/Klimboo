package com.example.klimboo

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.initialize
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class KlimbooApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
    }
}