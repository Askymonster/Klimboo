package com.example.macaco

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_page)

        auth = FirebaseAuth.getInstance()
        textView = findViewById(R.id.settings)

        textView.setOnClickListener {
            val intent = Intent(this, ConfigPage::class.java)
            startActivity(intent)
            finish()
        }





    }
}