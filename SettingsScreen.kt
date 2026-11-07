package com.ss.client

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { Nav.back() }) { Text("←", fontSize = 20.sp) }
            Text(t("SETTINGS", "সেটিংস"), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        SignInSection()
        AiSection()
        OtherSection()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SignInSection() {
    var u by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    Section(t("1. SIGN IN", "১. সাইন ইন")) {
        if (Account.signedIn) {
            Text("${t("Profile", "প্রোফাইল")}: ${Account.user}")
            Text(t("Account status: Signed in (local account on this device)", "অ্যাকাউন্ট স্ট্যাটাস: সাইন ইন করা (এই ডিভাইসের লোকাল অ্যাকাউন্ট)"), color = SS.Ok)
            OutlinedButton(onClick = { Account.signOut() }) { Text(t("Sign Out", "সাইন আউট")) }
        } else {
            OutlinedTextField(u, { u = it }, label = { Text(t("Username", "ইউজারনেম")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                p, { p = it }, label = { Text(t("Password", "পাসওয়ার্ড")) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { val e = Account.signIn(u, p); msg = e ?: ""; if (e == null) p = "" }) { Text(t("Sign In", "সাইন ইন")) }
                OutlinedButton(onClick = { val e = Account.create(u, p); msg = e ?: ""; if (e == null) p = "" }) { Text(t("Create Account", "অ্যাকাউন্ট তৈরি")) }
            }
            if (msg.isNotEmpty()) Text(msg, color = SS.Err)
            Text(t("Account status: Signed out", "অ্যাকাউন্ট স্ট্যাটাস: সাইন আউট"), color = SS.Text2)
        }
        Text(
            t("Online sync: Not Connected. No account server is configured, so accounts exist only on this device. Passwords are salted and hashed (PBKDF2), never saved as plain text.",
                "অনলাইন সিঙ্ক: সংযুক্ত নয়। কোনো অ্যাকাউন্ট সার্ভার সেট করা নেই, তাই অ্যাকাউন্ট শুধু এই ডিভাইসে থাকে। পাসওয়ার্ড salt সহ hash করে রাখা হয়, plain text-এ নয়।"),
            color = SS.Text2, fontSize = 12.sp
        )
    }
}

@Composable
fun AiSection() {
    val scope = rememberCoroutineScope()
    var prov by remember { mutableStateOf(Prefs.aiProvider) }
    var endpoint by remember { mutableStateOf(Prefs.aiEndpoint()) }
    var model by remember { mutableStateOf(Prefs.aiModel()) }
    var key by remember { mutableStateOf(Prefs.apiKey()) }
    var status by remember { mutableStateOf("") }

    fun load(p: String) {
        prov = p
        endpoint = Prefs.str("ai_endpoint_$p", Prefs.providerDefaultEndpoint(p))
        model = Prefs.str("ai_model_$p", Prefs.providerDefaultModel(p))
    }
    fun save() {
        Prefs.put("ai_provider", prov)
        Prefs.put("ai_endpoint_$prov", endpoint.trim())
        Prefs.put("ai_model_$prov", model.trim())
        val ok = Prefs.setApiKey(key.trim())
        status = if (ok) t("Saved.", "সেভ হয়েছে।")
        else t("Settings saved, but secure storage is unavailable, so the API key was NOT saved.", "সেটিংস সেভ হয়েছে, কিন্তু সুরক্ষিত স্টোরেজ নেই বলে API Key সেভ হয়নি।")
        Prefs.aiConnected = false
    }

    Section(t("2. SS AI'S API CUSTOMIZATION", "২. SS AI-এর API কাস্টমাইজেশন")) {
        Text(t("Default AI Provider: Grok", "ডিফল্ট AI প্রোভাইডার: Grok"), fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("grok" to "Grok — Default", "custom" to "Custom API", "other" to "Other (OpenAI-compatible)").forEach { (id, label) ->
                Chip(label, prov == id) { load(id) }
            }
        }
        OutlinedTextField(endpoint, { endpoint = it }, label = { Text("API Endpoint (base URL)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(model, { model = it }, label = { Text("Model") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            key, { key = it }, label = { Text("API Key") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                save()
                scope.launch {
                    status = t("Testing…", "পরীক্ষা চলছে…")
                    val r = AiClient.test()
                    Prefs.aiConnected = r.isSuccess
                    status = r.fold({ t("Connected ✓", "সংযুক্ত ✓") }, { "${t("Not connected", "সংযুক্ত নয়")}: ${it.message}" })
                }
            }) { Text(t("Test Connection", "কানেকশন টেস্ট")) }
            OutlinedButton(onClick = { save() }) { Text(t("Save", "সেভ")) }
            TextButton(onClick = {
                Prefs.put("ai_provider", "grok")
                listOf("grok", "custom", "other").forEach { Prefs.remove("ai_endpoint_$it"); Prefs.remove("ai_model_$it") }
                Prefs.setApiKey("")
                key = ""; load("grok"); Prefs.aiConnected = false
                status = t("Reset to default (Grok).", "ডিফল্টে (Grok) রিসেট হয়েছে।")
            }) { Text(t("Reset", "রিসেট")) }
        }
        Text(
            if (Prefs.aiConnected) t("AI status: Connected", "AI স্ট্যাটাস: সংযুক্ত")
            else t("AI status: Not Connected", "AI স্ট্যাটাস: সংযুক্ত নয়"),
            color = if (Prefs.aiConnected) SS.Ok else SS.Warn, fontWeight = FontWeight.Bold
        )
        if (status.isNotEmpty()) Text(status, color = SS.Text2, fontSize = 13.sp)
        Text(
            t("The key is stored only in Android's encrypted storage on this device. It is never written into the app's source code or APK. Providers must offer an OpenAI-compatible /chat/completions API.",
                "Key শুধু এই ডিভাইসের এনক্রিপ্টেড স্টোরেজে থাকে, অ্যাপের সোর্স কোড বা APK-তে লেখা হয় না। প্রোভাইডারের OpenAI-compatible /chat/completions API থাকতে হবে।"),
            color = SS.Text2, fontSize = 12.sp
        )
    }
}

@Composable
fun AccentPicker() {
    val ctx = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ACCENTS.forEach { c ->
            Box(
                Modifier.size(36.dp).background(Color(c), CircleShape)
                    .border(if (Prefs.accent == c) 3.dp else 0.dp, SS.Text, CircleShape)
                    .clickable { Prefs.put("accent", c); OverlayService.refresh(ctx) }
            )
        }
    }
}

@Composable
fun LanguageChooser() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("English", Prefs.language == "en") { Prefs.put("lang", "en") }
        Chip("বাংলা", Prefs.language == "bn") { Prefs.put("lang", "bn") }
    }
}

