package com.example.klimboo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.klimboo.data.ThemeManager
import com.example.klimboo.data.observeTheme
import com.example.klimboo.databinding.ActivityRegisterPageBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class RegisterPage : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterPageBinding
    private val currentUser get() = Firebase.auth.currentUser

    public override fun onStart() {
        super.onStart()
        if (currentUser != null && currentUser!!.isEmailVerified) {
            val intent = Intent(this@RegisterPage, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        val themeManager = ThemeManager(this)
        observeTheme(themeManager)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRegisterPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        binding.loginNow.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnRegister.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnRegister.isEnabled = false

            val username = binding.username.text.toString()
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            if (username.isEmpty()) {
                Toast.makeText(this@RegisterPage, "Insira seu nome", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
                binding.progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                Toast.makeText(this@RegisterPage, "Insira o E-mail", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
                binding.progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this@RegisterPage, "Insira a Senha", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
                binding.progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (password.length !in 6..25) {
                Toast.makeText(this@RegisterPage, "A senha deve ter entre 6 e 25 caracteres", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
                binding.progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            Firebase.auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build()

                        currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener {
                            currentUser!!.sendEmailVerification()

                            Firebase.firestore
                                .collection("usuarios")
                                .document(currentUser!!.uid)
                                .set(hashMapOf(
                                    "email" to currentUser!!.email,
                                    "isAdmin" to false
                                ))

                            binding.btnRegister.isEnabled = true
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(this, "Verifique seu e-mail antes de continuar!", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, LoginPage::class.java))
                            finish()
                        }
                    } else {
                        binding.btnRegister.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(baseContext, "Erro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }


        }



}
