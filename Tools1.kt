package com.ss.client

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

// ------------------------------------------------------------------ feature panel (in-app)

@Composable
fun FeaturesScreen() {
    var q by remember { mutableStateOf("") }
    val list = FEATURES.filter { q.isBlank() || it.name.contains(q, true) || it.desc.contains(q, true) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { Nav.back() }) { Text("←", fontSize = 20.sp) }
                    SSLogo(36.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("SS BEDROCK CLIENT", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                OutlinedTextField(
                    q, { q = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(t("Search feature", "ফিচার খুঁজুন")) }
                )
                Text(
                    "Build not yet verified on a device. Statuses describe what each feature is designed to do; test before relying on any of them.",
                    color = SS.Warn, fontSize = 12.sp
                )
            }
        }
        Cat.values().forEach { cat ->
            val group = list.filter { it.cat == cat }
            if (group.isNotEmpty()) {
                item(key = "h_${cat.name}") {
                    Text("${cat.icon}  ${t(cat.en, cat.bn)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
                items(group, key = { it.id }) { FeatureCard(it) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun FeatureCard(f: Feature) {
    Column(Modifier.fillMaxWidth().background(SS.Card, RoundedCornerShape(14.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(f.cat.icon, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(f.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(f.status.label, color = f.status.color(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(f.desc, color = SS.Text2, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { Nav.screen = Screen.Open(f.id) }) { Text(if (f.tool == Tool.GUIDE) "Info" else "Settings") }
            f.hud?.let { HudSwitch(it, "Overlay") }
        }
    }
}

@Composable
fun ToolHost(id: Int) {
    val f = FEATURES.firstOrNull { it.id == id }
    if (f == null) { Text("Unknown feature"); return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { Nav.back() }) { Text("←", fontSize = 20.sp) }
            Text(f.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Text("${f.cat.icon} ${f.cat.en}  •  ${f.status.label}", color = f.status.color(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(f.desc, color = SS.Text2, modifier = Modifier.padding(vertical = 6.dp))
        when (f.tool) {
            Tool.GUIDE -> Section("INFO") { Text(f.arg) }
            Tool.COUNTER -> CounterTool(f)
            Tool.CPS -> CpsTool()
            Tool.STOPWATCH -> StopwatchTool()
            Tool.CLOCK -> ClockTool()
            Tool.REACH -> ReachTool()
            Tool.WAYPOINTS -> WaypointsTool(f)
            Tool.TEMPLATES -> TemplatesTool(f)
            Tool.HARDWARE -> HardwareTool()
            Tool.PING -> PingTool()
            Tool.COMPASS -> CompassTool()
            Tool.GYRO -> GyroTool()
            Tool.PACKS -> PacksTool(f)
            Tool.CREATOR -> CreatorTool(f)
            Tool.ASSISTANT -> AiTool()
            Tool.KEYS -> KeysTool(f)
            Tool.MUSIC -> MusicTool()
            Tool.ANDROIDSETTINGS -> AndroidSettingsTool()
            Tool.HUD -> HudTool(f)
            Tool.OVERLAY -> OverlayTool()
            Tool.LANG -> Section("LANGUAGE") {
                LanguageChooser()
                Text("Switches SS Client's own screens between English and বাংলা. Minecraft's language is set inside Minecraft.", color = SS.Text2, fontSize = 13.sp)
            }
            Tool.UISETTINGS -> Section("UI") {
                Text("Accent colour", color = SS.Text2, fontSize = 13.sp)
                AccentPicker()
                PrefSlider("SS Client text size (%)", "font_scale", 100, 80, 140)
                LogoSettings()
                Text("Only SS Client's own UI changes. Minecraft's font and UI need a resource pack.", color = SS.Text2, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------------ counters

@Composable
fun CounterTool(f: Feature) {
    val names = when (f.arg) {
        "armor4" -> listOf("Helmet", "Chestplate", "Leggings", "Boots")
        "pin" -> listOf("Pins")
        "pot" -> listOf("Potions")
        "ssc" -> listOf("Successful shots")
        "entity" -> listOf("Entities")
        else -> listOf("Combo")
    }
    names.forEach { CounterRow("cnt_${f.arg}_$it", it) }
    Text("Manual counters: values are what you enter. SS Client cannot read them from Minecraft.", color = SS.Text2, fontSize = 12.sp)
}

@Composable
fun CounterRow(key: String, label: String) {
    val v = Prefs.int(key)
    val best = Prefs.int("${key}_best")
    Section(label) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$v", fontSize = 40.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { if (v > 0) Prefs.put(key, v - 1) }) { Text("−") }
            Button(onClick = { Prefs.put(key, v + 1); if (v + 1 > best) Prefs.put("${key}_best", v + 1) }) { Text("+") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Best: $best", color = SS.Text2, modifier = Modifier.weight(1f))
            TextButton(onClick = { Prefs.put(key, 0) }) { Text("Reset") }
        }
    }
}

@Composable
fun CpsTool() {
    val taps = remember { mutableStateListOf<Long>() }
    var total by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            now = SystemClock.elapsedRealtime()
            taps.removeAll { now - it > 1000 }
        }
    }
    val cps = taps.count { now - it <= 1000 }
    Section("CPS") {
        Text("Current: $cps    Highest: $best    Total clicks: $total", fontWeight = FontWeight.Bold)
        Box(
            Modifier.fillMaxWidth().height(180.dp)
                .background(SS.Bg2, RoundedCornerShape(16.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        val t0 = SystemClock.elapsedRealtime()
                        taps.add(t0); total++
                        val c = taps.count { t0 - it <= 1000 }
                        if (c > best) best = c
                    })
                },
            contentAlignment = Alignment.Center
        ) { Text("TAP HERE AS FAST AS YOU CAN", color = SS.Text2) }
        TextButton(onClick = { taps.clear(); total = 0; best = 0 }) { Text("Reset") }
        Text("Counts taps on this pad only. It cannot see clicks inside Minecraft.", color = SS.Text2, fontSize = 12.sp)
    }
}

// ------------------------------------------------------------------ stopwatch & clock

@Composable
fun StopwatchTool() {
    var acc by remember { mutableLongStateOf(0L) }
    var startAt by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(0L) }
    val running = startAt != 0L
    LaunchedEffect(running) { while (running) { now = SystemClock.elapsedRealtime(); delay(30) } }
    val ms = acc + if (running) now - startAt else 0L
    val text = "%02d:%02d.%02d".format(ms / 60000, (ms / 1000) % 60, (ms / 10) % 100)
    Section("STOPWATCH") {
        Text(text, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (running) {
                Button(onClick = { acc += SystemClock.elapsedRealtime() - startAt; startAt = 0L }) { Text("Pause") }
            } else {
                Button(onClick = { now = SystemClock.elapsedRealtime(); startAt = now }) { Text(if (acc > 0) "Resume" else "Start") }
            }
            OutlinedButton(onClick = { acc = 0L; startAt = 0L }) { Text("Reset") }
        }
    }
}

@Composable
fun ClockTool() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(500) } }
    Section("CLOCK") {
        Text(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now)), fontSize = 56.sp, fontWeight = FontWeight.Bold)
        Text(SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(now)), color = SS.Text2)
        HudSwitch("clock", "Show clock over the game")
    }
}

// ------------------------------------------------------------------ reach & waypoints

@Composable
fun ReachTool() {
    val a = remember { mutableStateListOf("0", "0", "0") }
    val b = remember { mutableStateListOf("0", "0", "0") }
    fun d(i: Int) = (a[i].toDoubleOrNull() ?: 0.0) - (b[i].toDoubleOrNull() ?: 0.0)
    val dist = sqrt(d(0).pow(2) + d(1).pow(2) + d(2).pow(2))
    Section("POINT A (X, Y, Z)") { XyzRow(a) }
    Section("POINT B (X, Y, Z)") { XyzRow(b) }
    Section("DISTANCE") {
        Text("%.2f blocks".format(dist), fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Straight-line distance between the two points you entered. Real combat reach cannot be measured without game data.", color = SS.Text2, fontSize = 12.sp)
    }
}

@Composable
fun XyzRow(v: MutableList<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("X", "Y", "Z").forEachIndexed { i, l ->
            OutlinedTextField(v[i], { v[i] = it }, label = { Text(l) }, singleLine = true, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun WaypointsTool(f: Feature) {
    val key = "wp_${f.arg}"
    var items by remember { mutableStateOf(Prefs.list(key)) }
    var name by remember { mutableStateOf("") }
    val p = remember { mutableStateListOf("0", "0", "0") }
    val me = remember { mutableStateListOf(Prefs.str("${key}_c0", "0"), Prefs.str("${key}_c1", "0"), Prefs.str("${key}_c2", "0")) }
    fun num(s: String) = s.toDoubleOrNull() ?: 0.0
    Section("YOUR CURRENT POSITION (type it in)") {
        XyzRow(me)
        Button(onClick = { me.forEachIndexed { i, s -> Prefs.put("${key}_c$i", s) } }) { Text("Save position") }
    }
    Section("ADD COORDINATE / WAYPOINT") {
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        XyzRow(p)
        Button(onClick = {
            if (name.isNotBlank()) {
                val n = name.trim().replace("|", "/")
                items = items + "$n|${p[0]}|${p[1]}|${p[2]}"
                Prefs.putList(key, items); name = ""
            }
        }) { Text("Save") }
    }
    items.forEachIndexed { i, s ->
        val parts = s.split("|")
        if (parts.size == 4) {
            val dist = sqrt((num(parts[1]) - num(me[0])).pow(2) + (num(parts[2]) - num(me[1])).pow(2) + (num(parts[3]) - num(me[2])).pow(2))
            Section(parts[0]) {
                Text("X ${parts[1]}   Y ${parts[2]}   Z ${parts[3]}")
                Text("Distance from your saved position: %.1f blocks".format(dist), color = SS.Text2)
                Row {
                    val ctx = LocalContext.current
                    Button(onClick = { copyText(ctx, "${parts[1]} ${parts[2]} ${parts[3]}") }) { Text("Copy") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { items = items.filterIndexed { j, _ -> j != i }; Prefs.putList(key, items) }) { Text("Delete") }
                }
            }
        }
    }
    Text("Manual only: SS Client cannot read your real Minecraft position.", color = SS.Text2, fontSize = 12.sp)
}

// ------------------------------------------------------------------ templates

private val PRESETS = mapOf(
    "cmd" to listOf("/time set day", "/time set night", "/weather clear", "/weather rain", "/gamemode creative", "/gamemode survival"),
    "weather" to listOf("/weather clear", "/weather rain", "/weather thunder"),
    "time" to listOf("/time set day", "/time set noon", "/time set night"),
    "gg" to listOf("GG", "gg, well played!", "GG WP"),
    "emoji" to listOf("😀", "😎", "🔥", "👍", "💀", "❤️", "😂", "🎮")
)

@Composable
fun TemplatesTool(f: Feature) {
    val ctx = LocalContext.current
    val key = "tpl_${f.arg}"
    var items by remember {
        mutableStateOf(
            if (Prefs.has(key)) Prefs.list(key)
            else (PRESETS[f.arg] ?: emptyList()).also { if (it.isNotEmpty()) Prefs.putList(key, it) }
        )
    }
    var text by remember { mutableStateOf("") }
    fun save(l: List<String>) { items = l; Prefs.putList(key, l) }
    val hint = when (f.arg) {
        "buttons" -> "Button name::text to copy"
        "nick" -> "Nickname"
        "players" -> "Player name"
        "potions" -> "Potion effect, e.g. Speed II 3:00"
        "inventory" -> "Inventory note"
        else -> "Message / command"
    }
    Section("NEW / EDIT") {
        OutlinedTextField(text, { text = it }, label = { Text(hint) }, modifier = Modifier.fillMaxWidth())
        if (f.arg == "nick" && text.isNotBlank()) Text("Preview: $text", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Button(onClick = { if (text.isNotBlank()) { save(items + text.trim()); text = "" } }) { Text("Save") }
    }
    items.forEachIndexed { i, s ->
        val (label, payload) = if (s.contains("::")) (s.substringBefore("::") to s.substringAfter("::")) else (s to s)
        Section(label) {
            if (label != payload) Text(payload, color = SS.Text2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { copyText(ctx, payload) }) { Text("Copy") }
                OutlinedButton(onClick = { text = s; save(items.filterIndexed { j, _ -> j != i }) }) { Text("Edit") }
                TextButton(onClick = { save(items.filterIndexed { j, _ -> j != i }) }) { Text("Delete") }
            }
        }
    }
    val note = when (f.arg) {
        "cmd", "weather", "time" -> "Copy, then paste into Minecraft's chat. Commands only work if the world allows cheats/commands."
        "buttons" -> "Android only. Tapping Copy puts the text on your clipboard; iOS shortcuts are not supported by this Android app."
        else -> "Copy, then paste in Minecraft. SS Client cannot send messages or detect events inside the game."
    }
    Text(note, color = SS.Text2, fontSize = 12.sp)
}

// ------------------------------------------------------------------ hardware & ping

@Composable
fun HardwareTool() {
    val ctx = LocalContext.current
    val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    val bat = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val lvl = bat?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = bat?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val st = bat?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
    val dm = ctx.resources.displayMetrics
    val rows = listOf(
        "Model" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "RAM" to "${mi.availMem / 1048576} MB free of ${mi.totalMem / 1048576} MB",
        "Battery" to if (lvl >= 0 && scale > 0) "${lvl * 100 / scale}%" + if (charging) " (charging)" else "" else "Unavailable",
        "Screen" to "${dm.widthPixels}×${dm.heightPixels} @ ${dm.densityDpi} dpi"
    )
    Section("DEVICE") {
        rows.forEach { (k, v) ->
            Row { Text(k, color = SS.Text2, modifier = Modifier.width(90.dp)); Text(v) }
        }
    }
}

@Composable
fun PingTool() {
    val scope = rememberCoroutineScope()
    var host by remember { mutableStateOf("1.1.1.1") }
    var port by remember { mutableStateOf("443") }
    var out by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Section("TCP CONNECTION TEST") {
        OutlinedTextField(host, { host = it }, label = { Text("Host / IP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(port, { port = it }, label = { Text("TCP port") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = !busy, onClick = {
            scope.launch {
                busy = true; out = "Testing…"
                out = withContext(Dispatchers.IO) {
                    (1..4).map { n ->
                        val r = try {
                            val t0 = System.nanoTime()
                            Socket().use { it.connect(InetSocketAddress(host.trim(), port.toIntOrNull() ?: 443), 3000) }
                            "${(System.nanoTime() - t0) / 1_000_000} ms"
                        } catch (e: Exception) { "failed (${e.javaClass.simpleName})" }
                        "#$n: $r"
                    }.joinToString("\n")
                }
                busy = false
            }
        }) { Text("Test") }
        if (out.isNotEmpty()) Text(out)
        Text("This measures TCP connect time to a host and port. Minecraft Bedrock uses UDP, so this is NOT the in-game ping and is shown separately on purpose.", color = SS.Text2, fontSize = 12.sp)
    }
}
