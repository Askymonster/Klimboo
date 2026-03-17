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
import com.example.klimboo.data.SupabaseQueries
import com.example.klimboo.data.SupabaseQueries.Armario
import com.example.klimboo.data.SupabaseQueries.Ferramenta
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
                armarios = SupabaseQueries.fetchArmarios()
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
                    SupabaseQueries.insertArmario(nome)
                    toast("Armário '$nome' adicionado!")
                } else {
                    val nome = b.editNomeItem.text.toString().trim()
                    if (nome.isEmpty()) { toast("Informe o nome do item"); return@launch }
                    val armario = armarios.getOrNull(b.spinnerArmarioDestino.selectedItemPosition)
                    if (armario == null) { toast("Selecione um armário"); return@launch }
                    SupabaseQueries.insertFerramenta(nome, armario.id)
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

        lifecycleScope.launch {
            armarios = SupabaseQueries.fetchArmarios()
            ferramentas = SupabaseQueries.fetchFerramentas()
            b.spinnerSelecionarArmario.adapter = spinnerAdapter(armarios.map { it.nome })
            b.spinnerSelecionarItem.adapter = spinnerAdapter(ferramentas.map { it.nome })
            b.spinnerMoverParaArmario.adapter = spinnerAdapter(armarios.map { it.nome })
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
                    val armario = armarios.getOrNull(b.spinnerSelecionarArmario.selectedItemPosition)
                    if (armario == null) { toast("Selecione um armário"); return@launch }
                    val novoNome = b.editNovoNomeArmario.text.toString().trim()
                    if (novoNome.isEmpty()) { toast("Informe o novo nome"); return@launch }
                    SupabaseQueries.updateArmario(armario.id, novoNome)
                    toast("Armário renomeado para '$novoNome'!")
                } else {
                    val ferramenta = ferramentas.getOrNull(b.spinnerSelecionarItem.selectedItemPosition)
                    if (ferramenta == null) { toast("Selecione um item"); return@launch }
                    val novoNome = b.editNovoNomeItem.text.toString().trim()
                    if (novoNome.isEmpty()) { toast("Informe o novo nome"); return@launch }
                    val destino = armarios.getOrNull(b.spinnerMoverParaArmario.selectedItemPosition)
                    if (destino == null) { toast("Selecione o armário destino"); return@launch }
                    SupabaseQueries.updateFerramenta(ferramenta.id, novoNome, destino.id)
                    toast("Item atualizado!")
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showDeleteSheet() {
        val dialog = BottomSheetDialog(this)
        val b = BottomSheetDeleteBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        var armarios: List<Armario> = emptyList()
        var ferramentas: List<Ferramenta> = emptyList()

        lifecycleScope.launch {
            armarios = SupabaseQueries.fetchArmarios()
            ferramentas = SupabaseQueries.fetchFerramentas()
            b.spinnerArmarioParaRemover.adapter = spinnerAdapter(armarios.map { it.nome })
            b.spinnerItemParaRemover.adapter = spinnerAdapter(ferramentas.map { it.nome })
            atualizarSpinnerDestino(b, armarios, 0)
        }

        b.spinnerArmarioParaRemover.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    atualizarSpinnerDestino(b, armarios, pos)
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

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
                    val armario = armarios.getOrNull(b.spinnerArmarioParaRemover.selectedItemPosition)
                    if (armario == null) { toast("Selecione um armário"); return@launch }
                    val outros = armarios.filter { it.id != armario.id }
                    val destino = outros.getOrNull(b.spinnerDestinoItens.selectedItemPosition)
                    SupabaseQueries.deleteArmario(armario.id, destino?.id)
                    toast(if (destino != null) "Itens movidos para '${destino.nome}'." else "Armário e itens removidos.")
                } else {
                    val ferramenta = ferramentas.getOrNull(b.spinnerItemParaRemover.selectedItemPosition)
                    if (ferramenta == null) { toast("Selecione um item"); return@launch }
                    SupabaseQueries.deleteFerramenta(ferramenta.id)
                    toast("Item '${ferramenta.nome}' removido!")
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun atualizarSpinnerDestino(b: BottomSheetDeleteBinding, armarios: List<Armario>, selectedIndex: Int) {
        val removido = armarios.getOrNull(selectedIndex)
        val outros = armarios.filter { it.id != removido?.id }
        if (outros.isEmpty()) {
            b.spinnerDestinoItens.adapter = spinnerAdapter(emptyList())
            b.spinnerDestinoItens.isEnabled = false
            b.txtAvisoSemDestino.visibility = View.VISIBLE
        } else {
            b.spinnerDestinoItens.adapter = spinnerAdapter(outros.map { it.nome })
            b.spinnerDestinoItens.isEnabled = true
            b.txtAvisoSemDestino.visibility = View.GONE
        }
    }
}