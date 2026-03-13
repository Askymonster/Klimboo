package com.example.klimboo

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.klimboo.data.ThemeManager
import com.example.klimboo.data.observeTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.klimboo.data.SupabaseQueries

class MainActivity : AppCompatActivity() {

    private lateinit var imageConfig: ImageView
    private lateinit var imageStock: ImageView
    private lateinit var imageQR: ImageView
    private lateinit var textNameMain: TextView
    private lateinit var textEmailMain: TextView
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
        imageConfig = findViewById(R.id.settings)
        imageStock = findViewById(R.id.stock)
        imageQR =  findViewById(R.id.qrcode)
        textNameMain = findViewById(R.id.user_name_main)
        textEmailMain  = findViewById(R.id.user_email_main)


        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginPage::class.java))
            finish()
            return
        }

        textNameMain.text = currentUser.displayName ?: "Nome não definido"
        textEmailMain.text = currentUser.email


        imageConfig.setOnClickListener {
            val intent = Intent(this, ConfigPage::class.java)
            startActivity(intent)
        }

        imageStock.setOnClickListener {
            val intent = Intent(this, StockPage::class.java)
            startActivity(intent)
        }

        imageQR.setOnClickListener {
            val intent = Intent(this, ScanPage::class.java)
            startActivity(intent)
        }
    }
}