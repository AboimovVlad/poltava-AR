package com.aboimov.poltava_ar.screens

import android.media.MediaPlayer
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aboimov.poltava_ar.database.MonumentEntity
import com.aboimov.poltava_ar.features.embedder.RecognizedMonument
import java.security.cert.Extension

@Composable
fun MoreInfoMonumentScreen(
    monument: RecognizedMonument,
    details: MonumentEntity?,
    onClose: () -> Unit
) {
    var isStarted by remember { mutableStateOf(false) }

    val outerPadding by animateDpAsState(
        targetValue = if (isStarted) 16.dp else 0.dp,
        animationSpec = tween(durationMillis = 250),
        label = "outerPadding"
    )
    val bottomCorner by  animateDpAsState(
        targetValue = if (isStarted) 20.dp else 0.dp,
        animationSpec = tween(durationMillis = 250),
        label = "bottomCorner"
    )

    val heightFraction by animateFloatAsState(
        targetValue = if (isStarted) 0.85f else 0.15f,
        animationSpec = tween(durationMillis = 300, delayMillis = 200),
        label = "heightFraction"
    )

    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isAudioReady by remember { mutableStateOf(false) }
    val mediaPlayer  = remember { MediaPlayer() }

    LaunchedEffect(Unit) {
        isStarted = true
    }

    LaunchedEffect(details?.audioPath) {
        val path = details?.audioPath
        if (!path.isNullOrBlank()) {
            try {
                val afd = context.assets.openFd(path)
                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener { isAudioReady = true }
                mediaPlayer.setOnCompletionListener { isPlaying = true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
    }

    Card(
        shape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = bottomCorner,
            bottomEnd = bottomCorner
        ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(heightFraction)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(outerPadding)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = monument.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Рік створення ${monument.year}p.",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.DarkGray
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "close",
                            tint = Color.DarkGray
                        )
                    }
                }
                AnimatedVisibility(
                    visible = isStarted && details != null,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 300, delayMillis = 450)
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 150))
                ) {
                    details?.let {info ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                            Text(
                                text = info.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.DarkGray
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            YoutubePreviewCard(info.youtubeUrl)

                        }
                    }
                }
            }
            if(isStarted && details != null && details.audioPath.isNotBlank()) {
                FloatingActionButton(
                    onClick = {
                        if (!isAudioReady) return@FloatingActionButton
                        if (isPlaying) {
                            mediaPlayer.pause()
                            isPlaying = false
                        } else {
                            mediaPlayer.start()
                            isPlaying = true
                        }
                    },
                    shape = CircleShape,
                    containerColor = Color.DarkGray,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.ArrowDropDown else Icons.Default.PlayArrow,
                        contentDescription = "озвучка"
                    )
                }
            }
        }
    }
}

