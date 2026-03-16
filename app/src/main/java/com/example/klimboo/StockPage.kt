package com.example.klimboo

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.klimboo.databinding.ActivityStockPageBinding
import com.example.klimboo.databinding.BottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore


class StockPage : AppCompatActivity() {

    private lateinit var binding: ActivityStockPageBinding


    @OptIn(ExperimentalMaterial3Api::class)
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

                binding.editStock.setOnClickListener {
                    val dialogBinding = BottomSheetBinding.inflate(layoutInflater)
                    val dialog = BottomSheetDialog(this)

                    dialogBinding.btnAdd.setOnClickListener {
                        dialog.dismiss()
                        openSheet(R.layout.bottom_sheet_add)
                    }

                    dialogBinding.btnEdit.setOnClickListener {
                        dialog.dismiss()
                        openSheet(R.layout.bottom_sheet_edit)
                    }

                    dialogBinding.btnDelete.setOnClickListener {
                        dialog.dismiss()
                        openSheet(R.layout.bottom_sheet_delete)
                    }
                    dialog.setContentView(dialogBinding.root)
                    dialog.show()
                }
            }


    }
    // Funcao para abrir os sheets secundarios
    private fun openSheet(layoutId: Int) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(layoutId, null)
        dialog.setContentView(view)
        dialog.show()
    }
}

