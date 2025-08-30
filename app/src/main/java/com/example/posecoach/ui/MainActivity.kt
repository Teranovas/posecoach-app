package com.example.posecoach.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.posecoach.R            // ✅ 앱 R 로 교체
import com.example.posecoach.databinding.ActivityMainBinding
import android.speech.tts.TextToSpeech
import java.util.Locale


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var b: ActivityMainBinding
    private val vm: PoseViewModel by viewModels()   // ✅ vm 으로 통일

    private var pickedFile: java.io.File? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = copyUriToCache(uri)
            pickedFile = file
            b.imageView.load(file)
            b.resultText.text = "이미지 선택 완료: ${file.name}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        tts = TextToSpeech(this, this)



        // 모드 스피너
        val modes = listOf("default(빈값)", "squat", "pushup")
        b.modeSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)

        // 피드백 리스트 UI
        b.rvFeedback.layoutManager = LinearLayoutManager(this)

        // 버튼들
        b.btnPick.setOnClickListener { pickImage.launch("image/*") }

        b.btnCamera.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        b.btnSendSimple.setOnClickListener {
            val f = pickedFile ?: return@setOnClickListener
            val mode = spinnerModeOrNull()
            vm.analyzeSimple(f, mode)
        }

        b.btnSendFull.setOnClickListener {
            val f = pickedFile ?: return@setOnClickListener
            val mode = spinnerModeOrNull()
            vm.analyzeFull(f, mode)
        }

        b.btnOverlay.setOnClickListener {
            val f = pickedFile ?: return@setOnClickListener
            val mode = spinnerModeOrNull()
            vm.overlay(f, mode)
        }

        // 상태 관찰 (LiveData)
        vm.state.observe(this) { state ->
            when (state) {
                is UiState.Idle -> {
                    // 필요 시 초기화 UI
                }
                is UiState.Loading -> {
                    b.resultText.text = "요청 중..."
                }
                is UiState.SimpleOk -> {
                    // Simple: feedback 이 String? 이므로 리스트로 변환
                    val items = listOfNotNull(state.data.feedback)
                    b.rvFeedback.adapter = FeedbackAdapter(items)
                    b.resultText.text =
                        "pose=${state.data.pose}\nfeedback=${state.data.feedback}\nscore=${state.data.score}"

                    if (items.isNotEmpty()) speakIfEnabled(items.first())
                }
                is UiState.FullOk -> {
                    val items = state.data.feedback ?: emptyList()
                    b.rvFeedback.adapter = FeedbackAdapter(items)
                    b.resultText.text = buildString {
                        append("ok=${state.data.ok}\n")
                        append("feedback=${items.joinToString(" / ")}\n")
                        append("angles=${state.data.angles}\n")
                        append("metrics=${state.data.metrics}\n")
                    }
                }
                is UiState.OverlayOk -> {
                    val bmp = BitmapFactory.decodeByteArray(state.bytes, 0, state.bytes.size)
                    b.imageView.setImageBitmap(bmp)
                    b.resultText.text = "오버레이 수신 완료 (PNG)"
                    // overlay는 피드백이 없을 수 있음
                    b.rvFeedback.adapter = FeedbackAdapter(emptyList())
                }
                is UiState.Error -> {
                    b.resultText.text = "에러: ${state.message}"
                    b.rvFeedback.adapter = FeedbackAdapter(emptyList())
                }
            }
        }
    }

    private fun spinnerModeOrNull(): String? {
        val sel = b.modeSpinner.selectedItem?.toString() ?: return null
        return if (sel.startsWith("default")) null else sel
    }

    private fun copyUriToCache(uri: Uri): java.io.File {
        val input = contentResolver.openInputStream(uri)!!
        val outFile = java.io.File(cacheDir, "picked_${System.currentTimeMillis()}.jpg")
        java.io.FileOutputStream(outFile).use { out -> input.copyTo(out) }
        return outFile
    }

    private fun speakIfEnabled(text: String) {
        if (!b.switchTts.isChecked) return
        if (!ttsReady) return
        if (text.isBlank()) return
        // QUEUE_FLUSH: 직전 발화를 끊고 바로 재생
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "POSE_FEEDBACK")
    }

    override fun onInit(status: Int) {
        ttsReady = (status == TextToSpeech.SUCCESS)
        if (ttsReady) {
            // 한국어 설정 (미지원 단말이면 기본 로케일)
            val ok = tts?.setLanguage(Locale.KOREAN)
            if (ok == TextToSpeech.LANG_MISSING_DATA || ok == TextToSpeech.LANG_NOT_SUPPORTED) {
                // 한국어 미지원 시 시스템 기본으로 폴백
                tts?.language = Locale.getDefault()
            }
            // 읽기 속도/피치 약간 안정화
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)
        }
    }
    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
