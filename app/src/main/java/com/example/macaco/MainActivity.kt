package com.example.macaco

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var imageConfig: ImageView
    private lateinit var imageStock: ImageView
    private lateinit var textNameMain: TextView
    private lateinit var textEmailMain: TextView
    private lateinit var auth: FirebaseAuth



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main_page)

        auth = FirebaseAuth.getInstance()
        imageConfig = findViewById(R.id.settings)
        imageStock = findViewById(R.id.stock)
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
    }
}