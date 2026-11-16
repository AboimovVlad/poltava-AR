package com.aboimov.poltava_ar.screens
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun YoutubePreviewCard(
    youtubeUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoId = remember(youtubeUrl) { extractYoutubeVideoId(youtubeUrl) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (videoId != null) {
        val thumbnailUrl = "https://i.ytimg.com/vi/$videoId/0.jpg"

        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E))
                .clickable {
                    val watchUrl = Uri.parse(youtubeUrl)
                    val customTabsIntent = CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                    customTabsIntent.launchUrl(context, watchUrl)
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Превью видео",
                contentScale = ContentScale.Crop,
                onError = { result ->
                    val err = result.result.throwable.message ?: "Ошибка загрузки"
                    Log.e("CoilYouTube", "Failed to load $thumbnailUrl: $err")
                    errorMessage = err
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
            )

            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF0000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Смотреть видео",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            errorMessage?.let { err ->
                Text(
                    text = err,
                    color = Color.Yellow,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(6.dp)
                )
            }
        }
    }
}

private fun extractYoutubeVideoId(url: String): String? {
    val cleanUrl = url.trim()
    val uri = Uri.parse(cleanUrl)
    val host = uri.host?.lowercase() ?: return null

    return when {
        // Формат: https://youtu.be...
        host.contains("youtu.be") -> {
            uri.pathSegments.firstOrNull()
        }
        // Формат: https://youtube.com
        uri.getQueryParameter("v") != null -> {
            uri.getQueryParameter("v")
        }
        // Форматы: /embed/ID, /shorts/ID, /v/ID
        uri.pathSegments.size >= 2 && uri.pathSegments[0] in listOf("embed", "shorts", "v") -> {
            uri.pathSegments[1]
        }
        else -> null
    }?.takeIf { it.length == 11 }
}
