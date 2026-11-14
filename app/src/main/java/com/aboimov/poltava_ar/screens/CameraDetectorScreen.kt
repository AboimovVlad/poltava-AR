package com.aboimov.poltava_ar.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aboimov.poltava_ar.database.AppDatabase
import com.aboimov.poltava_ar.features.detector.MonumentDetector
import com.aboimov.poltava_ar.features.detector.OverlayView
import com.aboimov.poltava_ar.features.embedder.MonumentEmbedder
import com.aboimov.poltava_ar.features.embedder.MonumentMatcher
import com.aboimov.poltava_ar.features.embedder.RecognizedMonument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    var recognizedMonument by remember { mutableStateOf<RecognizedMonument?>(null) }

    val database = remember { AppDatabase.getDatabase(context) }
    val dao = remember { database.monumentDao() }
    val matcher = remember { MonumentMatcher(dao, similarityThreshold = 0.70f) }

    val overlayView = remember { OverlayView(context) }
    val detector = remember { MonumentDetector(context, "best.tflite") }
    val embedder = remember { MonumentEmbedder(context, "efficientnet_lite0_android.tflite") }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            matcher.preloadEmbeddings()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            detector.close()
            embedder.close()
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
                    createDetectionPreviewView(
                        ctx = ctx,
                        lifecycleOwner = lifecycleOwner,
                        matcher = matcher,
                        embedder = embedder,
                        detector = detector,
                        cameraExecutor = cameraExecutor,
                        overlayView = overlayView,
                        onMonumentRecognized = { match ->
                            recognizedMonument = match
                        }
                    )
                }
            )

            AndroidView(
                factory = { overlayView },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
            )

            AnimatedVisibility(
                visible = recognizedMonument != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .zIndex(3f)
            ) {
                recognizedMonument?.let { monument ->
                    RecognitionMonumentCard(
                        monument = monument,
                        onMoreClick = {
                            
                        }
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
