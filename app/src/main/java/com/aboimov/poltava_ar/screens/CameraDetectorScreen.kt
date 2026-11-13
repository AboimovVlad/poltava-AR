package com.aboimov.poltava_ar.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aboimov.poltava_ar.features.detector.DetectionResult
import com.aboimov.poltava_ar.features.detector.MonumentAnalyzer
import com.aboimov.poltava_ar.features.detector.MonumentDetector
import java.util.concurrent.Executors

@Composable
fun CameraDetectorScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Используем State-объект напрямую, чтобы лямбда анализатора всегда обновляла актуальное состояние
    val detectionsState = remember { mutableStateOf<List<DetectionResult>>(emptyList()) }

    val detector = remember { MonumentDetector(context, "best_int8.tflite") }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            detector.close()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { hasCameraPermission = it }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
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
                                        MonumentAnalyzer(detector) { results ->
                                            ContextCompat.getMainExecutor(ctx).execute {
                                                detectionsState.value = results
                                            }
                                        }
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
            )

            // Явно поднимаем Canvas над AndroidView с помощью zIndex(1f)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                // Тестовый маркер в углу (50x50 px), чтобы убедиться, что Canvas виден поверх камеры
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(20f, 20f),
                    size = Size(50f, 50f),
                    style = Stroke(width = 6f)
                )

                val currentDetections = detectionsState.value
                currentDetections.forEach { box ->
                    val left = box.left * size.width
                    val top = box.top * size.height
                    val w = (box.right - box.left) * size.width
                    val h = (box.bottom - box.top) * size.height

                    Log.d("YOLO_DEBUG", "DRAW BOX -> left=$left, top=$top, w=$w, h=$h")

                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(left, top),
                        size = Size(width = w, height = h),
                        style = Stroke(width = 10f)
                    )
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text(text = "Дозвольте доступ до камери)")
            }
        }
    }
}
