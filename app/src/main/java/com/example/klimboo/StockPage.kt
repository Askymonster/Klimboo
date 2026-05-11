package com.example.klimboo

import android.Manifest
import android.content.Intent
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
import com.example.klimboo.data.FirebaseQueries.Locker
import com.example.klimboo.data.FirebaseQueries.Tool
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
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class StockPage : AppCompatActivity() {

    private lateinit var binding: ActivityStockPageBinding
    private var currentLockers: List<Locker> = emptyList()
    private var allTools: List<Tool> = emptyList()
    private var currentLockerPos: Int = 0

    // Listeners para cleanup
    private var lockerListener: ListenerRegistration? = null
    private var toolListener: ListenerRegistration? = null

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
    ) { granted ->
        if (granted) takePicture.launch(null)
        else toast("Camera permission required")
    }

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

        checkAdminStatus()
        loadPage()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup listeners para evitar vazamento de memória
        lockerListener?.remove()
        toolListener?.remove()
    }

    // ── Admin Check ───────────────────────────────────────────────────────────

    private fun checkAdminStatus() {
        binding.editStock.visibility = View.GONE

        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginPage::class.java))
            finish()
            return
        }
        FirebaseFirestore.getInstance().collection("usuarios")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                val isAdmin = doc.getBoolean("isAdmin") ?: false
                binding.editStock.visibility = if (isAdmin) View.VISIBLE else View.GONE
                binding.editStock.setOnClickListener { showMainSheet() }
            }
            .addOnFailureListener { e ->
                binding.editStock.visibility = View.GONE
                Log.e("STOCK", "Failed to check admin status", e)
            }
    }

    // ── Carregamento da página ────────────────────────────────────────────────

    private fun loadPage(restorePos: Int = 0) {
        lifecycleScope.launch {
            try {
                currentLockers = FirebaseQueries.fetchLockers()
                allTools = FirebaseQueries.fetchTools()

                setupLockerSpinner(currentLockers)

                if (restorePos < currentLockers.size) {
                    binding.spinnerLockers.setSelection(restorePos)
                }
            } catch (e: Exception) {
                Log.e("STOCK", "Error loading page", e)
                toast("Failed to load data. Try again later.")
            }
        }
    }

    private fun setupLockerSpinner(lockers: List<Locker>) {
        binding.spinnerLockers.adapter = LockerSpinnerAdapter(this, lockers)
        binding.spinnerLockers.onItemSelectedListener = LockerSpinnerListener()
    }

    private inner class LockerSpinnerListener :
        android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: android.widget.AdapterView<*>,
            view: View?,
            pos: Int,
            id: Long
        ) {
            currentLockerPos = pos
            val selected = currentLockers.getOrNull(pos) ?: return
            // Filtra tools em memória em vez de buscar do BD novamente
            updateToolsList(allTools.filter { it.local == selected.id })
        }

        override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
    }

    private fun updateToolsList(tools: List<Tool>) {
        binding.listTools.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this@StockPage)

        // Cria um mapa id -> local do armário
        val lockerMap = currentLockers.associate { it.id to it.local }

        binding.listTools.adapter = StockAdapter(
            this@StockPage,
            tools.map { tool ->
                Locker(
                    id = tool.id,
                    name = tool.name,
                    local = lockerMap[tool.local] ?: "Local desconhecido",  // Busca o nome do local
                    photoUrl = tool.photoUrl
                )
            }
        )
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

    // ── Add BottomSheet ───────────────────────────────────────────────────────

    private fun showAddSheet() {
        val dialog = BottomSheetDialog(this)
        val b = BottomSheetAddBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)
        dialog.show()

        var photoBitmapLocker: Bitmap? = null
        var photoBitmapItem: Bitmap? = null

        lifecycleScope.launch {
            try {
                val lockers = FirebaseQueries.fetchLockers()
                b.spinnerDestinyLocker.adapter = spinnerAdapter(lockers.map { it.name })
            } catch (e: Exception) {
                Log.e("STOCK", "Error loading lockers for add sheet", e)
            }
        }

        bindToggle(b.toggleGroup, b.layoutAddLocker, b.layoutAddItem)
        b.toggleGroup.check(R.id.btnToggleLocker)
        bindPhotoButtons(b.btnLockerPhoto, b.btnRemoveLockerPhoto, b.imgPreviewLocker) { photoBitmapLocker = it }
        bindPhotoButtons(b.btnItemPhoto, b.btnRemoveItemPhoto, b.imgPreviewItem) { photoBitmapItem = it }

        b.btnConfirmAdd.setOnClickListener {
            val isLocker = b.toggleGroup.checkedButtonId == R.id.btnToggleLocker
            lifecycleScope.launch {
                if (isLocker) {
                    addNewLocker(b, photoBitmapLocker)
                } else {
                    addNewTool(b, photoBitmapItem)
                }
                dialog.dismiss()
                loadPage(currentLockerPos)
            }
        }
    }

    // ── Adiciona novo armário ──────────────────────────────────────────────────────────────
    private suspend fun addNewLocker(b: BottomSheetAddBinding, photoBitmap: Bitmap?) {
        val name = b.editLockerName.text.toString().trim()
        val local = b.editLockerLocal.text.toString().trim()

        if (name.isEmpty()) {
            toast(MSG_ENTER_LOCKER_NAME)
            return
        }

        val url = getPhotoUrl(photoBitmap)
        FirebaseQueries.insertLocker(name, url, local)
        toast("Locker '$name' added!")
    }

    // ── Adiciona nova ferramenta ──────────────────────────────────────────────────────────────
    private suspend fun addNewTool(b: BottomSheetAddBinding, photoBitmap: Bitmap?) {
        val name = b.editNomeItem.text.toString().trim()
        if (name.isEmpty()) {
            toast(MSG_ENTER_ITEM_NAME)
            return
        }

        val locker = currentLockers.getOrNull(b.spinnerDestinyLocker.selectedItemPosition)
            ?: run { toast(MSG_SELECT_LOCKER); return }

        val url = getPhotoUrl(photoBitmap)
        FirebaseQueries.insertTool(name, locker.id, url)
        toast("Item '$name' added!")
    }

    // ── Edit BottomSheet ──────────────────────────────────────────────────────

    private fun showEditSheet() {
        val dialog = BottomSheetDialog(this)
        val b = BottomSheetEditBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        var lockers: List<Locker>
        var tools: List<Tool>
        var selectedLocker: Locker? = null
        var selectedTool: Tool? = null
        var selectedDestination: Locker? = null
        var newPhotoBitmapLocker: Bitmap? = null
        var newPhotoBitmapItem: Bitmap? = null

        listOf(
            b.checkChangeLockerName to b.layoutNewLockerName,
            b.checkChangeLockerLocal to b.layoutNewLockerLocal,
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
                    editLocker(b, selectedLocker, newPhotoBitmapLocker, b.imgPreviewLocker)
                } else {
                    editTool(b, selectedTool, selectedDestination, newPhotoBitmapItem, b.imgPreviewItem)
                }
                dialog.dismiss()
                loadPage(currentLockerPos)
            }
        }

        lifecycleScope.launch {
            lockers = FirebaseQueries.fetchLockers()
            tools = FirebaseQueries.fetchTools()

            // AutoComplete para Locker
            bindAutoCompleteGeneric(
                b.autoCompleteLocker,
                lockers,
                { it.name },
                { locker ->
                    selectedLocker = locker
                    newPhotoBitmapLocker = null
                    showPhotoPreview(locker.photoUrl, b.imgPreviewLocker, b.btnRemoverLockerPhoto)
                }
            )

            // AutoComplete para Tool
            bindAutoCompleteGeneric(
                b.autoCompleteItem,
                tools,
                { it.name },
                { tool ->
                    selectedTool = tool
                    newPhotoBitmapItem = null
                    showPhotoPreview(tool.photoUrl, b.imgPreviewItem, b.btnRemoveItemPhoto)
                }
            )

            // AutoComplete para Destino
            bindAutoCompleteGeneric(
                b.autoCompleteDestinyLocker,
                lockers,
                { it.name },
                { locker -> selectedDestination = locker }
            )

            dialog.show()
        }
    }

    private suspend fun editLocker(
        b: BottomSheetEditBinding,
        selectedLocker: Locker?,
        newPhotoBitmap: Bitmap?,
        imgPreview: ImageView
    ) {
        val locker = requireSelectedOrNull(selectedLocker, MSG_SELECT_LOCKER) ?: return

        val hasNameChange = b.checkChangeLockerName.isChecked
        val hasLocalChange = b.checkChangeLockerLocal.isChecked
        val hasNewPhoto = newPhotoBitmap != null
        val isRemovingPhoto = locker.photoUrl != null && imgPreview.isGone && !hasNewPhoto

        // ── Seletor para o que editar em editlocker ──────────────────────────────────────────────────────────────
        if (!hasNameChange && !hasLocalChange && !hasNewPhoto && !isRemovingPhoto) {
            toast(MSG_SELECT_WHAT_TO_EDIT)
            return
        }

        if (hasNameChange || hasLocalChange) {
            val newName = if (hasNameChange) {
                b.editNewLockerName.text.toString().trim().also {
                    if (it.isEmpty()) {
                        toast(MSG_ENTER_NEW_NAME)
                        return
                    }
                }
            } else locker.name
            val newLocal = if (hasLocalChange) {
                b.editNewLockerLocal.text.toString().trim()
            } else locker.local

            FirebaseQueries.updateLocker(locker.id, newName, newLocal)
        }

        handlePhotoUpdate(hasNewPhoto, isRemovingPhoto, newPhotoBitmap) {
            FirebaseQueries.updateLockerPhoto(locker.id, it)
        }

        toast("Locker updated!")
    }

    private suspend fun editTool(
        b: BottomSheetEditBinding,
        selectedTool: Tool?,
        selectedDestination: Locker?,
        newPhotoBitmap: Bitmap?,
        imgPreview: ImageView
    ) {
        val tool = requireSelectedOrNull(selectedTool, MSG_SELECT_ITEM) ?: return

        val hasNameChange = b.checkChangeItemName.isChecked
        val hasLocalChange = b.checkChangeItemLoc.isChecked
        val hasNewPhoto = newPhotoBitmap != null
        val isRemovingPhoto = tool.photoUrl != null && imgPreview.isGone && !hasNewPhoto

        // ── Seletor para o que editar em editTool ──────────────────────────────────────────────────────────────
        if (!hasNameChange && !hasLocalChange && !hasNewPhoto && !isRemovingPhoto) {
            toast(MSG_SELECT_WHAT_TO_EDIT)
            return
        }

        val newName = if (hasNameChange) {
            b.editNewItemName.text.toString().trim().also {
                if (it.isEmpty()) {
                    toast(MSG_ENTER_NEW_NAME)
                    return
                }
            }
        } else tool.name

        val newLockerId = if (hasLocalChange) {
            requireSelectedOrNull(selectedDestination, MSG_SELECT_DESTINATION)?.id ?: return
        } else tool.local

        FirebaseQueries.updateTool(tool.id, newName, newLockerId)

        handlePhotoUpdate(hasNewPhoto, isRemovingPhoto, newPhotoBitmap) {
            FirebaseQueries.updateToolPhoto(tool.id, it)
        }

        toast("Item updated!")
    }

    // ── Delete BottomSheet ────────────────────────────────────────────────────

    private fun showDeleteSheet() {
        val dialog = BottomSheetDialog(this)
        val b = BottomSheetDeleteBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        var lockers: List<Locker>
        var tools: List<Tool>
        var selectedLocker: Locker? = null
        var selectedDestination: Locker? = null
        var selectedTool: Tool? = null

        bindToggle(b.toggleGroup, b.layoutDeleteLocker, b.layoutDeleteItem)

        b.btnRemoveLockerPhoto.setOnClickListener {
            val locker = selectedLocker ?: return@setOnClickListener
            lifecycleScope.launch {
                FirebaseQueries.updateLockerPhoto(locker.id, null)
                selectedLocker = locker.copy(photoUrl = null)
                showPhotoPreview(null, b.imgPreviewLocker, b.btnRemoveLockerPhoto)
                toast("Photo removed")
            }
        }

        // ── Exige que ferramentas sejam movidas para outro armário antes de deletar ──────────────────────────────────────────────────────────────
        b.btnConfirmDelete.setOnClickListener {
            val isLocker = b.toggleGroup.checkedButtonId == R.id.btnToggleLocker
            lifecycleScope.launch {
                if (isLocker) {
                    deleteLocker(selectedLocker, selectedDestination)
                } else {
                    deleteTool(selectedTool)
                }
                dialog.dismiss()
                loadPage(currentLockerPos)
            }
        }

        lifecycleScope.launch {
            lockers = FirebaseQueries.fetchLockers()
            tools = FirebaseQueries.fetchTools()

            bindAutoCompleteGeneric(
                b.autoCompleteLockerRemover,
                lockers,
                { it.name },
                { locker ->
                    selectedLocker = locker
                    showPhotoPreview(locker.photoUrl, b.imgPreviewLocker, b.btnRemoveLockerPhoto)

                    val others = lockers.filter { it.id != locker.id }
                    bindAutoCompleteGeneric(
                        b.autoCompleteItemsDestiny,
                        others,
                        { it.name },
                        { selectedDestination = it }
                    )

                    b.autoCompleteItemsDestiny.text.clear()
                    selectedDestination = null
                    b.layoutItemsDestiny.isEnabled = others.isNotEmpty()
                    b.txtAvisoSemDestino.visibility = if (others.isEmpty()) View.VISIBLE else View.GONE
                }
            )

            bindAutoCompleteGeneric(
                b.autoCompleteItemRemover,
                tools,
                { it.name },
                { tool ->
                    selectedTool = tool
                    showPhotoPreview(tool.photoUrl, b.imgPreviewItem)
                }
            )

            dialog.show()
        }
    }

    private suspend fun deleteLocker(selectedLocker: Locker?, selectedDestination: Locker?) {
        val locker = requireSelectedOrNull(selectedLocker, MSG_SELECT_LOCKER) ?: return
        FirebaseQueries.deleteLocker(locker.id, selectedDestination?.id)
        val message = if (selectedDestination != null)
            "Items moved to '${selectedDestination.name}'."
        else
            "Locker and items removed."
        toast(message)
    }

    private suspend fun deleteTool(selectedTool: Tool?) {
        val tool = requireSelectedOrNull(selectedTool, MSG_SELECT_ITEM) ?: return
        FirebaseQueries.deleteTool(tool.id)
        toast("Item '${tool.name}' removed!")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun bindToggle(
        group: MaterialButtonToggleGroup,
        layoutLocker: View,
        layoutItem: View
    ) {
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

    private fun showPhotoPreview(
        url: String?,
        imgPreview: ImageView,
        btnRemove: android.widget.Button? = null
    ) {
        imgPreview.setImageDrawable(null)
        if (url != null) {
            val bitmap = PhotoManager.base64ToBitmap(url)
            imgPreview.setImageBitmap(bitmap)
            imgPreview.visibility = View.VISIBLE
            btnRemove?.visibility = View.VISIBLE
        } else {
            imgPreview.visibility = View.GONE
            btnRemove?.visibility = View.GONE
        }
    }

    private suspend fun handlePhotoUpdate(
        hasNewPhoto: Boolean,
        isRemoving: Boolean,
        bitmap: Bitmap?,
        onUrlReady: suspend (String?) -> Unit
    ) {
        when {
            hasNewPhoto && bitmap != null -> onUrlReady(getPhotoUrl(bitmap))
            isRemoving                    -> onUrlReady(null)
        }
    }

    private fun getPhotoUrl(bitmap: Bitmap?): String? =
        bitmap?.let { PhotoManager.bitmapToBase64(it) }

    private fun <T> bindAutoCompleteGeneric(
        view: AutoCompleteTextView,
        items: List<T>,
        toLabel: (T) -> String,
        onSelected: (T) -> Unit
    ) {
        val labels = items.map(toLabel)
        view.setAdapter(dropdownAdapter(labels))
        view.setOnClickListener { view.showDropDown() }
        view.setOnItemClickListener { _, _, pos, _ ->
            items.getOrNull(pos)?.let { onSelected(it) }
        }
    }

    private fun <T> requireSelectedOrNull(value: T?, errorMsg: String): T? {
        return value ?: run { toast(errorMsg); null }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun dropdownAdapter(items: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)

    private fun spinnerAdapter(items: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
}
