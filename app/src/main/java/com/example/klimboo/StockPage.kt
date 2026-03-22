package com.example.klimboo

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.klimboo.data.FirebaseQueries
import com.example.klimboo.data.FirebaseQueries.Armario
import com.example.klimboo.data.FirebaseQueries.Ferramenta
import com.example.klimboo.data.PhotoManager
import com.example.klimboo.databinding.ActivityStockPageBinding
import com.example.klimboo.databinding.BottomSheetAddBinding
import com.example.klimboo.databinding.BottomSheetBinding
import com.example.klimboo.databinding.BottomSheetDeleteBinding
import com.example.klimboo.databinding.BottomSheetEditBinding
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.core.view.isGone

class StockPage : AppCompatActivity() {

    private lateinit var binding: ActivityStockPageBinding

    companion object {
        private const val MSG_INFORME_NOME_ARMARIO = "Informe o nome do armário"
        private const val MSG_INFORME_NOME_ITEM    = "Informe o nome do item"
        private const val MSG_INFORME_NOVO_NOME    = "Informe o novo nome"
        private const val MSG_SELECIONE_ARMARIO    = "Selecione um armário"
        private const val MSG_SELECIONE_ITEM       = "Selecione um item"
        private const val MSG_SELECIONE_ALTERAR    = "Selecione o que deseja alterar"
        private const val MSG_SELECIONE_DESTINO    = "Selecione o armário destino"
    }

    // ── Câmera ────────────────────────────────────────────────────────────────

