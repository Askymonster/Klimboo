package com.example.klimboo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.klimboo.databinding.ActivityRegisterPageBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterPage : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterPageBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this@RegisterPage, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        progressBar = findViewById(R.id.progressBar)

        binding = ActivityRegisterPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginNow.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnRegister.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            val username = binding.username.toString()
            val email = binding.email.toString()
            val password = binding.password.toString()


            if (username.isEmpty()) {
                Toast.makeText(this@RegisterPage, "Insira seu nome", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                Toast.makeText(this@RegisterPage, "Insira o E-mail", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.password.toString().isEmpty()) {
                Toast.makeText(this@RegisterPage, "Insira a Senha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {

                        val user = auth.currentUser

                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build()

                        user?.updateProfile(profileUpdates)?.addOnCompleteListener {

                            progressBar.visibility = View.GONE
                            startActivity(Intent(this, LoginPage::class.java))
                            finish()
                        }
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(baseContext, "Erro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }

                }


        }



    }
}