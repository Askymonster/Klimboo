package com.example.klimboo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.klimboo.databinding.ActivityStockPageBinding
import com.google.android.material.bottomsheet.BottomSheetDialog


class StockPage : AppCompatActivity() {

    private lateinit var binding: ActivityStockPageBinding


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_stock_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        binding = ActivityStockPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.editStock.setOnClickListener {
            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate((R.layout.bottom_sheet), null)
            dialog.setContentView(view)
            dialog.show()
        }


    }
}

