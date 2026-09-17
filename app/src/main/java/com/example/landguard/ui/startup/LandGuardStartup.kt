package com.example.landguard.ui.startup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.landguard.ui.brand.StartupPrefs

private enum class StartupStage { SPLASH, ONBOARDING, APP }

/**
 * Launch experience: branded splash → first-run onboarding → [appContent].
 *
 * @param skipIntro     go straight to the app (e.g. opened from an alert notification).
 * @param onEnteredApp  called once when the app content is first shown.
 */
@Composable
fun LandGuardStartup(
    skipIntro: Boolean,
    onEnteredApp: () -> Unit,
    appContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    val enteredApp by rememberUpdatedState(onEnteredApp)

    var stage by rememberSaveable {
        mutableStateOf(if (skipIntro) StartupStage.APP else StartupStage.SPLASH)
    }

    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            if (targetState == StartupStage.APP) {
                (fadeIn(tween(450)) + scaleIn(tween(450), initialScale = 0.98f)) togetherWith fadeOut(tween(350))
            } else {
                fadeIn(tween(500)) togetherWith fadeOut(tween(400))
            }
        },
        label = "startupStage"
    ) { current ->
        Box(Modifier.fillMaxSize()) {
            when (current) {
                StartupStage.SPLASH -> LandGuardSplash(
                    onFinished = {
                        stage = if (StartupPrefs.isOnboardingComplete(context)) StartupStage.APP else StartupStage.ONBOARDING
                    }
                )

                StartupStage.ONBOARDING -> LandGuardOnboarding(
                    onFinished = {
                        StartupPrefs.setOnboardingComplete(context)
                        stage = StartupStage.APP
                    }
                )

                StartupStage.APP -> appContent()
            }
        }
    }

    LaunchedEffect(stage == StartupStage.APP) {
        if (stage == StartupStage.APP) enteredApp()
    }
}
