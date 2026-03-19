package com.example.klimboo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.klimboo.data.FirebaseQueries
import com.example.klimboo.data.FirebaseQueries.Armario
import com.example.klimboo.data.FirebaseQueries.Ferramenta
import com.example.klimboo.databinding.ActivityStockPageBinding
import com.example.klimboo.databinding.BottomSheetAddBinding
import com.example.klimboo.databinding.BottomSheetBinding
import com.example.klimboo.databinding.BottomSheetDeleteBinding
import com.example.klimboo.databinding.BottomSheetEditBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class StockPage : AppCompatActivity() {

    private lateinit var binding: ActivityStockPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityStockPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        FirebaseFirestore.getInstance().collection("usuarios")
            .document(Firebase.auth.currentUser!!.uid)
            .get()
            .addOnSuccessListener { document ->
                val isAdmin = document.getBoolean("isAdmin") ?: false
                binding.editStock.visibility = if (isAdmin) View.VISIBLE else View.GONE
                binding.editStock.setOnClickListener { showMainSheet() }
            }
    }

    private fun showMainSheet() {
        val dialogBinding = BottomSheetBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialogBinding.btnAdd.setOnClickListener { dialog.dismiss(); showAddSheet() }
        dialogBinding.btnEdit.setOnClickListener { dialog.dismiss(); showEditSheet() }
        dialogBinding.btnDelete.setOnClickListener { dialog.dismiss(); showDeleteSheet() }
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun spinnerAdapter(items: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

    private fun showAddSheet() {
        val dialog = BottomSheetDialog(this)
        val b = BottomSheetAddBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)
        dialog.show()

        var armarios: List<Armario> = emptyList()

        lifecycleScope.launch {
            try {
                armarios = FirebaseQueries.fetchArmarios()
                Log.d("DEBUG_SPINNER", "Armários: ${armarios.size} → $armarios")
                runOnUiThread {
                    b.spinnerArmarioDestino.adapter = spinnerAdapter(armarios.map { it.nome })
                }
            } catch (e: Exception) {
                Log.e("DEBUG_SPINNER", "Erro: ${e.message}", e)
            }
        }

        b.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isArmario = checkedId == R.id.btnToggleArmario
                b.layoutAddArmario.visibility = if (isArmario) View.VISIBLE else View.GONE
                b.layoutAddItem.visibility = if (isArmario) View.GONE else View.VISIBLE
            }
        }

        b.btnConfirmarAdd.setOnClickListener {
            val isArmario = b.toggleGroup.checkedButtonId == R.id.btnToggleArmario
            lifecycleScope.launch {
                if (isArmario) {
                    val nome = b.editNomeArmario.text.toString().trim()
                    if (nome.isEmpty()) { toast("Informe o nome do armário"); return@launch }
                    FirebaseQueries.insertArmario(nome)
                    toast("Armário '$nome' adicionado!")
                } else {
                    val nome = b.editNomeItem.text.toString().trim()
                    if (nome.isEmpty()) { toast("Informe o nome do item"); return@launch }
                    val armario = armarios.getOrNull(b.spinnerArmarioDestino.selectedItemPosition)
                    if (armario == null) { toast("Selecione um armário"); return@launch }
                    FirebaseQueries.insertFerramenta(nome, armario.id)
                    toast("Item '$nome' adicionado!")
                }
                dialog.dismiss()
            }
        }
    }

    private fun showEditSheet() {
        val dialog = BottomSheetDialog(this)
        val b = BottomSheetEditBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        var armarios: List<Armario> = emptyList()
        var ferramentas: List<Ferramenta> = emptyList()
        var armarioSelecionado: Armario? = null
        var ferramentaSelecionada: Ferramenta? = null
        var armarioDestinoSelecionado: Armario? = null

        // checkboxes controlam visibilidade
        b.checkAlterarNomeArmario.setOnCheckedChangeListener { _, checked ->
            b.layoutNovoNomeArmario.visibility = if (checked) View.VISIBLE else View.GONE
        }
        b.checkAlterarNomeItem.setOnCheckedChangeListener { _, checked ->
            b.layoutNovoNomeItem.visibility = if (checked) View.VISIBLE else View.GONE
        }
        b.checkAlterarLocalItem.setOnCheckedChangeListener { _, checked ->
            b.layoutMoverParaArmario.visibility = if (checked) View.VISIBLE else View.GONE
        }

        b.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isArmario = checkedId == R.id.btnToggleArmario
                b.layoutEditArmario.visibility = if (isArmario) View.VISIBLE else View.GONE
                b.layoutEditItem.visibility = if (isArmario) View.GONE else View.VISIBLE
            }
        }

        b.btnConfirmarEdit.setOnClickListener {
            val isArmario = b.toggleGroup.checkedButtonId == R.id.btnToggleArmario
            lifecycleScope.launch {
                if (isArmario) {
                    if (armarioSelecionado == null) { toast("Selecione um armário"); return@launch }
                    if (!b.checkAlterarNomeArmario.isChecked) { toast("Selecione o que deseja alterar"); return@launch }
                    val novoNome = b.editNovoNomeArmario.text.toString().trim()
                    if (novoNome.isEmpty()) { toast("Informe o novo nome"); return@launch }
                    FirebaseQueries.updateArmario(armarioSelecionado!!.id, novoNome)
                    toast("Armário renomeado para '$novoNome'!")
                } else {
                    if (ferramentaSelecionada == null) { toast("Selecione um item"); return@launch }
                    if (!b.checkAlterarNomeItem.isChecked && !b.checkAlterarLocalItem.isChecked) {
                        toast("Selecione o que deseja alterar"); return@launch
                    }
                    val novoNome = if (b.checkAlterarNomeItem.isChecked) {
                        b.editNovoNomeItem.text.toString().trim().also {
                            if (it.isEmpty()) { toast("Informe o novo nome"); return@launch }
                        }
                    } else ferramentaSelecionada!!.nome

                    val novoArmarioId = if (b.checkAlterarLocalItem.isChecked) {
                        armarioDestinoSelecionado?.id.also {
                            if (it == null) { toast("Selecione o armário destino"); return@launch }
                        } ?: return@launch
                    } else ferramentaSelecionada!!.local

                    FirebaseQueries.updateFerramenta(ferramentaSelecionada!!.id, novoNome, novoArmarioId)
                    toast("Item atualizado!")
                }
                dialog.dismiss()
            }
        }

        lifecycleScope.launch {
            armarios = FirebaseQueries.fetchArmarios()
            ferramentas = FirebaseQueries.fetchFerramentas()

            val adapterArmarios = ArrayAdapter(this@StockPage, android.R.layout.simple_dropdown_item_1line, armarios.map { it.nome })
            val adapterFerramentas = ArrayAdapter(this@StockPage, android.R.layout.simple_dropdown_item_1line, ferramentas.map { it.nome })
            val adapterDestinoArmarios = ArrayAdapter(this@StockPage, android.R.layout.simple_dropdown_item_1line, armarios.map { it.nome })

            b.autoCompleteArmario.setAdapter(adapterArmarios)
            b.autoCompleteItem.setAdapter(adapterFerramentas)
            b.autoCompleteArmarioDestino.setAdapter(adapterDestinoArmarios)

            b.autoCompleteArmario.setOnItemClickListener { _, _, pos, _ ->
                armarioSelecionado = armarios.getOrNull(pos)
            }
            b.autoCompleteItem.setOnItemClickListener { _, _, pos, _ ->
                ferramentaSelecionada = ferramentas.getOrNull(pos)
            }
            b.autoCompleteArmarioDestino.setOnItemClickListener { _, _, pos, _ ->
                armarioDestinoSelecionado = armarios.getOrNull(pos)
            }

            dialog.show()
        }
    }

    private fun showDeleteSheet() {
        val dialog = BottomSheetDialog(this)
        val b = BottomSheetDeleteBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        var armarios: List<Armario> = emptyList()
        var ferramentas: List<Ferramenta> = emptyList()
        var armarioSelecionado: Armario? = null
        var armarioDestinoSelecionado: Armario? = null
        var ferramentaSelecionada: Ferramenta? = null

        b.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isArmario = checkedId == R.id.btnToggleArmario
                b.layoutDeleteArmario.visibility = if (isArmario) View.VISIBLE else View.GONE
                b.layoutDeleteItem.visibility = if (isArmario) View.GONE else View.VISIBLE
            }
        }

        b.btnConfirmarDelete.setOnClickListener {
            val isArmario = b.toggleGroup.checkedButtonId == R.id.btnToggleArmario
            lifecycleScope.launch {
                if (isArmario) {
                    if (armarioSelecionado == null) { toast("Selecione um armário"); return@launch }
                    FirebaseQueries.deleteArmario(armarioSelecionado!!.id, armarioDestinoSelecionado?.id)
                    toast(if (armarioDestinoSelecionado != null) "Itens movidos para '${armarioDestinoSelecionado!!.nome}'." else "Armário e itens removidos.")
                } else {
                    if (ferramentaSelecionada == null) { toast("Selecione um item"); return@launch }
                    FirebaseQueries.deleteFerramenta(ferramentaSelecionada!!.id)
                    toast("Item '${ferramentaSelecionada!!.nome}' removido!")
                }
                dialog.dismiss()
            }
        }

        lifecycleScope.launch {
            armarios = FirebaseQueries.fetchArmarios()
            ferramentas = FirebaseQueries.fetchFerramentas()

            val adapterArmarios = ArrayAdapter(this@StockPage, android.R.layout.simple_dropdown_item_1line, armarios.map { it.nome })
            val adapterFerramentas = ArrayAdapter(this@StockPage, android.R.layout.simple_dropdown_item_1line, ferramentas.map { it.nome })
            val adapterDestino = ArrayAdapter(this@StockPage, android.R.layout.simple_dropdown_item_1line, armarios.map { it.nome })

            b.autoCompleteArmarioRemover.setAdapter(adapterArmarios)
            b.autoCompleteItemRemover.setAdapter(adapterFerramentas)
            b.autoCompleteDestinoItens.setAdapter(adapterDestino)

            b.autoCompleteArmarioRemover.setOnItemClickListener { _, _, pos, _ ->
                armarioSelecionado = armarios.getOrNull(pos)
                // atualiza destino excluindo o selecionado
                val outros = armarios.filter { it.id != armarioSelecionado?.id }
                val novoAdapter = ArrayAdapter(this@StockPage, android.R.layout.simple_dropdown_item_1line, outros.map { it.nome })
                b.autoCompleteDestinoItens.setAdapter(novoAdapter)
                b.autoCompleteDestinoItens.text.clear()
                armarioDestinoSelecionado = null
                if (outros.isEmpty()) {
                    b.layoutDestinoItens.isEnabled = false
                    b.txtAvisoSemDestino.visibility = View.VISIBLE
                } else {
                    b.layoutDestinoItens.isEnabled = true
                    b.txtAvisoSemDestino.visibility = View.GONE
                }
            }

            b.autoCompleteDestinoItens.setOnItemClickListener { _, _, pos, _ ->
                val outros = armarios.filter { it.id != armarioSelecionado?.id }
                armarioDestinoSelecionado = outros.getOrNull(pos)
            }

            b.autoCompleteItemRemover.setOnItemClickListener { _, _, pos, _ ->
                ferramentaSelecionada = ferramentas.getOrNull(pos)
            }

            dialog.show()
        }
    }

}