package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = MintGreen,
    secondary = NavySecondary,
    tertiary = AccentGold,
    background = DeepDarkBlue,
    surface = NavyPrimary,
    onPrimary = DeepDarkBlue,
    onSecondary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NavySecondary,
    secondary = MintGreen,
    tertiary = AccentGold,
    background = SlateBackground,
    surface = LightSurface,
    onPrimary = TextLight,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark
  )

@Composable
fun PijatKuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep premium handcraft colors as primary default override
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
