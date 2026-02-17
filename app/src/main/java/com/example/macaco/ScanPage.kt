package com.example.macaco

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ScanPage : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultTextView: TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scan_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
        previewView = findViewById(R.id.previewView)
        resultTextView = findViewById(R.id.resultTextView)
        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient()

        // Requerer permissão de câmera
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission())
        { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                resultTextView.text = getString(R.string.precisa_permissao)
            }
        }
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Iniciar câmera
    private fun startCamera(){
        val cameraProvideFuture = ProcessCameraProvider.getInstance(this)
        val screenSize = android.util.Size(1280,760)
        val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(
            ResolutionStrategy(screenSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER)
        ).build()

        cameraProvideFuture.addListener({
            val cameraProvider = cameraProvideFuture.get()
            val preview = Preview.Builder().setResolutionSelector(resolutionSelector)
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }
            // Setar Câmera traseira como deffault

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll() // Limpa antes de vincular
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy){
        val mediaImage = imageProxy.image
        if (mediaImage != null){
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes){
                        handleBarcode(barcode)
                    }
                }
                .addOnFailureListener {
                    resultTextView.text = getString(R.string.falha_ao_escanear_qr_code)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    // Lidar com código de barras
    private fun handleBarcode(barcode: Barcode) {
        val url = barcode.url?.url ?: barcode.displayValue
        runOnUiThread {
            if (url != null) {
                resultTextView.text = url
                resultTextView.setOnClickListener {
                    val intent = Intent(this, WebViewActivity::class.java)
                    intent.putExtra("url", url)
                    startActivity(intent)
                }
            } else {
                resultTextView.text = getString(R.string.qr_code_nao_detectado)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}