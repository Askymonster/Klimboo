package com.example.klimboo

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klimboo.data.FirebaseQueries
import com.example.klimboo.data.FirebaseQueries.Ferramenta
import com.example.klimboo.data.ThemeManager
import com.example.klimboo.data.observeTheme
import com.example.klimboo.databinding.ActivityMainPageBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainPageBinding
    private var allTools: List<Ferramenta> = emptyList()
    private var currentQuery: String = ""
    private var currentChip: String = ""  // "" = todos

    override fun onCreate(savedInstanceState: Bundle?) {
        val themeManager = ThemeManager(this)
        observeTheme(themeManager)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginPage::class.java))
            finish()
            return
        }

        binding.displayEmail.text = currentUser.displayName ?: "Nome não definido"
        binding.displayUsername.text = currentUser.email

        binding.settings.setOnClickListener { startActivity(Intent(this, ConfigPage::class.java)) }
        binding.stock.setOnClickListener { startActivity(Intent(this, StockPage::class.java)) }
        binding.qrcode.setOnClickListener { startActivity(Intent(this, ScanPage::class.java)) }

        binding.searchResults.layoutManager = LinearLayoutManager(this)

        // Carrega todos os itens
        lifecycleScope.launch {
            allTools = FirebaseQueries.fetchFerramentas()
        }

        // Campo de busca
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s?.toString()?.trim() ?: ""
                applyFilter()
            }
        })

        // Chips
        binding.chipTodos.setOnClickListener { currentChip = ""; applyFilter() }
        binding.chipChaves.setOnClickListener { currentChip = binding.chipChaves.text.toString(); applyFilter() }
        binding.chipFerramentas.setOnClickListener { currentChip = binding.chipFerramentas.text.toString(); applyFilter() }
        binding.chipPecas.setOnClickListener { currentChip = binding.chipPecas.text.toString(); applyFilter() }
    }

    private fun applyFilter() {
        val query = currentQuery.lowercase()
        val chip = currentChip.lowercase()

        val filtered = allTools.filter { tool ->
            val matchesQuery = query.isEmpty() || tool.nome.lowercase().contains(query)
            val matchesChip = chip.isEmpty() || tool.nome.lowercase().contains(chip)
            matchesQuery && matchesChip
        }

        val showResults = query.isNotEmpty() || currentChip.isNotEmpty()
        binding.searchResults.visibility = if (showResults) View.VISIBLE else View.GONE

        if (showResults) {
            binding.searchResults.adapter = StockAdapter(
                this,
                filtered.map { it.nome to it.photoUrl }
            )
        }
    }
}