package sk.flex.console.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sk.flex.console.GameRepository
import sk.flex.console.rememberSystemStatus

enum class Screen { SPLASH, HOME, SETTINGS }

@Composable
fun FlexApp(repo: GameRepository, resumeTick: Int) {
    var screen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
    val status by rememberSystemStatus()

    // Launcher: Back never leaves FLEX, it only returns from Settings.
    BackHandler(enabled = screen != Screen.SPLASH) {
        if (screen == Screen.SETTINGS) screen = Screen.HOME
    }

    when (screen) {
        Screen.SPLASH -> SplashScreen(onDone = { screen = Screen.HOME })
        Screen.HOME -> HomeScreen(
            repo = repo,
            status = status,
            resumeTick = resumeTick,
            onOpenSettings = { screen = Screen.SETTINGS },
        )
        Screen.SETTINGS -> SettingsScreen(
            repo = repo,
            status = status,
            onBack = { screen = Screen.HOME },
        )
    }
}

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val fadeIn = remember { Animatable(0f) }
    val zoom = remember { Animatable(0.85f) }
    val progress = remember { Animatable(0f) }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "glow",
    )

    LaunchedEffect(Unit) {
        launch { fadeIn.animateTo(1f, tween(800)) }
        launch { zoom.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
        progress.animateTo(1f, tween(2600, easing = LinearEasing))
        delay(250)
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF0E2A16), Color.Black), radius = 1400f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = fadeIn.value
                scaleX = zoom.value
                scaleY = zoom.value
            },
        ) {
            Text(
                text = "FLEX",
                color = Accent,
                fontSize = 110.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 18.sp,
                style = TextStyle(shadow = Shadow(Accent.copy(alpha = glow), Offset.Zero, 48f)),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "SOFTWARE",
                color = Color(0xFFB0BEC5),
                fontSize = 20.sp,
                letterSpacing = 12.sp,
                fontWeight = FontWeight.Light,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .width(220.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x33FFFFFF)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.value)
                    .background(Accent),
            )
        }
    }
}
