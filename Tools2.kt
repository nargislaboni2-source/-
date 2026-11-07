package com.ss.client

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// ------------------------------------------------------------------ overlay HUD controls

@Composable
fun HudSwitch(name: String, label: String) {
    val ctx = LocalContext.current
    val on = Prefs.bool("hud_$name")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label)
        Switch(checked = on, onCheckedChange = { c ->
            if (c && !Settings.canDrawOverlays(ctx)) {
                toast(ctx, "Grant the overlay permission first")
                ctx.startActivity(Launcher.overlayIntent(ctx))
            } else {
                Prefs.put("hud_$name", c)
                if (c) OverlayService.start(ctx) else OverlayService.refresh(ctx)
            }
        })
    }
}

@Composable
fun HudTool(f: Feature) {
    val ctx = LocalContext.current
    Section("OVERLAY") {
        HudSwitch(f.arg, "Enable")
        OverlayPermissionRow()
        Text("Needs the 'Display over other apps' permission. Enabling starts the SS overlay service (it also shows the floating SS logo).", color = SS.Text2, fontSize = 12.sp)
    }
    if (f.arg == "crosshair") {
        Section("CROSSHAIR") {
            val refresh = { OverlayService.refresh(ctx) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Plus", "Dot", "Ring").forEachIndexed { i, n -> Chip(n, Prefs.int("ch_shape", 0) == i) { Prefs.put("ch_shape", i); refresh() } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CH_COLORS.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(34.dp).background(Color(c), CircleShape)
                            .border(if (Prefs.int("ch_color", 0) == i) 3.dp else 1.dp, SS.Text2, CircleShape)
                            .pointerInput(i) { detectTapGestures { Prefs.put("ch_color", i); refresh() } }
                    )
                }
            }
            PrefSlider("Size (dp)", "ch_size", 28, 8, 80, refresh)
            PrefSlider("Opacity (%)", "ch_opacity", 90, 10, 100, refresh)
            PrefSlider("Position X offset (dp)", "ch_x", 0, -200, 200, refresh)
            PrefSlider("Position Y offset (dp)", "ch_y", 0, -200, 200, refresh)
            Text("Tip: the crosshair is a plain overlay. It does not read Minecraft's aim.", color = SS.Text2, fontSize = 12.sp)
        }
    }
}

@Composable
fun OverlayTool() {
    val ctx = LocalContext.current
    var running by remember { mutableStateOf(OverlayService.running) }
    Section("FLOATING OVERLAY SERVICE") {
        OverlayPermissionRow()
        Text(if (running) "Service: running" else "Service: stopped", color = if (running) SS.Ok else SS.Text2, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (Settings.canDrawOverlays(ctx)) { OverlayService.start(ctx); running = true }
                else ctx.startActivity(Launcher.overlayIntent(ctx))
            }) { Text("Start") }
            OutlinedButton(onClick = { OverlayService.stop(ctx); running = false }) { Text("Stop") }
        }
        Text("Uses an Android foreground service with a visible notification. Android battery optimisation or vendor task killers may still stop it.", color = SS.Text2, fontSize = 12.sp)
    }
}

@Composable
fun AndroidSettingsTool() {
    val ctx = LocalContext.current
    Section("MINECRAFT ON ANDROID") {
        Button(onClick = {
            runCatching {
                ctx.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${Launcher.MC}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure { toast(ctx, "Could not open Minecraft's app settings") }
        }) { Text("Open Minecraft app info") }
        OutlinedButton(onClick = { if (!Launcher.launch(ctx)) toast(ctx, "Minecraft is not installed") }) { Text("Open Minecraft") }
        Text(if (Launcher.isInstalled(ctx)) "Minecraft Bedrock: installed" else "Minecraft Bedrock: not found", color = if (Launcher.isInstalled(ctx)) SS.Ok else SS.Err)
        Text("This only opens Android's own settings pages. SS Client cannot change Minecraft's protected in-game settings.", color = SS.Text2, fontSize = 12.sp)
    }
}

// ------------------------------------------------------------------ sensors

@Composable
fun CompassTool() {
    val ctx = LocalContext.current
    var deg by remember { mutableFloatStateOf(0f) }
    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    DisposableEffect(sensor) {
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val r = FloatArray(9); val o = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(r, e.values)
                SensorManager.getOrientation(r, o)
                deg = ((Math.toDegrees(o[0].toDouble()) + 360) % 360).toFloat()
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(l) }
    }
    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    Section("PHONE COMPASS") {
        if (sensor == null) Text("No rotation sensor on this device.", color = SS.Err)
        else {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                Text("▲", fontSize = 90.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.rotate(-deg))
            }
            Text("${deg.toInt()}°  ${dirs[(((deg + 22.5f) % 360f) / 45f).toInt()]}", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("Hold the phone flat. The arrow points to magnetic north.", color = SS.Text2, fontSize = 12.sp)
        }
        Text("This is your phone's heading, not the direction your Minecraft player faces.", color = SS.Warn, fontSize = 12.sp)
    }
}

