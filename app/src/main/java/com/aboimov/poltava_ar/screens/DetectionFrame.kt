package com.aboimov.poltava_ar.screens

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.aboimov.poltava_ar.features.detector.MonumentAnalyzer
import com.aboimov.poltava_ar.features.detector.MonumentDetector
import com.aboimov.poltava_ar.features.detector.OverlayView
import com.aboimov.poltava_ar.features.embedder.MonumentEmbedder
import com.aboimov.poltava_ar.features.embedder.MonumentMatcher
import com.aboimov.poltava_ar.features.embedder.RecognizedMonument
import java.util.concurrent.ExecutorService

fun createDetectionPreviewView(
    ctx: Context,
    lifecycleOwner: LifecycleOwner,
    matcher: MonumentMatcher,
    embedder: MonumentEmbedder,
    detector: MonumentDetector,
    cameraExecutor: ExecutorService,
    overlayView: OverlayView,
    onMonumentRecognized: (RecognizedMonument?) -> Unit
): PreviewView {
    return PreviewView(ctx).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE

        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = surfaceProvider
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        MonumentAnalyzer(
                            detector = detector,
                            embedder = embedder,
                            matcher = matcher,
                            onResults = { results ->
                                overlayView.setResults(results)
                            },
                            onMonumentRecognized = onMonumentRecognized
                        )
                    )
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("YOLO_DEBUG", "Camera bind error", e)
            }
        }, ContextCompat.getMainExecutor(ctx))
    }
}
