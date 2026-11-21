package sk.flex.console.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.InputDevice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.flex.console.GameRepository
import sk.flex.console.SystemStatus
import sk.flex.console.toast
import kotlin.math.roundToInt

enum class Section(val title: String, val icon: ImageVector) {
    WIFI("Wi‑Fi", Icons.Filled.Wifi),
    BT("Bluetooth", Icons.Filled.Bluetooth),
    BATTERY("Batéria", Icons.Filled.BatteryFull),
    SOUND("Zvuk", Icons.Filled.VolumeUp),
    PADS("Ovládače", Icons.Filled.SportsEsports),
    DISPLAY("Displej", Icons.Filled.BrightnessMedium),
    SYSTEM("Systém", Icons.Filled.Info),
}

private fun openSystem(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        toast(ctx, "Táto voľba nie je na telefóne dostupná")
    }
}

@Composable
fun SettingsScreen(repo: GameRepository, status: SystemStatus, onBack: () -> Unit) {
    var section by rememberSaveable { mutableStateOf(Section.WIFI) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Späť", tint = Color.White)
            }
            Text("Nastavenia", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(status.time, color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.width(16.dp))
            StatusIcons(status)
            Spacer(Modifier.width(12.dp))
        }
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(230.dp)
                    .fillMaxHeight()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Section.entries.forEach { s ->
                    NavItem(s, s == section) { section = s }
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = 12.dp, end = 16.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Panel)
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (section) {
                        Section.WIFI -> WifiSection(status)
                        Section.BT -> BluetoothSection(status)
                        Section.BATTERY -> BatterySection(status)
                        Section.SOUND -> SoundSection()
                        Section.PADS -> PadsSection(status)
                        Section.DISPLAY -> DisplaySection()
                        Section.SYSTEM -> SystemSection(repo)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(s: Section, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Accent.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(s.icon, null, tint = if (selected) Accent else Color.White)
        Spacer(Modifier.width(12.dp))
        Text(
            s.title,
            color = if (selected) Accent else Color.White,
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun Title(t: String) {
    Text(t, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = Muted, fontSize = 16.sp, modifier = Modifier.width(200.dp))
        Text(value, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
private fun Note(t: String) {
    Text(t, color = Muted, fontSize = 13.sp)
}

@Composable
private fun WifiSection(status: SystemStatus) {
    val ctx = LocalContext.current
    Title("Wi‑Fi")
    InfoRow("Wi‑Fi", if (status.wifiEnabled) "Zapnutá" else "Vypnutá")
    InfoRow("Pripojenie", if (status.wifiConnected) "Pripojené" else "Nepripojené")
    if (status.wifiConnected) InfoRow("Signál", "${status.wifiLevel}/4  (${status.wifiRssi} dBm)")
    InfoRow("Internet", if (status.internet) "Dostupný" else "Nedostupný")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = {
            val i = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_WIFI)
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }
            openSystem(ctx, i)
        }) { Text("Zapnúť / vybrať sieť") }
        OutlinedButton(onClick = { openSystem(ctx, Intent(Settings.ACTION_WIFI_SETTINGS)) }) {
            Text("Všetky nastavenia Wi‑Fi")
        }
    }
    Note("Android 10+ nedovolí aplikácii priamo zapnúť Wi‑Fi – otvorí sa systémový panel.")
}

@Composable
private fun BluetoothSection(status: SystemStatus) {
    val ctx = LocalContext.current
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) toast(ctx, "Bez povolenia neuvidíš spárované zariadenia")
    }
    Title("Bluetooth")
    InfoRow(
        "Bluetooth",
        when (status.btEnabled) {
            true -> "Zapnutý"
            false -> "Vypnutý"
            null -> "Neznámy stav"
        },
    )
    if (!status.btPermission) {
        Note("FLEX nemá povolenie na Bluetooth zariadenia.")
        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }) { Text("Povoliť prístup k Bluetooth") }
    } else {
        Text("Spárované zariadenia", color = Color.White, fontWeight = FontWeight.SemiBold)
        if (status.btPaired.isEmpty()) Note("Žiadne") else status.btPaired.forEach { Text("• $it", color = Color.White) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (status.btEnabled == false) {
            Button(onClick = { openSystem(ctx, Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }) {
                Text("Zapnúť Bluetooth")
            }
        }
        OutlinedButton(onClick = { openSystem(ctx, Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }) {
            Text("Spárovať zariadenie")
        }
    }
}

@Composable
private fun BatterySection(status: SystemStatus) {
    val ctx = LocalContext.current
    Title("Batéria")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (status.batteryPct >= 0) "${status.batteryPct} %" else "--",
            color = if (status.charging) Accent else Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.width(16.dp))
        Box(Modifier.size(width = 90.dp, height = 40.dp), contentAlignment = Alignment.Center) {
            BatteryIndicator(status.batteryPct, status.charging)
        }
    }
    InfoRow("Stav", if (status.charging) "Nabíja sa" else "Vybíja sa")
    InfoRow("Zdroj", status.plugType)
    status.batteryTempC?.let { InfoRow("Teplota", "%.1f °C".format(it)) }
    status.batteryVoltageMv?.let { InfoRow("Napätie", "%.2f V".format(it / 1000f)) }
    InfoRow("Zdravie", status.batteryHealth)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { openSystem(ctx, Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) }) {
            Text("Šetrič batérie")
        }
        OutlinedButton(onClick = { openSystem(ctx, Intent(Intent.ACTION_POWER_USAGE_SUMMARY)) }) {
            Text("Využitie batérie")
        }
    }
}

