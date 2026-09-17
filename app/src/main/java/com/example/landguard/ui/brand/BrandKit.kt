package com.example.landguard.ui.brand

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.landguard.R

/*
 * LandGuard brand kit — the identity used by the launch experience
 * (splash, onboarding, server setup) and the launcher icon.
 *
 * Palette: forest night, forest, leaf, ember (hazard) and mist.
 */
object BrandColors {
    val ForestNight = Color(0xFF0B1F14)
    val ForestDeep = Color(0xFF0F2A1B)
    val Forest = Color(0xFF2E7D32)
    val Leaf = Color(0xFF6DBF67)
    val LeafBright = Color(0xFF8FD18A)
    val Ember = Color(0xFFFF8A3D)
    val Hazard = Color(0xFFFF4D3D)
    val Watch = Color(0xFFFFB02E)
    val Mist = Color(0xFFF4F6F1)
    val MistMuted = Color(0xFFB9C7BC)
    val Card = Color(0xB3102A1C)
    val CardBorder = Color(0x266DBF67)
}

const val LANDGUARD_TAGLINE = "Safer Lands. Stronger Tomorrows."

@Composable
fun LandGuardLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    Image(
        painter = painterResource(R.drawable.ic_landguard_logo),
        contentDescription = "LandGuard logo",
        modifier = modifier.size(size)
    )
}

/** "Land" in mist + "Guard" in leaf green. */
@Composable
fun LandGuardBrandWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 44.sp,
    landColor: Color = BrandColors.Mist,
    guardColor: Color = BrandColors.Leaf
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = landColor)) { append("Land") }
            withStyle(SpanStyle(color = guardColor)) { append("Guard") }
        },
        modifier = modifier,
        fontSize = fontSize,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.02).em,
        textAlign = TextAlign.Center
    )
}

@Composable
fun LandGuardTagline(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp,
    color: Color = BrandColors.MistMuted
) {
    Text(
        text = LANDGUARD_TAGLINE.uppercase(),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.22.em,
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}

@Composable
fun LandGuardLockup(
    modifier: Modifier = Modifier,
    logoSize: Dp = 132.dp,
    wordmarkSize: TextUnit = 44.sp
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LandGuardLogo(size = logoSize)
        LandGuardBrandWordmark(fontSize = wordmarkSize)
        LandGuardTagline()
    }
}

/**
 * Sets status/navigation bar icon colour for the screen that just entered.
 * Dark brand screens use light icons; the main app uses dark icons.
 */
@Composable
fun SystemBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    LaunchedEffect(darkIcons) {
        val activity = view.context.findActivity() ?: return@LaunchedEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = darkIcons
            isAppearanceLightNavigationBars = darkIcons
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ─────────────────────────────────────────────────────────────
// Startup preferences
// ─────────────────────────────────────────────────────────────

object StartupPrefs {
    private const val FILE = "LandGuardStartupPrefs"
    private const val KEY_ONBOARDING_DONE = "onboarding_complete"

    fun isOnboardingComplete(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingComplete(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ONBOARDING_DONE, true)
            .apply()
    }
}
