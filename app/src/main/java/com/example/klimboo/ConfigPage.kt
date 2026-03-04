package com.example.klimboo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class ConfigPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var buttonLogout: Button
    private lateinit var textName: TextView
    private lateinit var textEmail: TextView
    private lateinit var buttonBack: Button
    private lateinit var themeManager: ThemeManager
    private lateinit var changeEmail: Button
    private lateinit var changePassword: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_config_page)

        // 1. Inicializar Views
        auth = FirebaseAuth.getInstance()
        buttonLogout = findViewById(R.id.logout)
        buttonBack = findViewById(R.id.backtomain)
        textName = findViewById(R.id.user_details_name)
        textEmail = findViewById(R.id.user_details_email)
        changeEmail = findViewById(R.id.changeEmail)
        changePassword = findViewById(R.id.changePassword)



        // Display de nome/email
        val currentUser = Firebase.auth.currentUser
        val emailUser = currentUser?.email
        if (currentUser == null) {
            startActivity(Intent(this, LoginPage::class.java))
            finish()
            return
        }

        textName.text = currentUser.displayName ?: "Nome não definido"
        textEmail.text = emailUser



        // 2. Listener: Trocar Senha (E-mail de recuperação)
        changePassword.setOnClickListener {
            if (emailUser != null) {
                Firebase.auth.sendPasswordResetEmail(emailUser).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Link de redefinição enviado para: $emailUser", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // 3. Listener: Trocar email (requerimento de senha)
        changeEmail.setOnClickListener {
            showGenericDisplay("Confirmação", "Digite sua senha atual para continuar:", "Senha", true) { password ->
                val credential = EmailAuthProvider.getCredential(currentUser.email!!, password)

                currentUser.reauthenticate(credential).addOnSuccessListener {
                    showGenericDisplay("Novo E-mail", "Para qual e-mail deseja alterar?", "novo@email.com", false) { novoEmail ->
                        currentUser.verifyBeforeUpdateEmail(novoEmail).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Sucesso! Verifique o link no novo e-mail.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Senha incorreta!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 4. Listener de Logout, backtomain
        buttonBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        buttonLogout.setOnClickListener {
            Firebase.auth.signOut()
            startActivity(Intent(this, LoginPage::class.java))
            finish()
        }


        // 5. Troca de tema escuro/claro
        themeManager = ThemeManager(this)
        val switchTheme: MaterialSwitch = findViewById(R.id.switch_theme)
        lifecycleScope.launch {
            themeManager.isDarkMode.collect { isDark ->
                switchTheme.isChecked = isDark
            }
        }
        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                themeManager.setDarkMode(isChecked)
                val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }
}