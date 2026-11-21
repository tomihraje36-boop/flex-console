package sk.flex.console.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF39FF7A)
val Bg = Color(0xFF070B09)
val Panel = Color(0xFF121A15)
val Muted = Color(0xFF90A4AE)

@Composable
fun FlexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color.Black,
            secondary = Accent,
            background = Bg,
            onBackground = Color.White,
            surface = Panel,
            onSurface = Color.White,
        ),
        content = content,
    )
}
