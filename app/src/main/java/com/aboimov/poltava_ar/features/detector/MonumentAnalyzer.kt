package com.aboimov.poltava_ar.features.detector

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class MonumentAnalyzer(
    private val detector: MonumentDetector,
    private val onResults: (List<DetectionResult>) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            val bitmap = image.toBitmap()
            val matrix = Matrix().apply {
                postRotate(image.imageInfo.rotationDegrees.toFloat())
            }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            val results = detector.detect(rotated)
            onResults(results)
        } catch (e: Exception) {
            Log.e("YOLO_DEBUG", "Analyze error: ${e.message}", e)
            e.printStackTrace()
        } finally {
            image.close()
        }
    }
}
