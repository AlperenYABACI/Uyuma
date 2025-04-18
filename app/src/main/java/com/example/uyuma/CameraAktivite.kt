package com.example.uyuma

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraActivity
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import org.opencv.android.OpenCVLoader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

class CameraAktivite : CameraActivity() {

    private lateinit var cameraBridgeViewBase: CameraBridgeViewBase
    private var faceCascadeClassifier: CascadeClassifier? = null
    private var eyeGlassesCascadeClassifier: CascadeClassifier? = null
    private var upperBodyCascadeClassifier: CascadeClassifier? = null
    private lateinit var gray: Mat
    private lateinit var rgba: Mat
    private lateinit var faceRects: MatOfRect
    private lateinit var eyeGlassesRects: MatOfRect
    private lateinit var upperBodyRects: MatOfRect
    private lateinit var faceROI: Mat
    private val frameHistory = mutableListOf<Int>()
    private val STATUS_YELLOW = 0
    private val STATUS_BLUE = 1
    private val STATUS_GREEN = 2
    private val STATUS_RED = 3
    private var started = false
    private var frameCount = 0
    private val frameInterval = 10
    private var faceDetectedCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        val textView: TextView = findViewById(R.id.textView)
        val stopButton : Button = findViewById(R.id.stopButton)
        started = false
        getPermission()
        cameraBridgeViewBase = findViewById(R.id.cameraView)
        cameraBridgeViewBase.setCameraIndex(1)
        cameraBridgeViewBase.setCvCameraViewListener(object : CameraBridgeViewBase.CvCameraViewListener2 {
            override fun onCameraViewStarted(width: Int, height: Int) {
                if (!::rgba.isInitialized) {
                    rgba = Mat()
                }
                if (!::gray.isInitialized) {
                    gray = Mat()
                }
                if (!::faceRects.isInitialized) {
                    faceRects = MatOfRect()
                }
                if (!::eyeGlassesRects.isInitialized) {
                    eyeGlassesRects = MatOfRect()
                }
                if (!::upperBodyRects.isInitialized) {
                    upperBodyRects = MatOfRect()
                }
                if (!::faceROI.isInitialized) {
                    faceROI = Mat()
                }
            }

            override fun onCameraViewStopped() {
                rgba.release()
                gray.release()
                faceRects.release()
                upperBodyRects.release()
                eyeGlassesRects.release()
                faceROI.release()
                started = false
            }

            override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
                frameCount++
                val rotatedRgba = Mat()
                Core.rotate(inputFrame?.rgba(), rotatedRgba, Core.ROTATE_90_COUNTERCLOCKWISE)
                val flippedRgba = Mat()
                Core.flip(rotatedRgba, flippedRgba, 1)
                rotatedRgba.release()
                if (frameCount % frameInterval == 0) {
                    Imgproc.cvtColor(flippedRgba, gray, Imgproc.COLOR_RGBA2GRAY)
                    var upperBodyDetected = false
                    var faceDetected = false
                    var eyeGlassesDetected = false
                    frameCount = 0
                    faceCascadeClassifier?.detectMultiScale(gray, faceRects, 1.1, 5, 0, Size(50.0, 50.0))
                    if (!faceRects.empty()) {
                        upperBodyDetected = true
                        faceDetectedCount++
                    } else {
                        faceDetectedCount = 0
                    }
                    if (started || faceDetectedCount >= 6) {
                        if (!started) {
                            frameHistory.clear()
                        }
                        started = true
                        faceCascadeClassifier?.detectMultiScale(gray, faceRects, 1.1, 3, 0, Size(30.0, 30.0))
                        if (!faceRects.empty()) {
                            faceDetected = true
                        }
                        for (rect in faceRects.toArray()) {
                            // Yüz ROI'nin üst yarısını belirleyin
                            val upperFaceROI = rect.clone()
                            upperFaceROI.height = upperFaceROI.height / 2

                            // Yüzün üst yarısında göz tespitini gerçekleştirin
                            val faceROI = gray.submat(upperFaceROI)
                            eyeGlassesCascadeClassifier?.detectMultiScale(faceROI, eyeGlassesRects, 1.1, 3, 0, Size(20.0, 20.0))
                            if (!eyeGlassesRects.empty()) {
                                eyeGlassesDetected = true
                            }
                        }
                    }
                    val status = getStatus(faceDetected, eyeGlassesDetected)
                    frameHistory.add(status)
                    val dominantStatus = getDominantStatus()
                    val statusText = getStatusText(dominantStatus)
                    runOnUiThread {
                        textView.text = statusText
                        when (dominantStatus) {
                            STATUS_YELLOW -> {
                                textView.setTextColor(Color.YELLOW)
                                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(200)
                                }
                                val customRingtoneUri = Uri.parse("android.resource://${packageName}/${R.raw.alarm}")
                                val mediaPlayer = MediaPlayer.create(applicationContext, customRingtoneUri)
                                mediaPlayer.start()
                            }
                            STATUS_BLUE -> {
                                textView.setTextColor(Color.BLUE)
                                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(200)
                                }
                                val customRingtoneUri = Uri.parse("android.resource://${packageName}/${R.raw.alarm}")
                                val mediaPlayer = MediaPlayer.create(applicationContext, customRingtoneUri)
                                mediaPlayer.start()
                            }
                            STATUS_GREEN -> {
                                textView.setTextColor(Color.GREEN)
                                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                                vibrator.cancel()
                                val customRingtoneUri = Uri.parse("android.resource://${packageName}/${R.raw.alarm}")
                                val mediaPlayer = MediaPlayer.create(applicationContext, customRingtoneUri)
                                mediaPlayer.stop()
                                mediaPlayer.release()
                            }
                            STATUS_RED -> {
                                textView.setTextColor(Color.RED)
                                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                                vibrator.cancel()
                                val customRingtoneUri = Uri.parse("android.resource://${packageName}/${R.raw.alarm}")
                                val mediaPlayer = MediaPlayer.create(applicationContext, customRingtoneUri)
                                mediaPlayer.stop()
                                mediaPlayer.release()
                            }
                        }
                    }
                    Imgproc.circle(flippedRgba, Point(20.0, 20.0), 10, Scalar(255.0, 0.0, 0.0), -1)
                }
                return flippedRgba
            }
        })

        if (OpenCVLoader.initDebug()) {
            cameraBridgeViewBase.enableView()

            try {
                val faceInputStream: InputStream = resources.openRawResource(R.raw.haarcascade_frontalface_alt)
                val faceCascadeFile = File(getDir("cascade", MODE_PRIVATE), "haarcascade_frontalface_alt2.xml")
                val faceFileOutputStream = FileOutputStream(faceCascadeFile)
                val faceData = ByteArray(4096)
                var faceReadBytes: Int

                while (faceInputStream.read(faceData).also { faceReadBytes = it } != -1) {
                    faceFileOutputStream.write(faceData, 0, faceReadBytes)
                }

                faceCascadeClassifier = CascadeClassifier(faceCascadeFile.absolutePath)
                if (faceCascadeClassifier?.empty() == true) {
                    faceCascadeClassifier = null
                }

                faceInputStream.close()
                faceFileOutputStream.close()
                faceCascadeFile.delete()

                val eyeGlassesInputStream: InputStream = resources.openRawResource(R.raw.haarcascade_eye_tree_eyeglasses)
                val eyeGlassesCascadeFile = File(getDir("cascade", MODE_PRIVATE), "haarcascade_eye_tree_eyeglasses.xml")
                val eyeGlassesFileOutputStream = FileOutputStream(eyeGlassesCascadeFile)
                val eyeGlassesData = ByteArray(4096)
                var eyeGlassesReadBytes: Int

                while (eyeGlassesInputStream.read(eyeGlassesData).also { eyeGlassesReadBytes = it } != -1) {
                    eyeGlassesFileOutputStream.write(eyeGlassesData, 0, eyeGlassesReadBytes)
                }

                eyeGlassesCascadeClassifier = CascadeClassifier(eyeGlassesCascadeFile.absolutePath)
                if (eyeGlassesCascadeClassifier?.empty() == true) {
                    eyeGlassesCascadeClassifier = null
                }

                eyeGlassesInputStream.close()
                eyeGlassesFileOutputStream.close()
                eyeGlassesCascadeFile.delete()

                val upperBodyInputStream: InputStream = resources.openRawResource(R.raw.haarcascade_upperbody)
                val upperBodyCascadeFile = File(getDir("cascade", MODE_PRIVATE), "haarcascade_upperbody.xml")
                val upperBodyFileOutputStream = FileOutputStream(upperBodyCascadeFile)
                val upperBodyData = ByteArray(4096)
                var upperBodyReadBytes: Int

                while (upperBodyInputStream.read(upperBodyData).also { upperBodyReadBytes = it } != -1) {
                    upperBodyFileOutputStream.write(upperBodyData, 0, upperBodyReadBytes)
                }

                upperBodyCascadeClassifier = CascadeClassifier(upperBodyCascadeFile.absolutePath)
                if (upperBodyCascadeClassifier?.empty() == true) {
                    upperBodyCascadeClassifier = null
                }

                upperBodyInputStream.close()
                upperBodyFileOutputStream.close()
                upperBodyCascadeFile.delete()

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        stopButton.setOnClickListener {
            val analysis = analyzeAllFrames()
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("analysis", analysis)
            startActivity(intent)
            finish()
        }
    }

    private fun analyzeAllFrames(): String {
        val blueCount = frameHistory.count { it == STATUS_BLUE }
        val yellowCount = frameHistory.count { it == STATUS_YELLOW }
        val greenCount = frameHistory.count { it == STATUS_GREEN }

        return "Tüm frame'lerde:\n" +
                "Önüne Bakmama: $blueCount\n" +
                "Gözler Kapalı: $yellowCount\n" +
                "Normal: $greenCount"
    }


    private fun getStatus(faceDetected: Boolean, eyeGlassesDetected: Boolean): Int {
        return if (faceDetected && !eyeGlassesDetected) {
            STATUS_YELLOW
        } else if (!faceDetected) {
            STATUS_BLUE
        } else {
            STATUS_GREEN
        }
    }

    private fun getDominantStatus(): Int {
        if (!started) {
            return STATUS_RED
        }

        if (frameHistory.size < 10) {
            return STATUS_GREEN
        }

        val lastTenFrames = frameHistory.takeLast(10)
        val mostFrequentStatus = lastTenFrames.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        val mostFrequentCount = lastTenFrames.count { it == mostFrequentStatus }

        return if (mostFrequentCount >= 10) {
            mostFrequentStatus ?: STATUS_GREEN
        } else {
            STATUS_GREEN
        }
    }

    private fun getStatusText(status: Int): String {
        return when (status) {
            STATUS_YELLOW -> "GÖZLERİN KAPANIYOR , UYAN !"
            STATUS_BLUE -> "DİKKATİNİ YOLA VER !"
            STATUS_GREEN -> ""
            STATUS_RED -> "Telefonu Sizi Görecek Şekilde Sabitleyin."
            else -> ""
        }
    }

    override fun onResume() {
        super.onResume()
        cameraBridgeViewBase.enableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraBridgeViewBase.disableView()
    }

    override fun onPause() {
        super.onPause()
        cameraBridgeViewBase.disableView()
    }

    override fun getCameraViewList(): MutableList<out CameraBridgeViewBase> {
        return Collections.singletonList(cameraBridgeViewBase)
    }

    private fun getPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getPermission()
        }
    }
}

