package com.example.klimboo

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.klimboo.data.ThemeManager
import com.example.klimboo.data.observeTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.klimboo.data.SupabaseQueries
import com.example.klimboo.databinding.ActivityMainPageBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainPageBinding
    private lateinit var auth: FirebaseAuth



    override fun onCreate(savedInstanceState: Bundle?) {
        val themeManager = ThemeManager(this)
        observeTheme(themeManager) // Resolve o tema para essa tela

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)
        enableEdgeToEdge()


        lifecycleScope.launch {
            SupabaseQueries.testeFetch()
        }

        auth = FirebaseAuth.getInstance()
        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = auth.currentUser

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