@Composable
fun GyroTool() {
    val ctx = LocalContext.current
    var v by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    var off by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }
    DisposableEffect(sensor) {
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { v = e.values.clone() }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(l) }
    }
    val sens = Prefs.int("gyro_sens", 100) / 100f
    Section("GYROSCOPE") {
        if (sensor == null) Text("No gyroscope on this device.", color = SS.Err)
        else {
            listOf("X", "Y", "Z").forEachIndexed { i, n -> Text("$n: %.3f rad/s".format((v[i] - off[i]) * sens), fontSize = 20.sp) }
            Button(onClick = { off = v.clone() }) { Text("Calibrate (zero now)") }
            PrefSlider("Sensitivity (%)", "gyro_sens", 100, 10, 300)
        }
        Text("Readout only. Feeding gyro data into Minecraft's camera is not supported.", color = SS.Text2, fontSize = 12.sp)
    }
}

// ------------------------------------------------------------------ keys preview

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeysTool(f: Feature) {
    val keys = listOf("W", "A", "S", "D", "Space", "Shift", "Ctrl")
    val labels = when (f.arg) {
        "kms" -> keys + listOf("L", "R")
        "hotbar" -> (1..9).map { "$it" }
        "mouse" -> listOf("L", "R")
        else -> keys
    }
    Section("PREVIEW") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEach { KeyCap(it) }
        }
        Text("Visual preview only. Press a key to see it highlight. SS Client does not read or send Minecraft input, so this does not switch slots or click in-game.", color = SS.Text2, fontSize = 12.sp)
    }
}

@Composable
fun KeyCap(label: String) {
    var down by remember { mutableStateOf(false) }
    val a = MaterialTheme.colorScheme.primary
    Box(
        Modifier.defaultMinSize(56.dp, 56.dp)
            .background(if (down) a else SS.Bg2, RoundedCornerShape(10.dp))
            .border(1.dp, a, RoundedCornerShape(10.dp))
            .pointerInput(Unit) { detectTapGestures(onPress = { down = true; tryAwaitRelease(); down = false }) }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = if (down) SS.Bg else SS.Text, fontWeight = FontWeight.Bold) }
}

// ------------------------------------------------------------------ music

@Composable
fun MusicTool() {
    val ctx = LocalContext.current
    var mp by remember { mutableStateOf<MediaPlayer?>(null) }
    var name by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf(false) }
    var vol by remember { mutableFloatStateOf(0.7f) }
    var mute by remember { mutableStateOf(false) }
    fun applyVol() { val x = if (mute) 0f else vol; mp?.setVolume(x, x) }
    DisposableEffect(Unit) { onDispose { mp?.release() } }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            mp?.release(); playing = false
            runCatching {
                mp = MediaPlayer().apply { setDataSource(ctx, u); isLooping = true; prepare() }
                name = u.lastPathSegment ?: "audio"
                applyVol()
            }.onFailure { toast(ctx, "Could not load that audio file"); mp = null }
        }
    }
    Section("BACKGROUND MUSIC") {
        Text(if (name.isEmpty()) "No file selected" else name)
        Button(onClick = { pick.launch(arrayOf("audio/*")) }) { Text("Choose your own audio file") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = mp != null, onClick = {
                if (playing) mp?.pause() else mp?.start()
                playing = !playing
            }) { Text(if (playing) "Pause" else "Play") }
            OutlinedButton(enabled = mp != null, onClick = { mute = !mute; applyVol() }) { Text(if (mute) "Unmute" else "Mute") }
        }
        Text("Volume: ${(vol * 100).toInt()}%", color = SS.Text2, fontSize = 13.sp)
        Slider(value = vol, onValueChange = { vol = it; applyVol() })
        Text("No music is bundled with SS Client (copyright). Use only audio you own or have permission to use. It plays while this app is open.", color = SS.Text2, fontSize = 12.sp)
    }
}

// ------------------------------------------------------------------ pack manager

