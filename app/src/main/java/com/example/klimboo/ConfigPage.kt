package com.example.klimboo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ConfigPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var buttonLogout: Button
    private lateinit var textName: TextView
    private lateinit var textEmail: TextView
    private lateinit var buttonBack: Button
    private lateinit var themeManager: ThemeManager

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


        themeManager = ThemeManager(this)
        val switchTheme: MaterialSwitch = findViewById(R.id.switch_theme)

        lifecycleScope.launch {
            themeManager.isDarkMode.collect { isDark ->
                switchTheme.isChecked = isDark
            }
        }

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                themeManager.setDarkMode(isChecked)
                val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

}




