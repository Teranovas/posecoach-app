package com.example.posecoach.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import coil.load
import com.example.posecoach.databinding.ActivityCameraBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var b: ActivityCameraBinding
    private val vm: PoseViewModel by viewModels()

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else {
            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(b.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        val modes = listOf("default(빈값)", "squat", "pushup")
        b.modeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)

        b.btnCapture.setOnClickListener { takePhotoAndSend() }
        b.btnBack.setOnClickListener { finish() }

        vm.state.observe(this) { state ->
            when (state) {
                is UiState.Idle -> Unit
                is UiState.Loading -> b.statusText.text = "분석 중..."
                is UiState.SimpleOk -> b.statusText.text =
                    "pose=${state.data.pose} • score=${state.data.score}\n${state.data.feedback}"
                is UiState.FullOk -> b.statusText.text =
                    "ok=${state.data.ok} • feedback=${state.data.feedback?.joinToString(" / ")}"
                is UiState.OverlayOk -> {
                    b.previewImage.load(state.bytes)
                    b.statusText.text = "오버레이 수신 완료"
                }
                is UiState.Error -> b.statusText.text = "에러: ${state.message}"
            }
        }

        if (hasCameraPermission()) startCamera() else requestPermission.launch(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(b.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setJpegQuality(85)
                .build()

            val selector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, selector, preview, imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraActivity", "Use case binding failed", e)
                Toast.makeText(this, "카메라 시작 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoAndSend() {
        val capture = imageCapture ?: return
        val file = File(cacheDir, "captured_${System.currentTimeMillis()}.jpg")
        val output = ImageCapture.OutputFileOptions.Builder(file).build()

        capture.takePicture(
            output,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(this@CameraActivity, "촬영 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val mode = spinnerModeOrNull()
                    // 오버레이 PNG로 받아 화면에 표시(가장 직관적)
                    vm.overlay(file, mode)
                    runOnUiThread {
                        b.statusText.text = "서버로 업로드 중..."
                    }
                }
            }
        )
    }

    private fun spinnerModeOrNull(): String? {
        val sel = b.modeSpinner.selectedItem?.toString() ?: return null
        return if (sel.startsWith("default")) null else sel
    }
}
