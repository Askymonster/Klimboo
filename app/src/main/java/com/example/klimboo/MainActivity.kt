package com.example.klimboo

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klimboo.data.FirebaseQueries
import com.example.klimboo.data.FirebaseQueries.Locker
import com.example.klimboo.data.FirebaseQueries.Tool
import com.example.klimboo.data.ThemeManager
import com.example.klimboo.data.observeTheme
import com.example.klimboo.databinding.ActivityMainPageBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainPageBinding
    private var allTools: List<Tool> = emptyList()
    private var allLockers: List<Locker> = emptyList()
    private var currentQuery: String = ""
    private var currentChip: String = ""
    private var lockersListener: ListenerRegistration? = null
    private var toolsListener: ListenerRegistration? = null

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

        val isGuest = currentUser.isAnonymous
        binding.displayEmail.text = if (isGuest) "Acesso limitado" else currentUser.displayName ?: "Nome não definido"
        binding.displayUsername.text = if (isGuest) "Visitante" else currentUser.email ?: ""
        binding.exitGuestButton.visibility = if (isGuest) View.VISIBLE else View.GONE

        // ── Lógica dos botões  ────────────────────────────────────────────────

        binding.exitGuestButton.setOnClickListener {
            Firebase.auth.signOut()
            val intent = Intent(this, LoginPage::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        binding.settings.setOnClickListener {
            if (currentUser.isAnonymous) {
                AlertDialog.Builder(this)
                    .setTitle("Login necessário")
                    .setMessage("Você precisa estar logado para acessar as configurações.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                startActivity(Intent(this, ConfigPage::class.java))


            }
        }
        binding.stock.setOnClickListener { startActivity(Intent(this, StockPage::class.java)) }

        binding.searchResults.layoutManager = LinearLayoutManager(this)

        // ── Botão de procura de itens ────────────────────────────────────────────────
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s?.toString()?.trim() ?: ""
                applyFilter()
            }
        })

        binding.chipTodos.setOnClickListener { currentChip = ""; applyFilter() }
        binding.chipChaves.setOnClickListener { currentChip = "chave"; applyFilter() }
        binding.chipFerramentas.setOnClickListener { currentChip = "parafuso"; applyFilter() }
        binding.chipPecas.setOnClickListener { currentChip = "broca"; applyFilter() }
    }

    // ── Lógica de atualização do database ────────────────────────────────────────────────
    override fun onStart() {
        super.onStart()

        lockersListener = FirebaseQueries.listenToLockers { lockers ->
            allLockers = lockers
            applyFilter()
        }

        toolsListener = FirebaseQueries.listenToTools { tools ->
            allTools = tools
            applyFilter()
        }
    }

    override fun onStop() {
        super.onStop()
        lockersListener?.remove()
        toolsListener?.remove()
    }

    // ── Lógica dos filtros ────────────────────────────────────────────────
    private fun applyFilter() {
        val query = currentQuery.lowercase()
        val chip = currentChip.lowercase()

        val filtered = allTools.filter { tool ->
            val matchesQuery = query.isEmpty() || tool.name.lowercase().contains(query)
            val matchesChip = chip.isEmpty() || tool.name.lowercase().contains(chip)
            matchesQuery && matchesChip
        }

        // Cria um mapa id -> nome do armário
        val lockerMap = allLockers.associate { it.id to it.local }

        binding.searchResults.visibility = View.VISIBLE
        binding.searchResults.adapter = StockAdapter(
            this,
            filtered.map { tool ->
                Locker(
                    id = tool.id,
                    name = tool.name,
                    local = lockerMap[tool.local] ?: "Local desconhecido",  // Busca o nome do local
                    photoUrl = tool.photoUrl
                )
            }
        )
    }
}
