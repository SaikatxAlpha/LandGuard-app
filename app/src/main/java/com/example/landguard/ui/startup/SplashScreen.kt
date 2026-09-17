package com.example.landguard.ui.startup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.landguard.ui.brand.BrandColors
import com.example.landguard.ui.brand.LandGuardBrandWordmark
import com.example.landguard.ui.brand.LandGuardLogo
import com.example.landguard.ui.brand.LandGuardTagline
import com.example.landguard.ui.brand.ScenicTerrain
import com.example.landguard.ui.brand.SystemBarIcons
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SPLASH_TOTAL_MS = 2600

@Composable
fun LandGuardSplash(onFinished: () -> Unit) {
    SystemBarIcons(darkIcons = false)

    val finish by rememberUpdatedState(onFinished)

    val logoScale = remember { Animatable(0.82f) }
    val logoAlpha = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordShift = remember { Animatable(18f) }
    val tagAlpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    val sceneScale = remember { Animatable(1.08f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { sceneScale.animateTo(1f, tween(SPLASH_TOTAL_MS, easing = LinearOutSlowInEasing)) }
            launch { logoAlpha.animateTo(1f, tween(650)) }
            launch { logoScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
            launch {
                delay(350)
                launch { wordAlpha.animateTo(1f, tween(600)) }
                wordShift.animateTo(0f, tween(650, easing = FastOutSlowInEasing))
            }
            launch {
                delay(750)
                tagAlpha.animateTo(1f, tween(600))
            }
            launch {
                delay(400)
                progress.animateTo(1f, tween(SPLASH_TOTAL_MS - 600, easing = FastOutSlowInEasing))
            }
        }
        delay(150)
        finish()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandColors.ForestNight)
    ) {
        val logoSize = min(maxWidth * 0.36f, 168.dp)
        val compact = maxHeight < 640.dp
        val narrow = maxWidth < 360.dp
        val dimRadiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { (this@BoxWithConstraints.maxWidth * 0.9f).toPx() }

        ScenicTerrain(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = sceneScale.value
                    scaleY = sceneScale.value
                },
            sunX = 0.80f,
            sunY = 0.36f,
            horizon = 0.40f
        )

        // Soft dim behind the lockup for legibility.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(BrandColors.ForestNight.copy(alpha = 0.55f), Color.Transparent),
                        radius = dimRadiusPx
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = if (compact) 40.dp else 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LandGuardLogo(
                size = logoSize,
                modifier = Modifier.graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                }
            )
            Spacer(Modifier.height(if (compact) 10.dp else 16.dp))
            LandGuardBrandWordmark(
                fontSize = if (compact) 38.sp else 46.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = wordAlpha.value
                    translationY = wordShift.value * density
                }
            )
            Spacer(Modifier.height(6.dp))
            LandGuardTagline(
                fontSize = if (narrow) 9.sp else 11.sp,
                modifier = Modifier.graphicsLayer { alpha = tagAlpha.value }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 48.dp)
                .padding(bottom = if (compact) 28.dp else 56.dp)
                .graphicsLayer { alpha = tagAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BrandColors.Mist.copy(alpha = 0.14f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.value)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(BrandColors.Forest, BrandColors.Leaf)))
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Scanning the Earth for a safer tomorrow…",
                color = BrandColors.MistMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
