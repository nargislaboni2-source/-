package com.ss.client

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

sealed class Screen {
    data object Home : Screen()
    data object Loading : Screen()
    data object Settings : Screen()
    data object Features : Screen()
    data class Open(val id: Int) : Screen()
}

object Nav {
    var screen by mutableStateOf<Screen>(Screen.Home)
    fun back() { screen = if (screen is Screen.Open) Screen.Features else Screen.Home }
}

class MainActivity : ComponentActivity() {
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(applicationContext)
        if (savedInstanceState == null) handle(intent)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        enableEdgeToEdge()
        setContent {
            SSTheme {
                Surface(Modifier.fillMaxSize(), color = SS.Bg, contentColor = SS.Text) {
                    Box(Modifier.safeDrawingPadding()) { App() }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(i: Intent?) {
        val id = i?.getIntExtra("feature", -1) ?: -1
        if (id in 1..71) Nav.screen = Screen.Open(id)
    }
}

@Composable
fun App() {
    val s = Nav.screen
    BackHandler(enabled = s !is Screen.Home) { Nav.back() }
    when (s) {
        is Screen.Home -> HomeScreen()
        is Screen.Loading -> LoadingScreen()
        is Screen.Settings -> SettingsScreen()
        is Screen.Features -> FeaturesScreen()
        is Screen.Open -> ToolHost(s.id)
    }
}

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    var ask by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "press")
    val glow = MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(t("Welcome", "স্বাগতম"), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = SS.Text)
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { if (Settings.canDrawOverlays(ctx)) Nav.screen = Screen.Loading else ask = true },
            interactionSource = interaction,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.scale(scale).width(240.dp).height(64.dp)
                .shadow(24.dp, RoundedCornerShape(32.dp), ambientColor = glow, spotColor = glow)
        ) { Text("PLAY", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp) }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { Nav.screen = Screen.Settings },
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.width(240.dp).height(52.dp)
        ) { Text("SETTINGS", letterSpacing = 3.sp) }
    }

    if (ask) AlertDialog(
        onDismissRequest = { ask = false },
        title = { Text(t("Floating logo permission", "ফ্লোটিং লোগোর অনুমতি")) },
        text = {
            Text(t(
                "To show the floating SS logo over Minecraft, Android needs your permission (Display over other apps). You can also continue without the overlay.",
                "Minecraft-এর ওপর ভাসমান SS লোগো দেখাতে Android-এর অনুমতি (Display over other apps) লাগবে। চাইলে ওভারলে ছাড়াই চালাতে পারেন।"
            ))
        },
        confirmButton = {
            TextButton(onClick = { ask = false; ctx.startActivity(Launcher.overlayIntent(ctx)) }) { Text(t("Grant", "অনুমতি দিন")) }
        },
        dismissButton = {
            TextButton(onClick = { ask = false; Nav.screen = Screen.Loading }) { Text(t("Continue without", "ছাড়া চালান")) }
        }
    )
}

@Composable
fun LoadingScreen() {
    val ctx = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        delay(2200)
        if (!Launcher.isInstalled(ctx)) {
            error = t(
                "Minecraft Bedrock (com.mojang.minecraftpe) is not installed on this device.",
                "এই ডিভাইসে Minecraft Bedrock (com.mojang.minecraftpe) ইনস্টল করা নেই।"
            )
            return@LaunchedEffect
        }
        if (Settings.canDrawOverlays(ctx)) OverlayService.start(ctx)
        if (Launcher.launch(ctx)) Nav.screen = Screen.Home
        else error = t("Could not start Minecraft.", "Minecraft চালু করা যায়নি।")
    }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        SSLogo(120.dp)
        Spacer(Modifier.height(24.dp))
        Text("SS CLIENT", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
        Spacer(Modifier.height(24.dp))
        val e = error
        if (e == null) CircularProgressIndicator()
        else {
            Text(e, color = SS.Err, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { Nav.screen = Screen.Home }) { Text(t("Back", "ফিরে যান")) }
        }
    }
}
