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
        private const val MSG_CAMERA_PERMISSION   = "Permissão da câmera necessária"
        private const val MSG_LOAD_ERROR          = "Falha ao carregar dados. Tente novamente."
        private const val MSG_ENTER_LOCKER_NAME   = "Insira o nome do armário"
        private const val MSG_ENTER_ITEM_NAME     = "Insira o nome do item"
        private const val MSG_ENTER_NEW_NAME      = "Insira o novo nome"
        private const val MSG_SELECT_LOCKER       = "Selecione um armário"
        private const val MSG_SELECT_ITEM         = "Selecione um item"
        private const val MSG_SELECT_WHAT_TO_EDIT = "Selecione o que deseja alterar"
        private const val MSG_SELECT_DESTINATION  = "Selecione o armário de destino"
        private const val MSG_LOCKER_ADDED        = "Armário '%s' adicionado!"
        private const val MSG_ITEM_ADDED          = "Item '%s' adicionado!"
        private const val MSG_LOCKER_UPDATED      = "Armário atualizado!"
        private const val MSG_ITEM_UPDATED        = "Item atualizado!"
        private const val MSG_LOCKER_REMOVED      = "Armário removido."
        private const val MSG_ITEMS_MOVED         = "Itens movidos para '%s'."
        private const val MSG_ITEM_REMOVED        = "Item '%s' removido!"
        private const val MSG_SAVE_ERROR          = "Falha ao salvar alteração. Tente novamente."
        private const val MSG_DELETE_ERROR        = "Falha ao remover. Tente novamente."
    }

    private data class AutoCompleteOption<T>(
        val label: String,
        val value: T
    ) {
        override fun toString(): String = label
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
        else toast(MSG_CAMERA_PERMISSION)
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

        binding.backtomainButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        checkAdminStatus()
        loadPage()

    }



    override fun onDestroy() {
        super.onDestroy()
        // Cleanup listeners para evitar vazamento de memória
        lockerListener?.remove()
        toolListener?.remove()
        onPhotoTaken = null
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
                renderPage(restorePos)
            } catch (e: Exception) {
                Log.e("STOCK", "Error loading page", e)
                toast(MSG_LOAD_ERROR)
            }
        }
    }



    private fun renderPage(restorePos: Int = currentLockerPos) {
        setupLockerSearch(currentLockers)

        currentLockers.getOrNull(restorePos)?.let { locker ->
            binding.autoCompleteLockers.setText(locker.name, false)
            currentLockerPos = restorePos
            updateToolsList(allTools.filter { it.local == locker.id })
        } ?: run {
            binding.autoCompleteLockers.text.clear()
            updateToolsList(emptyList())
        }
    }

    private fun setupLockerSearch(lockers: List<Locker>) {
        binding.autoCompleteLockers.setAdapter(LockerSearchAdapter(this, lockers))
        binding.autoCompleteLockers.setOnClickListener { binding.autoCompleteLockers.showDropDown() }
        binding.autoCompleteLockers.setOnItemClickListener { parent, _, pos, _ ->
            val locker = parent.getItemAtPosition(pos) as? Locker ?: return@setOnItemClickListener
            currentLockerPos = currentLockers.indexOfFirst { it.id == locker.id }.coerceAtLeast(0)
            binding.autoCompleteLockers.setText(locker.name, false)
            updateToolsList(allTools.filter { it.local == locker.id })
        }
        binding.autoCompleteLockers.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.autoCompleteLockers.showDropDown()
        }


    }

    private fun updateToolsList(tools: List<Tool>) {
        binding.listTools.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this@StockPage)

        // Cria um mapa id -> local do armário
        val lockerMap = currentLockers.associateBy { it.id }

        binding.listTools.adapter = StockAdapter(
            this@StockPage,
            tools.map { tool ->
                val locker = lockerMap[tool.local]
                StockAdapter.StockItem(
                    id = tool.id,
                    name = tool.name,
                    lockerName = locker?.name ?: "Armário desconhecido",
                    lockerLocal = locker?.local ?: "Local desconhecido",
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
        var addSheetLockers: List<Locker>
        var selectedAddDestination: Locker? = null
        var isLockerMode = true

        lifecycleScope.launch {
            try {
                addSheetLockers = FirebaseQueries.fetchLockers()
                bindAutoCompleteGeneric(
                    b.autoCompleteDestinyLockerAdd,
                    addSheetLockers,
                    { "${it.name} - ${it.local}" },
                    { locker ->
                        selectedAddDestination = locker
                        b.autoCompleteDestinyLockerAdd.setText(locker.name, false)
                    }
                )
            } catch (e: Exception) {
                Log.e("STOCK", "Error loading lockers for add sheet", e)
            }
        }

        bindToggle(b.toggleGroup, b.layoutAddLocker, b.layoutAddItem) { isLockerMode = it }
        b.toggleGroup.check(R.id.btnToggleLocker)
        bindPhotoButtons(b.btnLockerPhoto, b.btnRemoveLockerPhoto, b.imgPreviewLocker) { photoBitmapLocker = it }
        bindPhotoButtons(b.btnItemPhoto, b.btnRemoveItemPhoto, b.imgPreviewItem) { photoBitmapItem = it }

        b.btnConfirmAdd.setOnClickListener {
            lifecycleScope.launch {
                val saved = try {
                    if (isLockerMode) {
                        addNewLocker(b, photoBitmapLocker)
                    } else {
                        addNewTool(b, selectedAddDestination, photoBitmapItem)
                    }
                } catch (e: Exception) {
                    Log.e("STOCK", "Error adding stock item", e)
                    toast(MSG_SAVE_ERROR)
                    false
                }
                if (saved) {
                    dialog.dismiss()
                    loadPage(currentLockerPos)
                }
            }
        }
    }

    // ── Adiciona novo armário ──────────────────────────────────────────────────────────────
    private suspend fun addNewLocker(b: BottomSheetAddBinding, photoBitmap: Bitmap?): Boolean {
        val name = b.editLockerName.text.toString().trim()
        val local = b.editLockerLocal.text.toString().trim()

        if (name.isEmpty()) {
            toast(MSG_ENTER_LOCKER_NAME)
            return false
        }

        val url = getPhotoUrl(photoBitmap)
        FirebaseQueries.insertLocker(name, url, local)
        toast(MSG_LOCKER_ADDED.format(name))
        return true
    }

    // ── Adiciona nova ferramenta ──────────────────────────────────────────────────────────────
    private suspend fun addNewTool(
        b: BottomSheetAddBinding,
        selectedDestination: Locker?,
        photoBitmap: Bitmap?
    ): Boolean {
        val name = b.editNomeItem.text.toString().trim()
        if (name.isEmpty()) {
            toast(MSG_ENTER_ITEM_NAME)
            return false
        }

        val locker = selectedDestination
            ?: run {
                toast(MSG_SELECT_LOCKER)
                return false
            }

        val url = getPhotoUrl(photoBitmap)
        FirebaseQueries.insertTool(name, locker.id, url)
        toast(MSG_ITEM_ADDED.format(name))
        return true
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
        var isLockerMode = true

        listOf(
            b.checkChangeLockerName to b.layoutNewLockerName,
            b.checkChangeLockerLocal to b.layoutNewLockerLocal,
            b.checkChangeItemName   to b.layoutNewItemName,
            b.checkChangeItemLoc    to b.layoutMoveToLocal
        ).forEach { (check, layout) ->
            check.setOnCheckedChangeListener { _, c -> layout.visibility = if (c) View.VISIBLE else View.GONE }
        }

        bindToggle(b.toggleGroup, b.layoutEditLocker, b.layoutEditItem) { isLockerMode = it }
        bindPhotoButtons(b.btnLockerPhoto, b.btnRemoverLockerPhoto, b.imgPreviewLocker) { newPhotoBitmapLocker = it }
        bindPhotoButtons(b.btnItemPhoto, b.btnRemoveItemPhoto, b.imgPreviewItem) { newPhotoBitmapItem = it }

        b.btnConfirmEdit.setOnClickListener {
            lifecycleScope.launch {
                val saved = try {
                    if (isLockerMode) {
                        editLocker(b, selectedLocker, newPhotoBitmapLocker, b.imgPreviewLocker)
                    } else {
                        editTool(b, selectedTool, selectedDestination, newPhotoBitmapItem, b.imgPreviewItem)
                    }
                } catch (e: Exception) {
                    Log.e("STOCK", "Error editing stock item", e)
                    toast(MSG_SAVE_ERROR)
                    false
                }
                if (saved) {
                    dialog.dismiss()
                    loadPage(currentLockerPos)
                }
            }
        }

        lifecycleScope.launch {
            lockers = FirebaseQueries.fetchLockers()
            tools = FirebaseQueries.fetchTools()
            val lockerNameById = lockers.associate { it.id to it.name }

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
                { tool -> "${tool.name} - ${lockerNameById[tool.local] ?: "Armário desconhecido"}" },
                { tool ->
                    selectedTool = tool
                    newPhotoBitmapItem = null
                    b.autoCompleteItem.setText(tool.name, false)
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
    ): Boolean {
        val locker = requireSelectedOrNull(selectedLocker, MSG_SELECT_LOCKER) ?: return false

        val hasNameChange = b.checkChangeLockerName.isChecked
        val hasLocalChange = b.checkChangeLockerLocal.isChecked
        val hasNewPhoto = newPhotoBitmap != null
        val isRemovingPhoto = locker.photoUrl != null && imgPreview.isGone && !hasNewPhoto

        // ── Seletor para o que editar em editlocker ──────────────────────────────────────────────────────────────
        if (!hasNameChange && !hasLocalChange && !hasNewPhoto && !isRemovingPhoto) {
            toast(MSG_SELECT_WHAT_TO_EDIT)
            return false
        }

        if (hasNameChange || hasLocalChange) {
            val newName = if (hasNameChange) {
                b.editNewLockerName.text.toString().trim().also {
                    if (it.isEmpty()) {
                        toast(MSG_ENTER_NEW_NAME)
                        return false
                    }
                }
            } else locker.name
            val newLocal = if (hasLocalChange) {
                b.editNewLockerLocal.text.toString().trim()
            } else locker.local

            FirebaseQueries.updateLocker(locker.id, newName, newLocal)
        }

        when {
            newPhotoBitmap != null -> {
                val newPhotoUrl = getPhotoUrl(newPhotoBitmap)
                FirebaseQueries.updateLockerPhoto(locker.id, newPhotoUrl)
                updateLockerPhotoInPage(locker.id, newPhotoUrl)
            }
            isRemovingPhoto -> {
                FirebaseQueries.updateLockerPhoto(locker.id, null)
                updateLockerPhotoInPage(locker.id, null)
            }
        }

        toast(MSG_LOCKER_UPDATED)
        return true
    }

    private suspend fun editTool(
        b: BottomSheetEditBinding,
        selectedTool: Tool?,
        selectedDestination: Locker?,
        newPhotoBitmap: Bitmap?,
        imgPreview: ImageView
    ): Boolean {
        val tool = requireSelectedOrNull(selectedTool, MSG_SELECT_ITEM) ?: return false

        val hasNameChange = b.checkChangeItemName.isChecked
        val hasLocalChange = b.checkChangeItemLoc.isChecked
        val hasNewPhoto = newPhotoBitmap != null
        val isRemovingPhoto = tool.photoUrl != null && imgPreview.isGone && !hasNewPhoto

        // ── Seletor para o que editar em editTool ──────────────────────────────────────────────────────────────
        if (!hasNameChange && !hasLocalChange && !hasNewPhoto && !isRemovingPhoto) {
            toast(MSG_SELECT_WHAT_TO_EDIT)
            return false
        }

        val newName = if (hasNameChange) {
            b.editNewItemName.text.toString().trim().also {
                if (it.isEmpty()) {
                    toast(MSG_ENTER_NEW_NAME)
                    return false
                }
            }
        } else tool.name

        val newLockerId = if (hasLocalChange) {
            requireSelectedOrNull(selectedDestination, MSG_SELECT_DESTINATION)?.id ?: return false
        } else tool.local

        FirebaseQueries.updateTool(tool.id, newName, tool.local, newLockerId)

        handlePhotoUpdate(hasNewPhoto, isRemovingPhoto, newPhotoBitmap) {
            FirebaseQueries.updateToolPhoto(tool.id, it)
        }

        toast(MSG_ITEM_UPDATED)
        return true
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
        var isLockerMode = true

        bindToggle(b.toggleGroup, b.layoutDeleteLocker, b.layoutDeleteItem) { isLockerMode = it }


        // ── Exige que ferramentas sejam movidas para outro armário antes de deletar ──────────────────────────────────────────────────────────────
        b.btnConfirmDelete.setOnClickListener {
            lifecycleScope.launch {
                val deleted = try {
                    if (isLockerMode) {
                        deleteLocker(selectedLocker, selectedDestination)
                    } else {
                        deleteTool(selectedTool)
                    }
                } catch (e: Exception) {
                    Log.e("STOCK", "Error deleting stock item", e)
                    toast(MSG_DELETE_ERROR)
                    false
                }
                if (deleted) {
                    dialog.dismiss()
                    loadPage(currentLockerPos)
                }
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
                    showPhotoPreview(locker.photoUrl, b.imgPreviewLocker)

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

    private suspend fun deleteLocker(selectedLocker: Locker?, selectedDestination: Locker?): Boolean {
        val locker = requireSelectedOrNull(selectedLocker, MSG_SELECT_LOCKER) ?: return false
        val hasItems = FirebaseQueries.hasToolsInLocker(locker.id)
        if (hasItems && selectedDestination == null) {
            toast(MSG_SELECT_DESTINATION)
            return false
        }

        FirebaseQueries.deleteLocker(locker.id, selectedDestination?.id)
        val message = if (selectedDestination != null)
            MSG_ITEMS_MOVED.format(selectedDestination.name)
        else
            MSG_LOCKER_REMOVED
        toast(message)
        return true
    }

    private suspend fun deleteTool(selectedTool: Tool?): Boolean {
        val tool = requireSelectedOrNull(selectedTool, MSG_SELECT_ITEM) ?: return false
        FirebaseQueries.deleteTool(tool.id, tool.local)
        toast(MSG_ITEM_REMOVED.format(tool.name))
        return true
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun bindToggle(
        group: MaterialButtonToggleGroup,
        layoutLocker: View,
        layoutItem: View,
        onModeChanged: (Boolean) -> Unit = {}
    ) {
        fun updateMode(checkedId: Int) {
            val isLocker = checkedId == R.id.btnToggleLocker
            layoutLocker.visibility = if (isLocker) View.VISIBLE else View.GONE
            layoutItem.visibility = if (isLocker) View.GONE else View.VISIBLE
            onModeChanged(isLocker)
        }

        group.findViewById<View>(R.id.btnToggleLocker)?.setOnClickListener {
            group.check(R.id.btnToggleLocker)
            updateMode(R.id.btnToggleLocker)
        }
        group.findViewById<View>(R.id.btnToggleItem)?.setOnClickListener {
            group.check(R.id.btnToggleItem)
            updateMode(R.id.btnToggleItem)
        }

        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            updateMode(checkedId)
        }

        if (group.checkedButtonId == View.NO_ID) {
            group.check(R.id.btnToggleLocker)
        } else {
            updateMode(group.checkedButtonId)
        }
    }

    private fun updateLockerPhotoInPage(lockerId: String, photoUrl: String?) {
        currentLockers = currentLockers.map { locker ->
            if (locker.id == lockerId) locker.copy(photoUrl = photoUrl) else locker
        }
        renderPage(currentLockerPos)
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
        val options = items.map { AutoCompleteOption(toLabel(it), it) }
        view.setAdapter(dropdownAdapter(options))
        view.setOnClickListener { view.showDropDown() }
        view.setOnItemClickListener { parent, _, pos, _ ->
            @Suppress("UNCHECKED_CAST")
            val option = parent.getItemAtPosition(pos) as? AutoCompleteOption<T>
            option?.value?.let { onSelected(it) }
        }
    }

    private fun <T> requireSelectedOrNull(value: T?, errorMsg: String): T? {
        return value ?: run { toast(errorMsg); null }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun <T> dropdownAdapter(items: List<T>) =
        ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)

}
