package com.example.klimboo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.materialswitch.MaterialSwitch
import androidx.lifecycle.lifecycleScope
import com.example.klimboo.data.ThemeManager
import com.example.klimboo.data.showGenericDisplay
import com.example.klimboo.databinding.ActivityConfigPageBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class ConfigPage : AppCompatActivity() {

    private lateinit var binding: ActivityConfigPageBinding
    private lateinit var themeManager: ThemeManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityConfigPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Display de nome/email
        val currentUser = Firebase.auth.currentUser
        val emailUser = currentUser?.email
        if (currentUser == null) {
            startActivity(Intent(this, LoginPage::class.java))
            finish()
            return
        }

        binding.userName.text = currentUser.displayName ?: "Nome não definido"
        binding.userEmail.text = emailUser


        // 2. Listener: Trocar Senha (E-mail de recuperação)
        binding.changePassword.setOnClickListener {
            if (emailUser != null) {
                Firebase.auth.sendPasswordResetEmail(emailUser).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Link de redefinição enviado para: $emailUser", Toast.LENGTH_LONG).show()
                    }
                    else {
                        Toast.makeText(this, "Erro ao enviar link!" , Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 3. Listener: Trocar email (requerimento de senha)
        binding.changeEmail.setOnClickListener {
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
        binding.backtomainButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.logoutButton.setOnClickListener {
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