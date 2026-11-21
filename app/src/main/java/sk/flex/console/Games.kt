package sk.flex.console

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import org.json.JSONArray

enum class PlayMode { LOCAL, CLOUD }

data class Game(
    val id: String,
    val title: String,
    val packageName: String?,
    val localSupported: Boolean,
    val cloudSupported: Boolean,
    val cloudUrl: String,
    val color: Long,
    val minAndroid: Int?,
    val minRamGb: Int?,
    val fromCatalog: Boolean,
    val installed: Boolean,
    val addedCloud: Boolean,
    val preferred: PlayMode?,
) {
    /** In "Knižnica" = installed on the phone, or added as a cloud game. */
    val inLibrary: Boolean get() = installed || (addedCloud && cloudSupported)
}

data class AppEntry(val label: String, val packageName: String)

data class Compat(val ok: Boolean, val reason: String)

class GameRepository(private val ctx: Context) {

    private val prefs = ctx.getSharedPreferences("flex", Context.MODE_PRIVATE)
    private val pm = ctx.packageManager
    private val iconCache = HashMap<String, ImageBitmap>()
    private val catalog: List<Game> by lazy { loadCatalog() }

    private fun loadCatalog(): List<Game> {
        val text = ctx.assets.open("games.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Game(
                id = o.getString("id"),
                title = o.getString("title"),
                packageName = o.optString("package", "").ifBlank { null },
                localSupported = o.optBoolean("local", false),
                cloudSupported = o.optBoolean("cloud", false),
                cloudUrl = o.optString("cloudUrl", "").ifBlank { DEFAULT_CLOUD_URL },
                color = java.lang.Long.parseLong(o.optString("color", "FF2E7D32"), 16),
                minAndroid = if (o.has("minAndroid")) o.getInt("minAndroid") else null,
                minRamGb = if (o.has("minRamGb")) o.getInt("minRamGb") else null,
                fromCatalog = true,
                installed = false,
                addedCloud = false,
                preferred = null,
            )
        }
    }

    fun isInstalled(pkg: String?): Boolean =
        pkg != null && pm.getLaunchIntentForPackage(pkg) != null

    fun allGames(): List<Game> {
        val cloudLib = prefs.getStringSet(KEY_CLOUD_LIB, emptySet()) ?: emptySet()
        val fromCatalog = catalog.map { g ->
            g.copy(
                localSupported = prefs.getBoolean("local_${g.id}", g.localSupported),
                cloudSupported = prefs.getBoolean("cloud_${g.id}", g.cloudSupported),
                installed = isInstalled(g.packageName),
                addedCloud = cloudLib.contains(g.id),
                preferred = prefs.getString("mode_${g.id}", null)
                    ?.let { runCatching { PlayMode.valueOf(it) }.getOrNull() },
            )
        }
        val known = fromCatalog.mapNotNull { it.packageName }.toSet()
        // Any other installed app that Android marks as a game shows up automatically.
        val detected = launchableApps()
            .filter { it.packageName !in known && isGameApp(it.packageName) }
            .map { a ->
                Game(
                    id = "pkg:${a.packageName}", title = a.label, packageName = a.packageName,
                    localSupported = true, cloudSupported = false, cloudUrl = DEFAULT_CLOUD_URL,
                    color = 0xFF37474F, minAndroid = null, minRamGb = null,
                    fromCatalog = false, installed = true, addedCloud = false,
                    preferred = PlayMode.LOCAL,
                )
            }
        return fromCatalog + detected
    }

    private fun isGameApp(pkg: String): Boolean = try {
        val ai = pm.getApplicationInfo(pkg, 0)
        @Suppress("DEPRECATION")
        val legacyFlag = (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0
        ai.category == ApplicationInfo.CATEGORY_GAME || legacyFlag
    } catch (e: Exception) {
        false
    }

    fun launchableApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val list = pm.queryIntentActivities(intent, 0)
        return list
            .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.packageName != ctx.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun icon(pkg: String?): ImageBitmap? {
        if (pkg == null) return null
        iconCache[pkg]?.let { return it }
        return try {
            val bmp = pm.getApplicationIcon(pkg).toBitmap(128, 128).asImageBitmap()
            iconCache[pkg] = bmp
            bmp
        } catch (e: Exception) {
            null
        }
    }

    // ---- compatibility -------------------------------------------------------------

    fun totalRamGb(): Double {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.totalMem / 1_073_741_824.0
    }

    fun localCompat(g: Game): Compat {
        if (g.installed) return Compat(true, "Nainštalované v telefóne")
        if (!g.localSupported || g.packageName == null) {
            return Compat(false, "Hra nemá verziu pre Android")
        }
        g.minAndroid?.let {
            if (Build.VERSION.SDK_INT < it) {
                return Compat(false, "Vyžaduje Android API $it, telefón má ${Build.VERSION.SDK_INT}")
            }
        }
        g.minRamGb?.let {
            val ram = totalRamGb()
            if (ram < it) {
                return Compat(false, "Vyžaduje $it GB RAM, telefón má ${"%.1f".format(ram)} GB")
            }
        }
        return Compat(true, "Podporované – stiahne sa z Google Play")
    }

    fun cloudCompat(g: Game, internet: Boolean): Compat = when {
        !g.cloudSupported -> Compat(false, "Hra nie je v Xbox Cloud Gaming")
        !internet -> Compat(false, "Bez internetu – cloud teraz nejde")
        else -> Compat(true, "Stream cez xbox.com/play (vyžaduje Game Pass)")
    }

    // ---- user choices --------------------------------------------------------------

    fun setPreferred(id: String, mode: PlayMode?) {
        prefs.edit().apply {
            if (mode == null) remove("mode_$id") else putString("mode_$id", mode.name)
        }.apply()
    }

    fun addCloud(id: String) {
        val s = HashSet(prefs.getStringSet(KEY_CLOUD_LIB, emptySet()) ?: emptySet())
        s.add(id)
        prefs.edit().putStringSet(KEY_CLOUD_LIB, s).apply()
    }

    fun removeCloud(id: String) {
        val s = HashSet(prefs.getStringSet(KEY_CLOUD_LIB, emptySet()) ?: emptySet())
        s.remove(id)
        prefs.edit().putStringSet(KEY_CLOUD_LIB, s).apply()
    }

    fun setSupport(id: String, local: Boolean, cloud: Boolean) {
        prefs.edit().putBoolean("local_$id", local).putBoolean("cloud_$id", cloud).apply()
    }

    fun resetAll() {
        prefs.edit().clear().apply()
    }

    // ---- launching -----------------------------------------------------------------

    fun launchLocal(pkg: String): Boolean {
        val i = pm.getLaunchIntentForPackage(pkg) ?: return false
        return open(i)
    }

    fun launchCloud(url: String): Boolean = open(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    fun openStore(pkg: String) {
        if (!open(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))) {
            if (!open(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))) {
                toast(ctx, "Google Play sa nepodarilo otvoriť")
            }
        }
    }

    fun open(i: Intent): Boolean = try {
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    companion object {
        const val DEFAULT_CLOUD_URL = "https://www.xbox.com/play"
        private const val KEY_CLOUD_LIB = "cloudLib"
    }
}
