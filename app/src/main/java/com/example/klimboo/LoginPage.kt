package com.example.klimboo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import androidx.appcompat.app.AlertDialog
import android.widget.EditText

class LoginPage : AppCompatActivity() {

    lateinit var editTextEmail: TextInputEditText
    lateinit var editTextPassword: TextInputEditText
    lateinit var auth: FirebaseAuth
    lateinit var buttonLogin: Button
    lateinit var progressBar: ProgressBar
    lateinit var textView_toRegister: TextView

    lateinit var forgotPassword: TextView

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this@LoginPage, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("MissingInflatedId")
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
        editTextEmail = findViewById(R.id.email)
        editTextPassword = findViewById(R.id.password)
        buttonLogin = findViewById(R.id.btn_login)
        progressBar = findViewById(R.id.progressBar)
        textView_toRegister = findViewById(R.id.registerNow)
        forgotPassword = findViewById(R.id.forgotPassword)




        forgotPassword.setOnClickListener {

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Recuperar Senha")
            builder.setMessage("Digite seu email para receber o link de recuperação.")

            val input = EditText(this)
            input.hint = "Seu email"
            input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

            builder.setView(input)

            builder.setPositiveButton("Enviar") { dialog, _ ->

                val email = input.text.toString().trim()

                if (email.isEmpty()) {
                    Toast.makeText(this, "Digite um email válido", Toast.LENGTH_SHORT).show()
                } else {

                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener {

                            // 👇 COLOQUE AQUI
                            Toast.makeText(
                                this,
                                "Se este email estiver cadastrado, você receberá um link de recuperação.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }

                dialog.dismiss()
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }

        textView_toRegister.setOnClickListener {
            val intent = Intent(this, RegisterPage::class.java)
            startActivity(intent)
            finish()
        }

        buttonLogin.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()

            if (email.isEmpty()) {
                Toast.makeText(this@LoginPage, "Insira o E-mail", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this@LoginPage, "Insira a Senha", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            baseContext,
                            "Conta existente. Iniciando sessão.",
                            Toast.LENGTH_SHORT,
                        ).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        progressBar.visibility = View.GONE
                        // If sign in fails, display a message to the user.
                        Toast.makeText(
                            baseContext,
                            "Falha na inicialização.",
                            Toast.LENGTH_SHORT,

                        ).show()
                    }
                }
        }
    }
}