@Composable
fun OverlayPermissionRow() {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }
    LaunchedEffect(Unit) { while (true) { granted = Settings.canDrawOverlays(ctx); delay(1000) } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${t("Overlay permission", "ওভারলে পারমিশন")}: " + if (granted) t("Granted", "দেওয়া আছে") else t("Not granted", "দেওয়া নেই"),
            color = if (granted) SS.Ok else SS.Warn, modifier = Modifier.weight(1f)
        )
        if (!granted) Button(onClick = { ctx.startActivity(Launcher.overlayIntent(ctx)) }) { Text(t("Grant", "অনুমতি")) }
    }
}

@Composable
fun LogoSettings() {
    val ctx = LocalContext.current
    fun pos(fx: Int, fy: Int) { Prefs.put("logo_fx", fx); Prefs.put("logo_fy", fy); OverlayService.refresh(ctx) }
    PrefSlider(t("Floating logo size (dp)", "ফ্লোটিং লোগোর সাইজ (dp)"), "logo_size", 56, 40, 100) { OverlayService.refresh(ctx) }
    Text(t("Floating logo position (you can also drag it)", "ফ্লোটিং লোগোর অবস্থান (টেনেও সরানো যায়)"), color = SS.Text2, fontSize = 13.sp)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { pos(0, 0) }) { Text(t("Top-left", "উপরে-বামে")) }
        OutlinedButton(onClick = { pos(1000, 0) }) { Text(t("Top-right", "উপরে-ডানে")) }
        OutlinedButton(onClick = { pos(0, 500) }) { Text(t("Left", "বামে")) }
        OutlinedButton(onClick = { pos(1000, 500) }) { Text(t("Right", "ডানে")) }
    }
    OverlayPermissionRow()
}

@Composable
fun OtherSection() {
    val ctx = LocalContext.current
    var dlg by remember { mutableStateOf("") }
    val version = remember { try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?" } catch (e: Exception) { "?" } }
    Section(t("3. OTHER SETTINGS", "৩. অন্যান্য সেটিংস")) {
        Text("${t("Theme", "থিম")}: SS Blue Glow")
        Text(t("Accent Colour", "অ্যাকসেন্ট কালার"), color = SS.Text2, fontSize = 13.sp)
        AccentPicker()
        Text(t("Language", "ভাষা"), color = SS.Text2, fontSize = 13.sp)
        LanguageChooser()
        LogoSettings()
        OutlinedButton(onClick = {
            ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
        }) { Text(t("Notifications", "নোটিফিকেশন")) }
        Row {
            TextButton(onClick = { dlg = "privacy" }) { Text(t("Privacy Policy", "প্রাইভেসি পলিসি")) }
            TextButton(onClick = { dlg = "about" }) { Text(t("About SS Client", "SS Client সম্পর্কে")) }
        }
        Text("${t("App Version", "অ্যাপ ভার্সন")}: $version", color = SS.Text2)
        Button(onClick = { Nav.screen = Screen.Features }) { Text(t("Open Feature Panel (in app)", "ফিচার প্যানেল খুলুন (অ্যাপের ভিতরে)")) }
    }
    if (dlg.isNotEmpty()) AlertDialog(
        onDismissRequest = { dlg = "" },
        title = { Text(if (dlg == "privacy") "Privacy Policy" else "About SS Client") },
        text = {
            Text(
                if (dlg == "privacy")
                    "SS Client keeps your settings, templates and local account only on this device. The API key is stored in Android's encrypted storage. If you use SS AI, your messages are sent to the AI provider you configured (Grok by default) and are governed by that provider's policy. SS Client has no analytics and does not read Minecraft's data or files."
                else
                    "SS Bedrock Client is an unofficial companion launcher and toolkit for Minecraft Bedrock. It is not affiliated with Mojang or Microsoft. It cannot read or change Minecraft's protected data."
            )
        },
        confirmButton = { TextButton(onClick = { dlg = "" }) { Text("OK") } }
    )
}
