package sk.flex.console.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import sk.flex.console.Compat
import sk.flex.console.Game
import sk.flex.console.GameRepository
import sk.flex.console.PlayMode
import sk.flex.console.SystemStatus
import sk.flex.console.toast

private data class LaunchInfo(val headline: String, val title: String, val action: () -> Unit)

private const val TXT_LOCAL = "Hráš lokálne"
private const val TXT_CLOUD = "Hráš cez Xbox Cloud Gaming"

@Composable
fun HomeScreen(
    repo: GameRepository,
    status: SystemStatus,
    resumeTick: Int,
    onOpenSettings: () -> Unit,
) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var dataTick by remember { mutableIntStateOf(0) }
    val games = remember(resumeTick, dataTick) { repo.allGames() }
    val library = games.filter { it.inLibrary }
    val store = games.filter { it.fromCatalog && !it.inLibrary }
    val apps = remember(resumeTick) { repo.launchableApps() }

    var dialogGame by remember { mutableStateOf<Game?>(null) }
    var launching by remember { mutableStateOf<LaunchInfo?>(null) }

    fun start(g: Game, mode: PlayMode) {
        if (mode == PlayMode.CLOUD && !status.internet) {
            toast(ctx, "Bez internetu – cloud hru teraz nespustíš")
            return
        }
        launching = when (mode) {
            PlayMode.LOCAL -> LaunchInfo(TXT_LOCAL, g.title) {
                val pkg = g.packageName
                if (pkg == null || !repo.launchLocal(pkg)) toast(ctx, "Hru sa nepodarilo spustiť")
            }
            PlayMode.CLOUD -> LaunchInfo(TXT_CLOUD, g.title) {
                if (!repo.launchCloud(g.cloudUrl)) toast(ctx, "Nemáš prehliadač na otvorenie xbox.com/play")
            }
        }
    }

    fun getLocal(g: Game, rememberChoice: Boolean) {
        val pkg = g.packageName ?: return
        if (rememberChoice) repo.setPreferred(g.id, PlayMode.LOCAL)
        repo.openStore(pkg)
        toast(ctx, "Po nainštalovaní sa ${g.title} objaví v Knižnici")
    }

    fun addCloudAndPlay(g: Game, rememberChoice: Boolean) {
        repo.addCloud(g.id)
        if (rememberChoice) repo.setPreferred(g.id, PlayMode.CLOUD)
        dataTick++
        start(g, PlayMode.CLOUD)
    }

    // Library: one option -> start right away; both -> use saved choice or ask.
    fun onLibraryClick(g: Game) {
        val canLocal = g.installed
        val canCloud = g.cloudSupported
        when {
            canLocal && canCloud -> {
                val pref = g.preferred
                if (pref != null) start(g, pref) else dialogGame = g
            }
            canLocal -> start(g, PlayMode.LOCAL)
            canCloud -> start(g, PlayMode.CLOUD)
            else -> dialogGame = g
        }
    }

    // Store: phone supports both -> ask; only one -> do it straight away.
    fun onStoreClick(g: Game) {
        val local = repo.localCompat(g).ok
        val cloud = g.cloudSupported
        when {
            local && cloud -> dialogGame = g
            local -> {
                toast(ctx, "Telefón podporuje len lokálnu hru – otváram Google Play")
                getLocal(g, rememberChoice = true)
            }
            cloud -> addCloudAndPlay(g, rememberChoice = true)
            else -> dialogGame = g
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0C1A11), Bg))),
    ) {
        Column(Modifier.fillMaxSize()) {
            TopStatusBar(status, onOpenSettings)
            TabRowFlex(tab, listOf("Knižnica", "Obchod", "Aplikácie")) { tab = it }
            when (tab) {
                0 -> GameGrid(
                    games = library,
                    repo = repo,
                    emptyText = "Knižnica je prázdna – choď do Obchodu a pridaj si hry.",
                    tags = { g ->
                        buildList {
                            if (g.installed) add("LOKÁLNE")
                            if (g.cloudSupported) add("CLOUD")
                        }
                    },
                    onClick = { onLibraryClick(it) },
                    onLongClick = { dialogGame = it },
                )
                1 -> GameGrid(
                    games = store,
                    repo = repo,
                    emptyText = "Všetky hry z obchodu už máš v Knižnici.",
                    tags = { g ->
                        buildList {
                            if (repo.localCompat(g).ok) add("LOKÁLNE")
                            if (g.cloudSupported) add("CLOUD")
                            if (isEmpty()) add("NEPODPOROVANÉ")
                        }
                    },
                    onClick = { onStoreClick(it) },
                    onLongClick = { dialogGame = it },
                )
                else -> AppGrid(apps, repo)
            }
        }

        dialogGame?.let { g ->
            GameDialog(
                g = g,
                repo = repo,
                internet = status.internet,
                onDismiss = { dialogGame = null },
                onPlayLocal = { rememberChoice ->
                    dialogGame = null
                    if (g.installed) {
                        if (rememberChoice) repo.setPreferred(g.id, PlayMode.LOCAL)
                        start(g, PlayMode.LOCAL)
                    } else {
                        getLocal(g, rememberChoice)
                    }
                },
                onPlayCloud = { rememberChoice ->
                    dialogGame = null
                    addCloudAndPlay(g, rememberChoice)
                },
                onChanged = {
                    dataTick++
                    dialogGame = repo.allGames().firstOrNull { it.id == g.id }
                },
            )
        }

        launching?.let { info ->
            LaunchedEffect(info) {
                toast(ctx, "${info.headline}: ${info.title}")
                delay(1300)
                info.action()
                launching = null
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Accent)
                    Spacer(Modifier.height(20.dp))
                    Text(info.headline, color = Accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(info.title, color = Color.White, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun GameGrid(
    games: List<Game>,
    repo: GameRepository,
    emptyText: String,
    tags: (Game) -> List<String>,
    onClick: (Game) -> Unit,
    onLongClick: (Game) -> Unit,
) {
    if (games.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = Color.Gray, fontSize = 18.sp)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(200.dp),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(games, key = { it.id }) { g ->
            FocusTile(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                color = Color(g.color),
                onClick = { onClick(g) },
                onLongClick = { onLongClick(g) },
            ) {
                val icon = if (g.installed) repo.icon(g.packageName) else null
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .align(Alignment.TopStart),
                    )
                } else {
                    Icon(
                        Icons.Filled.SportsEsports, contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.TopStart),
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tags(g).forEach { TagLabel(it) }
                }
                Text(
                    text = g.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 2,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }
    }
}

@Composable
private fun AppGrid(apps: List<sk.flex.console.AppEntry>, repo: GameRepository) {
    val ctx = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(apps, key = { it.packageName }) { a ->
            FocusTile(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                color = Panel,
                onClick = { if (!repo.launchLocal(a.packageName)) toast(ctx, "Aplikácia sa nedá spustiť") },
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val icon = repo.icon(a.packageName)
                    if (icon != null) {
                        Image(icon, null, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                    } else {
                        Icon(Icons.Filled.Apps, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(a.label, color = Color.White, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun GameDialog(
    g: Game,
    repo: GameRepository,
    internet: Boolean,
    onDismiss: () -> Unit,
    onPlayLocal: (Boolean) -> Unit,
    onPlayCloud: (Boolean) -> Unit,
    onChanged: () -> Unit,
) {
    val local = repo.localCompat(g)
    val cloud = repo.cloudCompat(g, internet)
    var rememberChoice by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text(g.title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CompatRow(Icons.Filled.PhoneAndroid, "Lokálne na telefóne", local)
                CompatRow(Icons.Filled.Cloud, "Xbox Cloud Gaming", cloud)

                if (local.ok && cloud.ok) {
                    Text("Telefón podporuje obe možnosti. Ako chceš hrať?", color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = rememberChoice, onCheckedChange = { rememberChoice = it })
                        Text("Zapamätať voľbu", color = Color.White)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (local.ok) {
                        Button(onClick = { onPlayLocal(rememberChoice) }) {
                            Icon(Icons.Filled.PhoneAndroid, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (g.installed) "Hrať lokálne" else "Stiahnuť (lokálne)")
                        }
                    }
                    if (cloud.ok) {
                        Button(onClick = { onPlayCloud(rememberChoice) }) {
                            Icon(Icons.Filled.Cloud, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Hrať cez cloud")
                        }
                    }
                }
                if (!local.ok && !cloud.ok) {
                    Text("Túto hru teraz nemôžeš spustiť.", color = Color(0xFFFF8A80))
                }

                HorizontalDivider(color = Color(0x22FFFFFF))
                if (g.fromCatalog) {
                    Text("Upraviť podporu hry", color = Muted, fontSize = 13.sp)
                    SwitchRow("Má verziu pre Android", g.localSupported) {
                        repo.setSupport(g.id, local = it, cloud = g.cloudSupported); onChanged()
                    }
                    SwitchRow("Je v Xbox Cloud Gaming", g.cloudSupported) {
                        repo.setSupport(g.id, local = g.localSupported, cloud = it); onChanged()
                    }
                    if (g.preferred != null) {
                        TextButton(onClick = { repo.setPreferred(g.id, null); onChanged() }) {
                            Text("Zabudnúť zapamätanú voľbu (${if (g.preferred == PlayMode.LOCAL) "lokálne" else "cloud"})")
                        }
                    }
                    if (g.addedCloud) {
                        TextButton(onClick = { repo.removeCloud(g.id); onChanged() }) {
                            Text("Odstrániť cloud hru z Knižnice")
                        }
                    }
                } else {
                    Text("Hra bola nájdená v telefóne automaticky.", color = Muted, fontSize = 13.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zavrieť") } },
    )
}

@Composable
private fun CompatRow(icon: ImageVector, label: String, c: Compat) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (c.ok) Accent else Color(0xFFFF8A80))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text((if (c.ok) "✓ " else "✗ ") + c.reason, color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
