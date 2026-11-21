package sk.flex.console.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.flex.console.SystemStatus

/** Top bar: logo, clock, Wi-Fi, Bluetooth, battery, settings. */
@Composable
fun TopStatusBar(status: SystemStatus, onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("FLEX", color = Accent, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 4.sp)
        Spacer(Modifier.width(16.dp))
        Text(status.time, color = Color.White, fontSize = 18.sp)
        Spacer(Modifier.weight(1f))
        StatusIcons(status)
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Nastavenia", tint = Color.White)
        }
    }
}

@Composable
fun StatusIcons(status: SystemStatus) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (status.wifiConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
            contentDescription = "Wi-Fi",
            tint = if (status.wifiConnected) Color.White else Color.Gray,
            modifier = Modifier.size(22.dp),
        )
        if (status.wifiConnected) {
            Spacer(Modifier.width(4.dp))
            SignalBars(status.wifiLevel)
        }
        Spacer(Modifier.width(16.dp))
        Icon(
            imageVector = if (status.btEnabled == true) Icons.Filled.Bluetooth else Icons.Filled.BluetoothDisabled,
            contentDescription = "Bluetooth",
            tint = if (status.btEnabled == true) Color.White else Color.Gray,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        BatteryIndicator(status.batteryPct, status.charging)
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (status.batteryPct >= 0) "${status.batteryPct}%" else "--",
            color = Color.White,
            fontSize = 16.sp,
        )
    }
}

@Composable
fun SignalBars(level: Int) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..4) {
            Box(
                Modifier
                    .width(3.dp)
                    .height((4 + i * 3).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i <= level) Color.White else Color(0x44FFFFFF)),
            )
        }
    }
}

@Composable
fun BatteryIndicator(pct: Int, charging: Boolean) {
    val frac = pct.coerceIn(0, 100) / 100f
    val fill = when {
        charging -> Accent
        pct in 0..15 -> Color(0xFFFF5252)
        pct in 16..30 -> Color(0xFFFFC107)
        else -> Color.White
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(30.dp)
                .height(15.dp)
                .border(1.5.dp, Color.White, RoundedCornerShape(3.dp))
                .padding(2.dp),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(frac)
                    .background(fill, RoundedCornerShape(1.dp)),
            )
            if (charging) {
                Icon(
                    Icons.Filled.FlashOn, contentDescription = "Nabíja sa", tint = Color.Black,
                    modifier = Modifier.align(Alignment.Center).size(12.dp),
                )
            }
        }
        Box(Modifier.width(2.dp).height(6.dp).background(Color.White))
    }
}

/** Console-style tile: grows and gets a white frame when focused (D-pad / gamepad). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusTile(
    modifier: Modifier = Modifier,
    color: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "tileScale")
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.55f), Color(0xFF0B0F0C))))
            .then(if (focused) Modifier.border(3.dp, Color.White, shape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        content = content,
    )
}

@Composable
fun TabRowFlex(selected: Int, titles: List<String>, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        titles.forEachIndexed { i, t ->
            var focused by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { onSelect(i) }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = t,
                    color = if (i == selected || focused) Color.White else Color.Gray,
                    fontSize = 20.sp,
                    fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal,
                )
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .height(3.dp)
                        .width(if (i == selected) 36.dp else 0.dp)
                        .background(Accent),
                )
            }
        }
    }
}

@Composable
fun TagLabel(text: String, color: Color = Color(0xCC000000)) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
