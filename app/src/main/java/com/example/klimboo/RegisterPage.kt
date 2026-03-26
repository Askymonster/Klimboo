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


class RegisterPage : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterPageBinding
    private val currentUser get() = Firebase.auth.currentUser

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
            startActivity(Intent(this, LoginPage::class.java))
            finish()
        }

        binding.btnRegister.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnRegister.isEnabled = false

            val username = binding.username.text.toString()
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            if (username.isEmpty()) {
                Toast.makeText(this, "Insira seu nome", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
                binding.progressBar.visibility = View.GONE
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                Toast.makeText(this, "Insira o E-mail", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
                binding.progressBar.visibility = View.GONE
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Insira a Senha", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = true
                binding.progressBar.visibility = View.GONE
                return@setOnClickListener
            }
            if (password.length !in 6..18) {
                Toast.makeText(this, "A senha deve ter entre 6 e 18 caracteres", Toast.LENGTH_SHORT).show()
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



                            // faz signOut pra não entrar automaticamente sem verificar email
                            Firebase.auth.signOut()

                            binding.btnRegister.isEnabled = true
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(this, "Verifique seu e-mail antes de continuar!", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, LoginPage::class.java))
                            finish()
                        }
                    } else {
                        binding.btnRegister.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Erro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}