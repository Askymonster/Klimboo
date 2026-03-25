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
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
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

class StockPage : AppCompatActivity() {

    private lateinit var binding: ActivityStockPageBinding
    private var currentLockers: List<Armario> = emptyList()
    private var currentLockerPos: Int = 0

    // ── Mensagens de erro ────────────────────────────────────────────────
    companion object {
        private const val MSG_ENTER_LOCKER_NAME   = "Enter the locker name"
        private const val MSG_ENTER_ITEM_NAME     = "Enter the item name"
        private const val MSG_ENTER_NEW_NAME      = "Enter the new name"
        private const val MSG_SELECT_LOCKER       = "Select a locker"
        private const val MSG_SELECT_ITEM         = "Select an item"
        private const val MSG_SELECT_WHAT_TO_EDIT = "Select what you want to change"
        private const val MSG_SELECT_DESTINATION  = "Select the destination locker"
    }

    // ── Câmera ────────────────────────────────────────────────────────────────

    private var onPhotoTaken: ((Bitmap) -> Unit)? = null

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> bitmap?.let { onPhotoTaken?.invoke(it) } }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) takePicture.launch(null) else toast("Camera permission required") }

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

        loadPage()
    }

    // ── Carregamento da página ────────────────────────────────────────────────

    private fun loadPage(restorePos: Int = 0) {
        lifecycleScope.launch {
            try {
                currentLockers = FirebaseQueries.fetchArmarios()
                binding.spinnerLockers.adapter = LockerSpinnerAdapter(this@StockPage, currentLockers)
                binding.spinnerLockers.onItemSelectedListener =
                    object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, pos: Int, id: Long) {
                            currentLockerPos = pos
                            val selected = currentLockers.getOrNull(pos) ?: return
                            lifecycleScope.launch {
                                val tools = FirebaseQueries.fetchFerramentasByLocker(selected.id)
                                binding.listTools.layoutManager =
                                    androidx.recyclerview.widget.LinearLayoutManager(this@StockPage)
                                binding.listTools.adapter = StockAdapter(
                                    this@StockPage,
                                    tools.map { Triple(it.nome, it.photoUrl, null) }
                                )
                            }
                        }
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                    }
                if (restorePos < currentLockers.size) {
                    binding.spinnerLockers.setSelection(restorePos)
                }
            } catch (e: Exception) {
                Log.e("STOCK", "Error loading: ${e.message}", e)
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

    // ── Add BottomSheet ────────────────────────────────────────────────
    private fun showAddSheet() {
        val dialog = BottomSheetDialog(this)
        val b = BottomSheetAddBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)
        dialog.show()

        var lockers: List<Armario> = emptyList()
        var photoBitmapLocker: Bitmap? = null
        var photoBitmapItem: Bitmap? = null

        lifecycleScope.launch {
            try {
                lockers = FirebaseQueries.fetchArmarios()
                runOnUiThread { b.spinnerDestinyLocker.adapter = spinnerAdapter(lockers.map { it.nome }) }
            } catch (e: Exception) { Log.e("DEBUG_SPINNER", "Error: ${e.message}", e) }
        }

        bindToggle(b.toggleGroup, b.layoutAddLocker, b.layoutAddItem)
        b.toggleGroup.check(R.id.btnToggleLocker)
        bindPhotoButtons(b.btnLockerPhoto, b.btnRemoveLockerPhoto, b.imgPreviewLocker) { photoBitmapLocker = it }
        bindPhotoButtons(b.btnItemPhoto, b.btnRemoveItemPhoto, b.imgPreviewItem) { photoBitmapItem = it }

        b.btnConfirmAdd.setOnClickListener {
            val isLocker = b.toggleGroup.checkedButtonId == R.id.btnToggleLocker
            lifecycleScope.launch {
                if (isLocker) {
                    val name = b.editLockerName.text.toString().trim()
                    if (name.isEmpty()) { toast(MSG_ENTER_LOCKER_NAME); return@launch }
                    val url = photoBitmapLocker?.let { PhotoManager.bitmapToBase64(it) }
                    FirebaseQueries.insertArmario(name, url)
                    toast("Locker '$name' added!")
                } else {
                    val name = b.editNomeItem.text.toString().trim()
                    if (name.isEmpty()) { toast(MSG_ENTER_ITEM_NAME); return@launch }
                    val locker = lockers.getOrNull(b.spinnerDestinyLocker.selectedItemPosition)
                        ?: run { toast(MSG_SELECT_LOCKER); return@launch }
                    val url = photoBitmapItem?.let { PhotoManager.bitmapToBase64(it) }
                    FirebaseQueries.insertFerramenta(name, locker.id, url)
                    toast("Item '$name' added!")
                }
                dialog.dismiss()
                loadPage(currentLockerPos)
            }
        }
    }


    // ── Edit BottomSheet ────────────────────────────────────────────────
    private fun showEditSheet() {
        val dialog = BottomSheetDialog(this)
        val b = BottomSheetEditBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        var lockers: List<Armario>
        var tools: List<Ferramenta>
        var selectedLocker: Armario? = null
        var selectedTool: Ferramenta? = null
        var selectedDestination: Armario? = null
        var newPhotoBitmapLocker: Bitmap? = null
        var newPhotoBitmapItem: Bitmap? = null

        listOf(
            b.checkChangeLockerName to b.layoutNewLockerName,
            b.checkChangeItemName   to b.layoutNewItemName,
            b.checkChangeItemLoc    to b.layoutMoveToLocal
        ).forEach { (check, layout) ->
            check.setOnCheckedChangeListener { _, c -> layout.visibility = if (c) View.VISIBLE else View.GONE }
        }

        bindToggle(b.toggleGroup, b.layoutEditLocker, b.layoutEditItem)
        bindPhotoButtons(b.btnLockerPhoto, b.btnRemoverLockerPhoto, b.imgPreviewLocker) { newPhotoBitmapLocker = it }
        bindPhotoButtons(b.btnItemPhoto, b.btnRemoveItemPhoto, b.imgPreviewItem) { newPhotoBitmapItem = it }

        b.btnConfirmEdit.setOnClickListener {
            val isLocker = b.toggleGroup.checkedButtonId == R.id.btnToggleLocker
            lifecycleScope.launch {
                if (isLocker) {
                    val locker = selectedLocker ?: run { toast(MSG_SELECT_LOCKER); return@launch }
                    val hasNameChange = b.checkChangeItemLoc.isChecked
                    val hasNewPhoto = newPhotoBitmapLocker != null
                    val isRemovingPhoto = locker.photoUrl != null && b.imgPreviewLocker.isGone && !hasNewPhoto
                    if (!hasNameChange && !hasNewPhoto && !isRemovingPhoto) { toast(MSG_SELECT_WHAT_TO_EDIT); return@launch }
                    if (hasNameChange) {
                        val newName = b.editNewLockerName.text.toString().trim()
                        if (newName.isEmpty()) { toast(MSG_ENTER_NEW_NAME); return@launch }
                        FirebaseQueries.updateArmario(locker.id, newName)
                    }
                    handlePhotoUpdate(hasNewPhoto, isRemovingPhoto, newPhotoBitmapLocker) {
                        FirebaseQueries.updateArmarioPhoto(locker.id, it)
                    }
                    toast("Locker updated!")
                } else {
                    val tool = selectedTool ?: run { toast(MSG_SELECT_ITEM); return@launch }
                    val hasNameChange = b.checkChangeItemName.isChecked
                    val hasLocalChange = b.checkChangeItemLoc.isChecked
                    val hasNewPhoto = newPhotoBitmapItem != null
                    val isRemovingPhoto = tool.photoUrl != null && b.imgPreviewItem.isGone && !hasNewPhoto
                    if (!hasNameChange && !hasLocalChange && !hasNewPhoto && !isRemovingPhoto) { toast(MSG_SELECT_WHAT_TO_EDIT); return@launch }
                    val newName = if (hasNameChange) {
                        b.editNewItemName.text.toString().trim().also { if (it.isEmpty()) { toast(MSG_ENTER_NEW_NAME); return@launch } }
                    } else tool.nome
                    val newLockerId = if (hasLocalChange) {
                        selectedDestination?.id ?: run { toast(MSG_SELECT_DESTINATION); return@launch }
                    } else tool.local
                    FirebaseQueries.updateFerramenta(tool.id, newName, newLockerId)
                    handlePhotoUpdate(hasNewPhoto, isRemovingPhoto, newPhotoBitmapItem) {
                        FirebaseQueries.updateFerramentaPhoto(tool.id, it)
                    }
                    toast("Item updated!")
                }
                dialog.dismiss()
                loadPage(currentLockerPos)
            }
        }

        lifecycleScope.launch {
            lockers = FirebaseQueries.fetchArmarios()
            tools = FirebaseQueries.fetchFerramentas()
            bindAutoComplete(b.autoCompleteLocker, lockers.map { it.nome }) { pos ->
                selectedLocker = lockers.getOrNull(pos)
                newPhotoBitmapLocker = null
                showPhotoPreview(selectedLocker?.photoUrl, b.imgPreviewLocker, b.btnRemoverLockerPhoto)
            }
            bindAutoComplete(b.autoCompleteItem, tools.map { it.nome }) { pos ->
                selectedTool = tools.getOrNull(pos)
                newPhotoBitmapItem = null
                showPhotoPreview(selectedTool?.photoUrl, b.imgPreviewItem, b.btnRemoveItemPhoto)
            }
            bindAutoComplete(b.autoCompleteDestityLocker, lockers.map { it.nome }) { pos ->
                selectedDestination = lockers.getOrNull(pos)
            }
            dialog.show()
        }
    }


    // ── Delete BottomSheet ────────────────────────────────────────────────
    private fun showDeleteSheet() {
        val dialog = BottomSheetDialog(this)
        val b = BottomSheetDeleteBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        var lockers: List<Armario>
        var tools: List<Ferramenta>
        var selectedLocker: Armario? = null
        var selectedDestination: Armario? = null
        var selectedTool: Ferramenta? = null

        bindToggle(b.toggleGroup, b.layoutDeleteLocker, b.layoutDeleteItem)

        b.btnRemoveLockerPhoto.setOnClickListener {
            val locker = selectedLocker ?: return@setOnClickListener
            lifecycleScope.launch {
                FirebaseQueries.updateArmarioPhoto(locker.id, null)
                selectedLocker = locker.copy(photoUrl = null)
                showPhotoPreview(null, b.imgPreviewLocker, b.btnRemoveLockerPhoto)
                toast("Photo removed")
            }
        }
        b.btnRemoverFotoItem.setOnClickListener {
            val tool = selectedTool ?: return@setOnClickListener
            lifecycleScope.launch {
                FirebaseQueries.updateFerramentaPhoto(tool.id, null)
                selectedTool = tool.copy(photoUrl = null)
                showPhotoPreview(null, b.imgPreviewItem, b.btnRemoverFotoItem)
                toast("Photo removed")
            }
        }

        b.btnConfirmarDelete.setOnClickListener {
            val isLocker = b.toggleGroup.checkedButtonId == R.id.btnToggleLocker
            lifecycleScope.launch {
                if (isLocker) {
                    val locker = selectedLocker ?: run { toast(MSG_SELECT_LOCKER); return@launch }
                    FirebaseQueries.deleteArmario(locker.id, selectedDestination?.id)
                    toast(if (selectedDestination != null) "Items moved to '${selectedDestination!!.nome}'." else "Locker and items removed.")
                } else {
                    val tool = selectedTool ?: run { toast(MSG_SELECT_ITEM); return@launch }
                    FirebaseQueries.deleteFerramenta(tool.id)
                    toast("Item '${tool.nome}' removed!")
                }
                dialog.dismiss()
                loadPage(currentLockerPos)
            }
        }

        lifecycleScope.launch {
            lockers = FirebaseQueries.fetchArmarios()
            tools = FirebaseQueries.fetchFerramentas()
            bindAutoComplete(b.autoCompleteArmarioRemover, lockers.map { it.nome }) { pos ->
                selectedLocker = lockers.getOrNull(pos)
                showPhotoPreview(selectedLocker?.photoUrl, b.imgPreviewLocker, b.btnRemoveLockerPhoto)
                val others = lockers.filter { it.id != selectedLocker?.id }
                bindAutoComplete(b.autoCompleteItemsDestiny, others.map { it.nome }) { p ->
                    selectedDestination = others.getOrNull(p)
                }
                b.autoCompleteItemsDestiny.text.clear()
                selectedDestination = null
                b.layoutItemsDestiny.isEnabled = others.isNotEmpty()
                b.txtAvisoSemDestino.visibility = if (others.isEmpty()) View.VISIBLE else View.GONE
            }
            bindAutoComplete(b.autoCompleteItemRemover, tools.map { it.nome }) { pos ->
                selectedTool = tools.getOrNull(pos)
                showPhotoPreview(selectedTool?.photoUrl, b.imgPreviewItem, b.btnRemoverFotoItem)
            }
            bindAutoComplete(b.autoCompleteItemsDestiny, lockers.map { it.nome }) { pos ->
                selectedDestination = lockers.getOrNull(pos)
            }
            dialog.show()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun bindToggle(group: MaterialButtonToggleGroup, layoutLocker: View, layoutItem: View) {
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val isLocker = checkedId == R.id.btnToggleLocker
            layoutLocker.visibility = if (isLocker) View.VISIBLE else View.GONE
            layoutItem.visibility = if (isLocker) View.GONE else View.VISIBLE
        }
    }

    private fun bindPhotoButtons(
        btnPhoto: android.widget.Button,
        btnRemove: android.widget.Button,
        imgPreview: ImageView,
        onBitmapChanged: (Bitmap?) -> Unit
    ) {
        btnPhoto.setOnClickListener {
            openCamera { bitmap ->
                onBitmapChanged(bitmap)
                imgPreview.setImageBitmap(bitmap)
                imgPreview.visibility = View.VISIBLE
                btnRemove.visibility = View.VISIBLE
            }
        }
        btnRemove.setOnClickListener {
            onBitmapChanged(null)
            imgPreview.setImageDrawable(null)
            imgPreview.visibility = View.GONE
            btnRemove.visibility = View.GONE
        }
    }

    private fun showPhotoPreview(url: String?, imgPreview: ImageView, btnRemove: android.widget.Button) {
        if (url != null) {
            val bitmap = PhotoManager.base64ToBitmap(url)
            imgPreview.setImageBitmap(bitmap)
            imgPreview.visibility = View.VISIBLE
            btnRemove.visibility = View.VISIBLE
        } else {
            imgPreview.setImageDrawable(null)
            imgPreview.visibility = View.GONE
            btnRemove.visibility = View.GONE
        }
    }

    private fun handlePhotoUpdate(
        hasNewPhoto: Boolean,
        isRemoving: Boolean,
        bitmap: Bitmap?,
        onUrlReady: suspend (String?) -> Unit
    ) {
        lifecycleScope.launch {
            when {
                hasNewPhoto && bitmap != null -> onUrlReady(PhotoManager.bitmapToBase64(bitmap))
                isRemoving                    -> onUrlReady(null)
            }
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