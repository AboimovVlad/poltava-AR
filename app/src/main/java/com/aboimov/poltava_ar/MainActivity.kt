package com.aboimov.poltava_ar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aboimov.poltava_ar.screens.CameraDetectorScreen
import com.aboimov.poltava_ar.ui.theme.PoltavaarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoltavaarTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .background(
                                    Color.White.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(0.0f, 0.0f, 20.0f, 20.0f)
                                )
                                .height(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Poltava-AR",
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontSize = 25.sp
                            )
                        }
                    }
                ) {_ ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraDetectorScreen()
                    }
                }
            }
        }
    }
}