@Composable
fun PacksTool(f: Feature) {
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var info by remember { mutableStateOf("") }
    var bmp by remember { mutableStateOf<ImageBitmap?>(null) }
    var confirm by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            uri = u; bmp = null
            var name = "?"; var size = -1L
            ctx.contentResolver.query(u, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (ni >= 0) name = c.getString(ni) ?: "?"
                    if (si >= 0) size = c.getLong(si)
                }
            }
            val ext = name.substringAfterLast('.', "").lowercase()
            val known = listOf("mcpack", "mcaddon", "mcworld", "mctemplate", "png", "zip")
            info = "Name: $name\nSize: ${if (size >= 0) "${size / 1024} KB" else "unknown"}\nType: ${ctx.contentResolver.getType(u) ?: "unknown"}\n" +
                if (ext in known) "Extension .$ext is a recognised Minecraft/pack format." else "Extension .$ext is not a usual Minecraft pack format."
            if (ext == "png") bmp = runCatching {
                ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }
    val hint = when (f.arg) {
        "texture" -> "Textures must be packaged as a .mcpack (a zip with manifest.json). Pick the .mcpack, then Open with Minecraft."
        "skin" -> "A raw skin .png cannot be imported by another app. Inside Minecraft use Dressing Room → Choose New Skin → Import, or package it as a .mcpack skin pack. A PNG picked here is previewed."
        "pack" -> "Pick a .mcpack (resource/behavior pack) or .mcaddon. Minecraft imports it; you activate it inside Minecraft (world settings → Resource/Behavior Packs)."
        else -> "Supports .mcpack, .mcaddon, .mcworld, .mctemplate and PNG previews."
    }
    Section("FILE") {
        Text(hint, color = SS.Text2, fontSize = 13.sp)
        Button(onClick = { pick.launch(arrayOf("*/*")) }) { Text("Pick file") }
        if (info.isNotEmpty()) Text(info)
        bmp?.let { Image(it, null, Modifier.size(128.dp)) }
        val u = uri
        if (u != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val i = Intent(Intent.ACTION_VIEW).setDataAndType(u, "application/octet-stream")
                    .setPackage(Launcher.MC)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                try { ctx.startActivity(i) } catch (e: Exception) {
                    toast(ctx, "Minecraft could not open this file directly. Copy it to Downloads and open it from your file manager with Minecraft.")
                }
            }) { Text("Open with Minecraft") }
            OutlinedButton(onClick = {
                val s = Intent(Intent.ACTION_SEND).setType("*/*").putExtra(Intent.EXTRA_STREAM, u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ctx.startActivity(Intent.createChooser(s, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("Share") }
            TextButton(onClick = { confirm = true }) { Text("Remove") }
        }
        Text("SS Client never edits Minecraft's protected data folders. 'Remove' only clears this selection and does not delete your file.", color = SS.Text2, fontSize = 12.sp)
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Remove from SS Client?") },
        text = { Text("This clears the selected file here. The file itself is not deleted.") },
        confirmButton = { TextButton(onClick = { uri = null; info = ""; bmp = null; confirm = false }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
    )
}

// ------------------------------------------------------------------ creation studio

private fun slug(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "ss" }

private fun manifest(name: String, type: String, uuid: String, mod: String, dep: String?): String {
    val ver = { JSONArray(listOf(1, 0, 0)) }
    val o = JSONObject()
    o.put("format_version", 2)
    o.put("header", JSONObject().put("name", name).put("description", "Created with SS Bedrock Client")
        .put("uuid", uuid).put("version", ver()).put("min_engine_version", JSONArray(listOf(1, 21, 0))))
    o.put("modules", JSONArray().put(JSONObject().put("type", type).put("uuid", mod).put("version", ver())))
    if (dep != null) o.put("dependencies", JSONArray().put(JSONObject().put("uuid", dep).put("version", ver())))
    return o.toString(2)
}

private fun animTemplate(name: String) = """{
  "format_version": "1.8.0",
  "animations": {
    "animation.${slug(name)}.example": {
      "loop": true,
      "animation_length": 1.0,
      "bones": {
        "body": {
          "rotation": {
            "0.0": [0, 0, 0],
            "0.5": [0, 15, 0],
            "1.0": [0, 0, 0]
          }
        }
      }
    }
  }
}"""

@Composable
fun CreatorTool(f: Feature) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(if (f.arg == "addon") "MyAddon" else "MyAnimation") }
    val files = remember { mutableStateListOf<Pair<String, String>>() }
    var sel by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf("") }

    fun generate() {
        files.clear()
        if (f.arg == "addon") {
            val rp = UUID.randomUUID().toString(); val rpm = UUID.randomUUID().toString()
            val bp = UUID.randomUUID().toString(); val bpm = UUID.randomUUID().toString()
            files.add("${name}_BP/manifest.json" to manifest("$name BP", "data", bp, bpm, rp))
            files.add("${name}_RP/manifest.json" to manifest("$name RP", "resources", rp, rpm, null))
        } else {
            files.add("${name}.animation.json" to animTemplate(name))
        }
        sel = 0; msg = ""
    }
    LaunchedEffect(Unit) { generate() }

    val exportZip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { u ->
        if (u != null) runCatching {
            ctx.contentResolver.openOutputStream(u)?.use { os ->
                ZipOutputStream(os).use { z ->
                    files.forEach { (p, c) -> z.putNextEntry(ZipEntry(p)); z.write(c.toByteArray()); z.closeEntry() }
                }
            }
        }.onSuccess { msg = "Exported. If your file manager added an extra extension, rename it to end in .mcaddon." }
            .onFailure { msg = "Export failed: ${it.message}" }
    }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        if (u != null) runCatching {
            ctx.contentResolver.openOutputStream(u)?.use { it.write(files[0].second.toByteArray()) }
        }.onSuccess { msg = "Exported." }.onFailure { msg = "Export failed: ${it.message}" }
    }

    Section("PROJECT") {
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { generate() }) { Text("Generate template (resets edits)") }
    }
    if (files.isNotEmpty()) Section("EDITOR") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            files.forEachIndexed { i, p -> Chip(p.first.substringAfterLast('/'), sel == i) { sel = i } }
        }
        val idx = sel.coerceIn(0, files.size - 1)
        Text(files[idx].first, color = SS.Text2, fontSize = 12.sp)
        OutlinedTextField(
            files[idx].second, { files[idx] = files[idx].first to it },
            modifier = Modifier.fillMaxWidth(), minLines = 10, maxLines = 24
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val bad = files.firstOrNull { runCatching { JSONObject(it.second) }.isFailure }
                msg = if (bad == null) "All files are valid JSON ✓" else "Invalid JSON in ${bad.first}: " +
                    runCatching { JSONObject(bad.second) }.exceptionOrNull()?.message
            }) { Text("Validate") }
            OutlinedButton(onClick = {
                if (f.arg == "addon") exportZip.launch("$name.mcaddon") else exportJson.launch("${slug(name)}.animation.json")
            }) { Text("Export") }
        }
        if (msg.isNotEmpty()) Text(msg, color = SS.Text2, fontSize = 13.sp)
        val note = when (f.arg) {
            "addon" -> "Creates matching Behavior Pack and Resource Pack manifests with fresh UUIDs and a dependency link, packed as a .mcaddon zip. Add your own entities/scripts/textures to the packs afterwards. Validation checks JSON syntax only, not Minecraft's schema."
            "emote" -> "Creates an animation JSON starting point. Minecraft does not let you play a custom emote from a plain add-on; custom emotes need Marketplace/persona content, so this will not appear in-game by itself."
            else -> "Template for a Bedrock client animation. Validation checks JSON syntax only, not Minecraft's schema."
        }
        Text(note, color = SS.Text2, fontSize = 12.sp)
    }
}

