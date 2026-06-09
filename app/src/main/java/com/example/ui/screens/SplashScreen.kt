package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmiriQuran
import com.example.ui.theme.PoppinsBold
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    var showEnglish by remember { mutableStateOf(false) }
    var showArabic by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showEnglish = true
        delay(800)
        showArabic = true
        delay(1500)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AnimatedVisibility(
                visible = showEnglish,
                enter = fadeIn(animationSpec = tween(durationMillis = 600))
            ) {
                Text(
                    text = "Welcome Doctor",
                    fontFamily = PoppinsBold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            AnimatedVisibility(
                visible = showArabic,
                enter = fadeIn(animationSpec = tween(durationMillis = 600))
            ) {
                Text(
                    text = "إن الله يحب إذا عمل أحدكم عملاً أن يتقنه",
                    fontFamily = AmiriQuran,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 42.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}