@Composable
private fun SoundSection() {
    val ctx = LocalContext.current
    val am = remember { ctx.getSystemService(AudioManager::class.java) }
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    var vol by remember { mutableFloatStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    Title("Zvuk")
    Text("Hlasitosť hier a médií: ${vol.roundToInt()}/$max", color = Color.White)
    Slider(
        value = vol,
        onValueChange = {
            vol = it
            try {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, it.roundToInt(), 0)
            } catch (e: SecurityException) {
                toast(ctx, "Hlasitosť teraz nejde zmeniť (režim Nerušiť)")
            }
        },
        valueRange = 0f..max.toFloat(),
        steps = (max - 1).coerceAtLeast(0),
    )
    OutlinedButton(onClick = { openSystem(ctx, Intent(Settings.ACTION_SOUND_SETTINGS)) }) {
        Text("Nastavenia zvuku")
    }
}

@Composable
private fun PadsSection(status: SystemStatus) {
    val ctx = LocalContext.current
    Title("Ovládače")
    // Re-read on every status refresh (every 2 s), so a newly connected pad appears.
    val pads = remember(status) {
        InputDevice.getDeviceIds().toList().mapNotNull { id ->
            val d = InputDevice.getDevice(id) ?: return@mapNotNull null
            val s = d.sources
            val isPad = (s and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (s and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            if (isPad) d.name else null
        }.distinct()
    }
    if (pads.isEmpty()) {
        Note("Žiadny ovládač nie je pripojený. Spáruj ho cez Bluetooth alebo pripoj cez USB‑C.")
    } else {
        pads.forEach { Text("🎮 $it", color = Color.White, fontSize = 16.sp) }
    }
    Button(onClick = { openSystem(ctx, Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }) {
        Text("Spárovať ovládač")
    }
    Note("FLEX sa dá celý ovládať šípkami/D‑padom ovládača, A = potvrdiť, B = späť.")
}

@Composable
private fun DisplaySection() {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    var bright by remember {
        mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.8f)
    }
    Title("Displej")
    Text("Jas v rozhraní FLEX: ${(bright * 100).roundToInt()} %", color = Color.White)
    Slider(
        value = bright,
        onValueChange = {
            bright = it
            activity?.window?.let { w ->
                val lp = w.attributes
                lp.screenBrightness = it
                w.attributes = lp
            }
        },
        valueRange = 0.05f..1f,
    )
    OutlinedButton(onClick = { openSystem(ctx, Intent(Settings.ACTION_DISPLAY_SETTINGS)) }) {
        Text("Systémové nastavenia displeja")
    }
}

@Composable
private fun SystemSection(repo: GameRepository) {
    val ctx = LocalContext.current
    val version = remember {
        try {
            @Suppress("DEPRECATION")
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
    }
    Title("Systém")
    InfoRow("FLEX software", "verzia $version")
    InfoRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    InfoRow("Zariadenie", "${Build.MANUFACTURER} ${Build.MODEL}")
    InfoRow("RAM", "%.1f GB".format(repo.totalRamGb()))
    Spacer(Modifier.height(6.dp))
    Button(onClick = { openSystem(ctx, Intent(Settings.ACTION_HOME_SETTINGS)) }) {
        Text("Nastaviť FLEX ako domovskú obrazovku")
    }
    Note("Keď je FLEX domovská obrazovka, po zapnutí telefónu sa hneď ukáže logo FLEX a konzola.")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { openSystem(ctx, Intent(Settings.ACTION_SETTINGS)) }) {
            Text("Nastavenia Androidu")
        }
        OutlinedButton(onClick = {
            repo.resetAll()
            toast(ctx, "Knižnica a voľby boli resetované")
        }) { Text("Resetovať knižnicu") }
    }
}