// ------------------------------------------------------------------ SS AI

@Composable
fun AiTool() {
    val scope = rememberCoroutineScope()
    val msgs = remember { mutableStateListOf<Pair<String, String>>() }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val hasKey = Prefs.apiKey().isNotBlank()
    Section("SS AI") {
        Text(
            if (Prefs.aiConnected) "Status: Connected" else if (hasKey) "Status: key saved, connection not tested" else "Status: Not Connected. Add your API key in Settings.",
            color = if (Prefs.aiConnected) SS.Ok else SS.Warn, fontWeight = FontWeight.Bold
        )
        if (!hasKey) Button(onClick = { Nav.screen = Screen.Settings }) { Text("Open Settings") }
    }
    msgs.forEach { (r, c) ->
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(if (r == "user") SS.Card else SS.Bg2, RoundedCornerShape(12.dp)).padding(10.dp)) {
            Text(if (r == "user") "You" else "SS AI", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(c)
        }
    }
    OutlinedTextField(input, { input = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Ask about add-ons, JSON, packs… / বাংলায়ও লিখতে পারেন") })
    Button(
        enabled = !busy && input.isNotBlank() && hasKey,
        onClick = {
            val q = input.trim(); input = ""
            msgs.add("user" to q); busy = true
            scope.launch {
                val r = AiClient.chat(msgs.toList())
                msgs.add("assistant" to r.getOrElse { "Error: ${it.message}" })
                busy = false
            }
        }
    ) { Text(if (busy) "…" else "Send") }
}
