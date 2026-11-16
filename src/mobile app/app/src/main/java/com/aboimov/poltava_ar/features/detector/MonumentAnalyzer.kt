package com.aboimov.poltava_ar.features.detector

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.aboimov.poltava_ar.features.embedder.MonumentEmbedder
import com.aboimov.poltava_ar.features.embedder.MonumentMatcher
import com.aboimov.poltava_ar.features.embedder.RecognizedMonument

class MonumentAnalyzer(
    private val detector: MonumentDetector,
    private val embedder: MonumentEmbedder,
    private val matcher: MonumentMatcher,
    private val onResults: (List<DetectionResult>) -> Unit,
    private val onMonumentRecognized: (RecognizedMonument?) -> Unit
) : ImageAnalysis.Analyzer {
    private var lastEmbedTime = 0L
    private val embedIntervalMs = 1000L

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

            val box = results.firstOrNull()
            val currentTime = System.currentTimeMillis()

            if (box != null) {
                if (currentTime - lastEmbedTime >= embedIntervalMs) {
                    lastEmbedTime = currentTime
                    val cropped = cropDetection(rotated, box)
                    if (cropped != null) {
                        val vector = embedder.getEmbedding(cropped)
                        val match = matcher.findBestMatch(vector)
                        onMonumentRecognized(match)
                        cropped.recycle()
                    }
                }
            } else {
                onMonumentRecognized(null)
            }
        } catch (e: Exception) {
            Log.e("YOLO_DEBUG", "Analyze error: ${e.message}", e)
            e.printStackTrace()
        } finally {
            image.close()
        }
    }

    private fun cropDetection(source: Bitmap, box: DetectionResult): Bitmap? {
        val left = (box.left * source.width).toInt().coerceIn(0, source.width - 1)
        val top = (box.top * source.height).toInt().coerceIn(0, source.height - 1)
        val right = (box.right * source.width).toInt().coerceIn(left + 1, source.width)
        val bottom = (box.bottom * source.height).toInt().coerceIn(top + 1, source.height)

        val width = right - left
        val height = bottom - top
        if (width < 20 || height < 20) return null

        return Bitmap.createBitmap(source, left, top, width, height)
    }
}
