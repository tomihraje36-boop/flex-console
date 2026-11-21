package sk.flex.console

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SystemStatus(
    val time: String = "",
    val batteryPct: Int = -1,
    val charging: Boolean = false,
    val plugType: String = "-",
    val batteryTempC: Float? = null,
    val batteryVoltageMv: Int? = null,
    val batteryHealth: String = "-",
    val wifiEnabled: Boolean = false,
    val wifiConnected: Boolean = false,
    val wifiLevel: Int = 0,
    val wifiRssi: Int = 0,
    val internet: Boolean = false,
    val btEnabled: Boolean? = null,
    val btPermission: Boolean = true,
    val btPaired: List<String> = emptyList(),
)

fun hasBtPermission(ctx: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
fun readStatus(ctx: Context): SystemStatus {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    // Battery (sticky system broadcast, no permission needed)
    val bi = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = bi?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
    val st = bi?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
    val plug = when (bi?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) {
        BatteryManager.BATTERY_PLUGGED_AC -> "Sieťový adaptér"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Bezdrôtovo"
        else -> "Nenabíja sa"
    }
    val tempRaw = bi?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
    val temp = if (tempRaw == Int.MIN_VALUE) null else tempRaw / 10f
    val voltRaw = bi?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
    val health = when (bi?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Dobrý"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Prehriata"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Opotrebovaná"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Prepätie"
        BatteryManager.BATTERY_HEALTH_COLD -> "Studená"
        else -> "Neznámy"
    }

    // Wi-Fi / internet
    val cm = ctx.getSystemService(ConnectivityManager::class.java)
    val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
    val wifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    val internet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    val wm = ctx.applicationContext.getSystemService(WifiManager::class.java)
    val wifiEnabled = wm?.isWifiEnabled == true
    @Suppress("DEPRECATION")
    val rssi = if (wifiConnected) (wm?.connectionInfo?.rssi ?: -127) else -127
    @Suppress("DEPRECATION")
    val wifiLevel = if (wifiConnected) WifiManager.calculateSignalLevel(rssi, 5) else 0

    // Bluetooth
    val perm = hasBtPermission(ctx)
    val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter
    val btEnabled = try { adapter?.isEnabled } catch (e: SecurityException) { null }
    val paired: List<String> = if (perm) {
        try { adapter?.bondedDevices?.map { it.name ?: it.address } ?: emptyList() }
        catch (e: SecurityException) { emptyList() }
    } else emptyList()

    return SystemStatus(
        time = time,
        batteryPct = pct,
        charging = charging,
        plugType = plug,
        batteryTempC = temp,
        batteryVoltageMv = if (voltRaw > 0) voltRaw else null,
        batteryHealth = health,
        wifiEnabled = wifiEnabled,
        wifiConnected = wifiConnected,
        wifiLevel = wifiLevel,
        wifiRssi = rssi,
        internet = internet,
        btEnabled = btEnabled,
        btPermission = perm,
        btPaired = paired,
    )
}

/** Live system status, refreshed every 2 s. */
@Composable
fun rememberSystemStatus(): State<SystemStatus> {
    val ctx = LocalContext.current
    return produceState(SystemStatus()) {
        while (true) {
            value = readStatus(ctx)
            delay(2000)
        }
    }
}

fun toast(ctx: Context, msg: String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}