    private var onPhotoTaken: ((Bitmap) -> Unit)? = null

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> bitmap?.let { onPhotoTaken?.invoke(it) } }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) takePicture.launch(null) else toast("Permissão de câmera necessária") }

    private fun openCamera(onPhoto: (Bitmap) -> Unit) {
        onPhotoTaken = onPhoto
        requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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
            .addOnSuccessListener { doc ->
                val isAdmin = doc.getBoolean("isAdmin") ?: false
                binding.editStock.visibility = if (isAdmin) View.VISIBLE else View.GONE
                binding.editStock.setOnClickListener { showMainSheet() }
            }

        lifecycleScope.launch {
            try {
                val lockers = FirebaseQueries.fetchArmarios()
                binding.spinnerLockers.adapter = spinnerAdapter(lockers.map { it.nome })
                binding.spinnerLockers.onItemSelectedListener =
                    object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, pos: Int, id: Long) {
                            val selected = lockers.getOrNull(pos) ?: return
                            lifecycleScope.launch {
                                val tools = FirebaseQueries.fetchFerramentasByLocker(selected.id)
                                binding.listTools.adapter = ArrayAdapter(
                                    this@StockPage,
                                    android.R.layout.simple_list_item_1,
                                    tools.map { it.nome }
                                )
                            }
                        }
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                    }
            } catch (e: Exception) {
                Log.e("STOCK", "Error loading lockers: ${e.message}", e)
            }
        }
    }

    // ── Sheets ────────────────────────────────────────────────────────────────

    private fun showMainSheet() {
        val b = BottomSheetBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        b.btnAdd.setOnClickListener { dialog.dismiss(); showAddSheet() }
        b.btnEdit.setOnClickListener { dialog.dismiss(); showEditSheet() }
        b.btnDelete.setOnClickListener { dialog.dismiss(); showDeleteSheet() }
        dialog.setContentView(b.root)
        dialog.show()
    }

    private fun showAddSheet() {
        val dialog = BottomSheetDialog(this)
        val b = BottomSheetAddBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)
        dialog.show()

        var lockers: List<Armario> = emptyList()
        var photoBitmapArmario: Bitmap? = null
        var photoBitmapItem: Bitmap? = null

        lifecycleScope.launch {
            try {
                lockers = FirebaseQueries.fetchArmarios()
                runOnUiThread { b.spinnerArmarioDestino.adapter = spinnerAdapter(lockers.map { it.nome }) }
            } catch (e: Exception) { Log.e("DEBUG_SPINNER", "Error: ${e.message}", e) }
        }

        bindToggle(b.toggleGroup, b.layoutAddArmario, b.layoutAddItem)
        bindPhotoButtons(b.btnFotoArmario, b.btnRemoverFotoArmario, b.imgPreviewArmario) { photoBitmapArmario = it }
        bindPhotoButtons(b.btnFotoItem, b.btnRemoverFotoItem, b.imgPreviewItem) { photoBitmapItem = it }

        b.btnConfirmarAdd.setOnClickListener {
            val isLocker = b.toggleGroup.checkedButtonId == R.id.btnToggleArmario
            lifecycleScope.launch {
                if (isLocker) {
                    val name = b.editNomeArmario.text.toString().trim()
                    if (name.isEmpty()) { toast(MSG_INFORME_NOME_ARMARIO); return@launch }
                    val url = photoBitmapArmario?.let { PhotoManager.uploadPhoto(it, "armarios") }
                    FirebaseQueries.insertArmario(name, url)
                    toast("Armário '$name' adicionado!")
                } else {
                    val name = b.editNomeItem.text.toString().trim()
                    if (name.isEmpty()) { toast(MSG_INFORME_NOME_ITEM); return@launch }
                    val locker = lockers.getOrNull(b.spinnerArmarioDestino.selectedItemPosition)
                        ?: run { toast(MSG_SELECIONE_ARMARIO); return@launch }
                    val url = photoBitmapItem?.let { PhotoManager.uploadPhoto(it, "ferramentas") }
                    FirebaseQueries.insertFerramenta(name, locker.id, url)
                    toast("Item '$name' adicionado!")
                }
                dialog.dismiss()
            }
        }
    }

    private fun showEditSheet() {
        val dialog = BottomSheetDialog(this)
        val b = BottomSheetEditBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        var lockers: List<Armario>
        var tools: List<Ferramenta>
        var selectedLocker: Armario? = null
        var selectedTool: Ferramenta? = null
        var selectedDestination: Armario? = null
        var newPhotoBitmapArmario: Bitmap? = null
        var newPhotoBitmapItem: Bitmap? = null

        listOf(
            b.checkAlterarNomeArmario to b.layoutNovoNomeArmario,
            b.checkAlterarNomeItem    to b.layoutNovoNomeItem,
            b.checkAlterarLocalItem   to b.layoutMoverParaArmario
        ).forEach { (check, layout) ->
            check.setOnCheckedChangeListener { _, c -> layout.visibility = if (c) View.VISIBLE else View.GONE }
        }

        bindToggle(b.toggleGroup, b.layoutEditArmario, b.layoutEditItem)
        bindPhotoButtons(b.btnFotoArmario, b.btnRemoverFotoArmario, b.imgPreviewArmario) { newPhotoBitmapArmario = it }
        bindPhotoButtons(b.btnFotoItem, b.btnRemoverFotoItem, b.imgPreviewItem) { newPhotoBitmapItem = it }

        b.btnConfirmarEdit.setOnClickListener {
            val isLocker = b.toggleGroup.checkedButtonId == R.id.btnToggleArmario
            lifecycleScope.launch {
                if (isLocker) {
                    val locker = selectedLocker ?: run { toast(MSG_SELECIONE_ARMARIO); return@launch }
                    val hasNameChange = b.checkAlterarNomeArmario.isChecked
                    val hasNewPhoto = newPhotoBitmapArmario != null
                    val isRemovingPhoto = locker.photoUrl != null && b.imgPreviewArmario.isGone && !hasNewPhoto
                    if (!hasNameChange && !hasNewPhoto && !isRemovingPhoto) { toast(MSG_SELECIONE_ALTERAR); return@launch }
                    if (hasNameChange) {
                        val newName = b.editNovoNomeArmario.text.toString().trim()
                        if (newName.isEmpty()) { toast(MSG_INFORME_NOVO_NOME); return@launch }
                        FirebaseQueries.updateArmario(locker.id, newName)
                    }
                    handlePhotoUpdate(hasNewPhoto, isRemovingPhoto, newPhotoBitmapArmario, locker.photoUrl, "armarios") {
                        FirebaseQueries.updateArmarioPhoto(locker.id, it)
                    }
                    toast("Armário atualizado!")
                } else {
                    val tool = selectedTool ?: run { toast(MSG_SELECIONE_ITEM); return@launch }
                    val hasNameChange = b.checkAlterarNomeItem.isChecked
                    val hasLocalChange = b.checkAlterarLocalItem.isChecked
                    val hasNewPhoto = newPhotoBitmapItem != null
                    val isRemovingPhoto = tool.photoUrl != null && b.imgPreviewItem.isGone && !hasNewPhoto
                    if (!hasNameChange && !hasLocalChange && !hasNewPhoto && !isRemovingPhoto) { toast(MSG_SELECIONE_ALTERAR); return@launch }
                    val newName = if (hasNameChange) {
                        b.editNovoNomeItem.text.toString().trim().also { if (it.isEmpty()) { toast(MSG_INFORME_NOVO_NOME); return@launch } }
                    } else tool.nome
                    val newLockerId = if (hasLocalChange) {
                        selectedDestination?.id ?: run { toast(MSG_SELECIONE_DESTINO); return@launch }
                    } else tool.local
                    FirebaseQueries.updateFerramenta(tool.id, newName, newLockerId)
                    handlePhotoUpdate(hasNewPhoto, isRemovingPhoto, newPhotoBitmapItem, tool.photoUrl, "ferramentas") {
                        FirebaseQueries.updateFerramentaPhoto(tool.id, it)
                    }
                    toast("Item atualizado!")
                }
                dialog.dismiss()
            }
        }

        lifecycleScope.launch {
            lockers = FirebaseQueries.fetchArmarios()
            tools = FirebaseQueries.fetchFerramentas()
            bindAutoComplete(b.autoCompleteArmario, lockers.map { it.nome }) { pos ->
                selectedLocker = lockers.getOrNull(pos)
                newPhotoBitmapArmario = null
                showPhotoPreview(selectedLocker?.photoUrl, b.imgPreviewArmario, b.btnRemoverFotoArmario)
            }
            bindAutoComplete(b.autoCompleteItem, tools.map { it.nome }) { pos ->
                selectedTool = tools.getOrNull(pos)
                newPhotoBitmapItem = null
                showPhotoPreview(selectedTool?.photoUrl, b.imgPreviewItem, b.btnRemoverFotoItem)
            }
            bindAutoComplete(b.autoCompleteArmarioDestino, lockers.map { it.nome }) { pos ->
                selectedDestination = lockers.getOrNull(pos)
            }
            dialog.show()
        }
    }

    private fun showDeleteSheet() {
        val dialog = BottomSheetDialog(this)
        val b = BottomSheetDeleteBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        var lockers: List<Armario>
        var tools: List<Ferramenta>
        var selectedLocker: Armario? = null
        var selectedDestination: Armario? = null
        var selectedTool: Ferramenta? = null

        bindToggle(b.toggleGroup, b.layoutDeleteArmario, b.layoutDeleteItem)

        b.btnRemoverFotoArmario.setOnClickListener {
            val locker = selectedLocker ?: return@setOnClickListener
            lifecycleScope.launch {
                locker.photoUrl?.let { PhotoManager.deletePhoto(it) }
                FirebaseQueries.updateArmarioPhoto(locker.id, null)
                selectedLocker = locker.copy(photoUrl = null)
                showPhotoPreview(null, b.imgPreviewArmario, b.btnRemoverFotoArmario)
                toast("Foto removida")
            }
        }
        b.btnRemoverFotoItem.setOnClickListener {
            val tool = selectedTool ?: return@setOnClickListener
            lifecycleScope.launch {
                tool.photoUrl?.let { PhotoManager.deletePhoto(it) }
                FirebaseQueries.updateFerramentaPhoto(tool.id, null)
                selectedTool = tool.copy(photoUrl = null)
                showPhotoPreview(null, b.imgPreviewItem, b.btnRemoverFotoItem)
                toast("Foto removida")
            }
        }

        b.btnConfirmarDelete.setOnClickListener {
            val isLocker = b.toggleGroup.checkedButtonId == R.id.btnToggleArmario
            lifecycleScope.launch {
                if (isLocker) {
                    val locker = selectedLocker ?: run { toast(MSG_SELECIONE_ARMARIO); return@launch }
                    locker.photoUrl?.let { PhotoManager.deletePhoto(it) }
                    FirebaseQueries.deleteArmario(locker.id, selectedDestination?.id)
                    toast(if (selectedDestination != null) "Itens movidos para '${selectedDestination!!.nome}'." else "Armário e itens removidos.")
                } else {
                    val tool = selectedTool ?: run { toast(MSG_SELECIONE_ITEM); return@launch }
                    tool.photoUrl?.let { PhotoManager.deletePhoto(it) }
                    FirebaseQueries.deleteFerramenta(tool.id)
                    toast("Item '${tool.nome}' removido!")
                }
                dialog.dismiss()
            }
        }

        lifecycleScope.launch {
            lockers = FirebaseQueries.fetchArmarios()
            tools = FirebaseQueries.fetchFerramentas()
            bindAutoComplete(b.autoCompleteArmarioRemover, lockers.map { it.nome }) { pos ->
                selectedLocker = lockers.getOrNull(pos)
                showPhotoPreview(selectedLocker?.photoUrl, b.imgPreviewArmario, b.btnRemoverFotoArmario)
                val others = lockers.filter { it.id != selectedLocker?.id }
                bindAutoComplete(b.autoCompleteDestinoItens, others.map { it.nome }) { p ->
                    selectedDestination = others.getOrNull(p)
                }
                b.autoCompleteDestinoItens.text.clear()
                selectedDestination = null
                b.layoutDestinoItens.isEnabled = others.isNotEmpty()
                b.txtAvisoSemDestino.visibility = if (others.isEmpty()) View.VISIBLE else View.GONE
            }
            bindAutoComplete(b.autoCompleteItemRemover, tools.map { it.nome }) { pos ->
                selectedTool = tools.getOrNull(pos)
                showPhotoPreview(selectedTool?.photoUrl, b.imgPreviewItem, b.btnRemoverFotoItem)
            }
            bindAutoComplete(b.autoCompleteDestinoItens, lockers.map { it.nome }) { pos ->
                selectedDestination = lockers.getOrNull(pos)
            }
            dialog.show()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun bindToggle(group: MaterialButtonToggleGroup, layoutLocker: View, layoutItem: View) {
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val isLocker = checkedId == R.id.btnToggleArmario
            layoutLocker.visibility = if (isLocker) View.VISIBLE else View.GONE
            layoutItem.visibility = if (isLocker) View.GONE else View.VISIBLE
        }
    }

    private fun bindPhotoButtons(
        btnFoto: android.widget.Button,
        btnRemover: android.widget.Button,
        imgPreview: ImageView,
        onBitmapChanged: (Bitmap?) -> Unit
    ) {
        btnFoto.setOnClickListener {
            openCamera { bitmap ->
                onBitmapChanged(bitmap)
                imgPreview.setImageBitmap(bitmap)
                imgPreview.visibility = View.VISIBLE
                btnRemover.visibility = View.VISIBLE
            }
        }
        btnRemover.setOnClickListener {
            onBitmapChanged(null)
            imgPreview.setImageDrawable(null)
            imgPreview.visibility = View.GONE
            btnRemover.visibility = View.GONE
        }
    }

    private fun showPhotoPreview(url: String?, imgPreview: ImageView, btnRemover: android.widget.Button) {
        if (url != null) {
            Glide.with(this).load(url).into(imgPreview)
            imgPreview.visibility = View.VISIBLE
            btnRemover.visibility = View.VISIBLE
        } else {
            imgPreview.setImageDrawable(null)
            imgPreview.visibility = View.GONE
            btnRemover.visibility = View.GONE
        }
    }

    private suspend fun handlePhotoUpdate(
        hasNewPhoto: Boolean,
        isRemoving: Boolean,
        bitmap: Bitmap?,
        oldUrl: String?,
        folder: String,
        onUrlReady: suspend (String?) -> Unit
    ) {
        when {
            hasNewPhoto && bitmap != null -> onUrlReady(PhotoManager.updatePhoto(this, bitmap, oldUrl, folder))
            isRemoving && oldUrl != null  -> { PhotoManager.deletePhoto(oldUrl); onUrlReady(null) }
        }
    }

    private fun bindAutoComplete(view: AutoCompleteTextView, items: List<String>, onSelected: (Int) -> Unit) {
        view.setAdapter(dropdownAdapter(items))
        view.setOnClickListener { view.showDropDown() }
        view.setOnItemClickListener { _, _, pos, _ -> onSelected(pos) }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun dropdownAdapter(items: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)

    private fun spinnerAdapter(items: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
}