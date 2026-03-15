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
import com.example.klimboo.data.showGenericDisplay
import com.example.klimboo.databinding.ActivityLoginPageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class LoginPage : AppCompatActivity() {

    private lateinit var binding: ActivityLoginPageBinding
    private lateinit var auth: FirebaseAuth


    public override fun onStart() {
        super.onStart()
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            val intent = Intent(this@LoginPage, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        val themeManager = ThemeManager(this)
        observeTheme(themeManager)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        binding = ActivityLoginPageBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // ir para aba de registro
        binding.registerNow.setOnClickListener {
            val intent = Intent(this, RegisterPage::class.java)
            startActivity(intent)
            finish()
        }

        // Botao de login e verificacao de preenchimento de campo
        binding.btnLogin.setOnClickListener {
            binding.btnLogin.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE

            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            if (email.isEmpty()) {
                Toast.makeText(this@LoginPage, "Insira o E-mail", Toast.LENGTH_SHORT).show()
                binding.btnLogin.isEnabled = true
                binding.progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this@LoginPage, "Insira a Senha", Toast.LENGTH_SHORT).show()
                binding.btnLogin.isEnabled = true
                binding.progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            // verificar se senha/email estao corretos e se o email foi verificado
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = Firebase.auth.currentUser!!

                        if (!user.isEmailVerified) {
                            binding.btnLogin.isEnabled = true
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(this, "Verifique seu e-mail antes de continuar!", Toast.LENGTH_LONG).show()
                            Firebase.auth.signOut()
                            return@addOnCompleteListener
                        }

                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Iniciando sessão.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        binding.btnLogin.isEnabled = true
                        binding.progressBar.visibility = View.GONE

                        val error = when (task.exception) {
                            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Senha incorreta!"
                            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "E-mail não cadastrado!"
                            else -> "Erro: ${task.exception?.message}"
                        }
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    }
                }

            // Funcao para recuperar conta se esquecer senha
            binding.forgotPassword.setOnClickListener {
                showGenericDisplay("Recuperar Senha", "Digite seu e-mail para receber o link:", "email@exemplo.com", false) { email ->
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "E-mail de recuperação enviado!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Erro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}