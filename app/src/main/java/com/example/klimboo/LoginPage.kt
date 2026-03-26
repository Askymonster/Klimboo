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
import com.example.klimboo.data.ThemeManager
import com.example.klimboo.data.observeTheme
import com.example.klimboo.data.showGenericDisplay
import com.example.klimboo.databinding.ActivityLoginPageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

class LoginPage : AppCompatActivity() {

    private lateinit var binding: ActivityLoginPageBinding
    private lateinit var progressBar: ProgressBar
    private val currentUser get() = Firebase.auth.currentUser
    private lateinit var auth: FirebaseAuth

    public override fun onStart() {
        super.onStart()
        if (currentUser != null && currentUser!!.isEmailVerified) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val themeManager = ThemeManager(this)
        observeTheme(themeManager)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()



        binding = ActivityLoginPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        progressBar = binding.progressBar

        binding.forgotPassword.setOnClickListener {
            showGenericDisplay("Recuperar Senha", "Digite seu e-mail para receber o link:", "email@exemplo.com", false) { email ->
                FirebaseFirestore.getInstance().collection("usuarios")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { result ->
                        if (result.isEmpty) {
                            Toast.makeText(this, "Nenhuma conta encontrada com esse e-mail.", Toast.LENGTH_SHORT).show()
                        } else {
                            FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "E-mail de recuperação enviado!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this, "Erro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao verificar e-mail.", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        binding.registerNow.setOnClickListener {
            startActivity(Intent(this, RegisterPage::class.java))
            finish()
        }

        binding.btnLogin.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            if (email.isEmpty()) {
                Toast.makeText(this, "Insira o E-mail", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Insira a Senha", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        if (currentUser != null && currentUser!!.isEmailVerified) {
                            Toast.makeText(this, "Conta existente. Iniciando sessão.", Toast.LENGTH_SHORT).show()


                            val userUpdate = hashMapOf(
                                "email" to currentUser!!.email
                            )

                            Firebase.firestore
                                .collection("usuarios")
                                .document(currentUser!!.uid)
                                .set(userUpdate, SetOptions.merge())

                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            progressBar.visibility = View.GONE
                            Firebase.auth.signOut()
                            Toast.makeText(this, "Verifique seu e-mail antes de continuar!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Falha na inicialização.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}