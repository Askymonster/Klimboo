package com.example.macaco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class                 MainActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var button: Button
    lateinit var textView: TextView
    lateinit var user: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_page)

        auth = FirebaseAuth.getInstance()
        button = findViewById(R.id.logout)
        textView = findViewById(R.id.user_details)

        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginPage::class.java))
            finish()
        } else {
            user = currentUser
            textView.text = user.email
        }

        button.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginPage::class.java))
            finish()
        }
    }
}