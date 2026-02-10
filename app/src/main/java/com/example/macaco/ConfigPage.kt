package com.example.macaco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth

class ConfigPage : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var buttonLogout: Button
    lateinit var textName: TextView
    lateinit var textEmail: TextView
    lateinit var buttonBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_config_page)

        auth = FirebaseAuth.getInstance()
        buttonLogout = findViewById(R.id.logout)
        buttonBack = findViewById(R.id.backtomain)
        textName = findViewById(R.id.user_details_name)
        textEmail = findViewById(R.id.user_details_email)

        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginPage::class.java))
            finish()
            return
        }


        textName.text = currentUser.displayName ?: "Nome não definido"
        textEmail.text = currentUser.email


        buttonBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        buttonLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginPage::class.java))
            finish()
        }


        val switchTheme: MaterialSwitch = findViewById(R.id.switch_theme)
        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }
}