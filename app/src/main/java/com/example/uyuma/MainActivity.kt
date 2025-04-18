package com.example.uyuma

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.view.Gravity
import android.graphics.Color

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton: ImageView = findViewById(R.id.startButton)
        startButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, CameraAktivite::class.java)
                startActivity(intent)
                startButton.isEnabled = false
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            }
        }

        // Uygulama başlangıcında da bir mesaj göster
        showInitialMessage()

        val analysis = intent.getStringExtra("analysis")
        if (!analysis.isNullOrBlank()) {
            val analysisTextView: TextView = findViewById(R.id.analysisTextView)
            val result = analyzeResults(analysis)
            analysisTextView.text = result

            // Animasyon ve stil uygulaması
            if (result != "DURUMUNUZ İYİ") {
                analysisTextView.setTextColor(Color.RED)
                analysisTextView.setTypeface(null, Typeface.BOLD)
                analysisTextView.setBackgroundColor(Color.BLACK)
                val blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink)
                analysisTextView.startAnimation(blinkAnimation)
            } else {
                val normalMessages = listOf(
                    "Kaza geliyorum der, kulak verin yeter!",
                    "Trafik dikkat ister, azami gayret göster.",
                    "Uykunu yolda alma, insanların kâbusu olma!",
                    "Yorgun çıkma yola, dinlen ver bir mola.",
                    "Trafikte bir hata yüz doğru götürür.",
                    "Uykunu al, hayatta kal."
                )
                val randomMessage = normalMessages.random()
                analysisTextView.text = randomMessage
                analysisTextView.setTextColor(Color.BLACK)
                analysisTextView.setTypeface(null, Typeface.ITALIC)
                analysisTextView.setBackgroundColor(Color.TRANSPARENT)
                analysisTextView.gravity = Gravity.CENTER_HORIZONTAL
            }
        }
    }

    private fun showInitialMessage() {
        val initialMessage = "HOŞGELDİNİZ"
        val initialTextView: TextView = findViewById(R.id.analysisTextView)
        initialTextView.text = initialMessage
        initialTextView.setTextColor(Color.BLACK)
        initialTextView.setTypeface(null, Typeface.ITALIC)
        initialTextView.setBackgroundColor(Color.TRANSPARENT)
        initialTextView.gravity = Gravity.CENTER_HORIZONTAL
    }

    private fun analyzeResults(analysis: String): String {
        val lines = analysis.split("\n")
        val frontNotLookingCount = lines[1].split(": ")[1].toInt()
        val eyesClosedCount = lines[2].split(": ")[1].toInt()
        val normalCount = lines[3].split(": ")[1].toInt()
        val totalCount = frontNotLookingCount + eyesClosedCount + normalCount

        val frontNotLookingPercentage = (frontNotLookingCount.toDouble() / totalCount) * 100
        val eyesClosedPercentage = (eyesClosedCount.toDouble() / totalCount) * 100

        val warnings = mutableListOf<String>()

        if (frontNotLookingPercentage > 25) {
            warnings.add("DİKKATİNİZ DAĞINIK!\n")
        }
        if (eyesClosedPercentage > 15) {
            warnings.add("GÖZÜNÜZ ÇOK SIK KAPANIYOR!")
        }
        if (warnings.isNotEmpty()) {
            warnings.add("LÜTFEN DİNLENİN!")
        }

        return if (warnings.isEmpty()) {
            "DURUMUNUZ İYİ"
        } else {
            warnings.joinToString("\n")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            findViewById<Button>(R.id.startButton).isEnabled = false
        }
    }
}
