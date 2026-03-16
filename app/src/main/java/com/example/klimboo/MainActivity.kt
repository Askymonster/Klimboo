package com.example.klimboo

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.klimboo.data.ThemeManager
import com.example.klimboo.data.observeTheme
import com.example.klimboo.databinding.ActivityMainPageBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainPageBinding




    override fun onCreate(savedInstanceState: Bundle?) {
        val themeManager = ThemeManager(this)
        observeTheme(themeManager)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val currentUser = Firebase.auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginPage::class.java))
            finish()
            return
        }

        binding.displayEmail.text = currentUser.displayName ?: "Nome não definido"
        binding.displayUsername.text = currentUser.email


        binding.settings.setOnClickListener {
            val intent = Intent(this, ConfigPage::class.java)
            startActivity(intent)
        }

        binding.stock.setOnClickListener {
            val intent = Intent(this, StockPage::class.java)
            startActivity(intent)
        }

        binding.qrcode.setOnClickListener {
            val intent = Intent(this, ScanPage::class.java)
            startActivity(intent)
        }
    }
}