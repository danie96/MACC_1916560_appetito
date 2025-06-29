package com.example.macc_1916560

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class PoseActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var poseOverlay: PoseOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        poseOverlay = findViewById(R.id.poseOverlay)

        val snapshotContainer = findViewById<FrameLayout>(R.id.snapshotContainer)
        val snapshotView = findViewById<ImageView>(R.id.snapshotView)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        val captureButton = findViewById<Button>(R.id.savePoseButton)

        var lastCapturedBitmap: Bitmap? = null

        captureButton.setOnClickListener {
            val snapshot = captureAndSaveFrame()
            lastCapturedBitmap = snapshot
            snapshotView.setImageBitmap(snapshot)
            snapshotContainer.visibility = View.VISIBLE
        }

        saveButton.setOnClickListener {
            lastCapturedBitmap?.let {
                saveToGallery(it)
                Toast.makeText(this, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
            }
            snapshotContainer.visibility = View.GONE
        }

        deleteButton.setOnClickListener {
            lastCapturedBitmap = null
            snapshotContainer.visibility = View.GONE
        }

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun captureAndSaveFrame(): Bitmap {
        val bitmap = Bitmap.createBitmap(previewView.width, previewView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        previewView.draw(canvas)
        poseOverlay.draw(canvas)
        return bitmap
    }

    private fun saveToGallery(bitmap: Bitmap): Uri? {
        val filename = "pose_${System.currentTimeMillis()}.png"
        val resolver = contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/PoseApp")
        }

        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        return uri
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val file = File(cacheDir, "pose_snapshot_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(previewView.display.rotation)
                .build()

            val options = PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
            val poseDetector = PoseDetection.getClient(options)

            analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                imageProxy.image?.let { mediaImage ->
                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    poseDetector.process(inputImage)
                        .addOnSuccessListener { pose ->
                            poseOverlay.setPose(
                                pose,
                                mediaImage.width,
                                mediaImage.height,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            Log.d("PoseDebug", "rotationDegrees = ${imageProxy.imageInfo.rotationDegrees}")

                        }
                        .addOnCompleteListener { imageProxy.close() }
                } ?: imageProxy.close()
            }

            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(requestCode: Int, perms: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, perms, grantResults)
        if (requestCode == 10 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            finish()
        }
    }
}
