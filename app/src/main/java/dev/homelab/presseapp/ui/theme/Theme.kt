package dev.homelab.presseapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AccentOrange = Color(0xFFEA4100)

private val LightColors = lightColorScheme(primary = AccentOrange)
private val DarkColors = darkColorScheme(primary = AccentOrange)

/**
 * Material 3 mit dynamischer Farbgebung (Android 12+, Material You) als
 * Standard - faellt auf ein festes, an Muenzingers Akzentfarbe angelehntes
 * Farbschema zurueck, wenn dynamische Farben nicht verfuegbar sind
 * (aeltere Geraete unter minSdk 26 bis 31).
 */
@Composable
fun PresseAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
