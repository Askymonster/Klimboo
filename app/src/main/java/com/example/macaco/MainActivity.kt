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
    private lateinit var textName: TextView
    private lateinit var textEmail: TextView
    private lateinit var auth: FirebaseAuth



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main_page)

        imageConfig = findViewById(R.id.settings)
        textName = findViewById(R.id.user_name_main)
        textEmail = findViewById(R.id.user_email_main)

        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginPage::class.java))
            finish()
            return
        }


        textName.text = currentUser.displayName ?: "Nome não definido"
        textEmail.text = currentUser.email


        imageConfig.setOnClickListener {
            val intent = Intent(this, ConfigPage::class.java)
            startActivity(intent)
        }
    }
}