package com.ss.client

import com.ss.client.Cat.*
import com.ss.client.Status.*
import com.ss.client.Tool.*

enum class Cat(val en: String, val bn: String, val icon: String) {
    PVP("PvP & HUD", "পিভিপি ও HUD", "⚔️"),
    PERF("Performance", "পারফরম্যান্স", "⚡"),
    GAME("Gameplay Tools", "গেমপ্লে টুলস", "🎮"),
    CHAT("Chat & Social", "চ্যাট ও সোশ্যাল", "💬"),
    CUST("Customization", "কাস্টমাইজেশন", "🎨"),
    RPM("Resource Pack Manager", "রিসোর্স প্যাক ম্যানেজার", "📦"),
    WORLD("World Tools", "ওয়ার্ল্ড টুলস", "🌍"),
    CREATE("Creation Studio", "ক্রিয়েশন স্টুডিও", "🛠️"),
    AI("SS AI", "SS AI", "🤖"),
    ADV("Advanced & Java Features", "অ্যাডভান্সড ও জাভা ফিচার", "☕")
}

enum class Status(val label: String) {
    AVAILABLE("Available"), LIMITED("Limited"), REQUIRES("Requires Integration"), UNSUPPORTED("Not Supported")
}

enum class Tool {
    GUIDE, COUNTER, CPS, STOPWATCH, CLOCK, REACH, WAYPOINTS, TEMPLATES, HARDWARE, PING, COMPASS, GYRO,
    PACKS, CREATOR, ASSISTANT, KEYS, MUSIC, ANDROIDSETTINGS, HUD, OVERLAY, LANG, UISETTINGS
}

data class Feature(
    val id: Int, val name: String, val cat: Cat, val desc: String,
    val status: Status, val tool: Tool, val arg: String = ""
) {
    /** Name of the overlay HUD this feature controls, if any. */
    val hud: String? get() = when (tool) { HUD -> arg; CLOCK -> "clock"; else -> null }
}

private const val NODATA = "This needs live data from inside Minecraft. Android does not let a separate app read another app's game data and Bedrock has no official API for it, so SS Client cannot do this and will not fake it."
private const val NOINPUT = "This needs SS Client to inject or intercept touch/keyboard input inside Minecraft. Android blocks that for normal apps (it would need root, accessibility abuse or a modified game), so it is Not Supported."
private const val NEEDPACK = "This is only possible through a Bedrock Resource Pack, Add-on or shader that runs inside Minecraft; SS Client cannot change the game's renderer itself. Import a compatible pack you trust with the Pack Manager, then activate it in Minecraft."
private const val NEEDSERVER = "This needs a compatible server-side plugin or voice service and its API. No such integration is included in this build."

private fun f(id: Int, name: String, cat: Cat, st: Status, tool: Tool, arg: String, desc: String) =
    Feature(id, name, cat, desc, st, tool, arg)

val FEATURES: List<Feature> = listOf(
    f(1, "FPS Display", PVP, UNSUPPORTED, GUIDE, "Real Minecraft FPS shows as Unavailable. Android does not let one app read another app's frame rate. Nothing is faked. Android Developer options → Profile GPU rendering can measure it.", "Minecraft FPS counter (shows Unavailable)"),
    f(2, "FPS Booster", PERF, AVAILABLE, GUIDE, "Advice only. In Minecraft → Settings → Video: lower Render Distance (try 6–8 chunks), turn off Fancy Leaves, Smooth Lighting, Beautiful Skies and Particles, and lower graphics quality. Close other apps and turn off battery saver while playing.", "Settings advice to reduce lag"),
    f(3, "CPS Counter", PVP, LIMITED, CPS, "", "Counts taps on the in-app tap pad"),
    f(4, "CPS Limiter", PVP, UNSUPPORTED, GUIDE, NOINPUT, "Cap clicks per second (needs input control)"),
    f(5, "Third Person Gamer Tag", PVP, REQUIRES, GUIDE, NEEDPACK, "Show your name in third person"),
    f(6, "Nickname", CHAT, AVAILABLE, TEMPLATES, "nick", "Create, save and copy nicknames"),
    f(7, "DVD Screen", PVP, AVAILABLE, HUD, "dvd", "Bouncing SS logo overlay"),
    f(8, "Armor HUD", PVP, LIMITED, COUNTER, "armor4", "Manual armor tracker (no live game data)"),
    f(9, "Free Look", GAME, UNSUPPORTED, GUIDE, NODATA, "Free camera look (needs game camera access)"),
    f(10, "Zoom", GAME, REQUIRES, GUIDE, NEEDPACK, "Zoom view in gameplay"),
    f(11, "Combo Counter", PVP, LIMITED, COUNTER, "combo", "Manual combo counter with best score"),
    f(12, "Quick Drop", GAME, UNSUPPORTED, GUIDE, NOINPUT, "One-tap item drop"),
    f(13, "Key Strokes", PVP, LIMITED, KEYS, "keys", "Visual key preview (not connected to the game)"),
    f(14, "Custom Crosshair", PVP, AVAILABLE, HUD, "crosshair", "Overlay crosshair: shape, size, colour, opacity, position"),
    f(15, "Crosshair Indicator", PVP, AVAILABLE, HUD, "crosshair", "Show/hide and adjust the overlay crosshair"),
    f(16, "OPL — Online Player List", CHAT, LIMITED, TEMPLATES, "players", "Manual player name list (no server data)"),
    f(17, "Gyro", GAME, AVAILABLE, GYRO, "", "Phone gyroscope readout, calibration, sensitivity"),
    f(18, "Air Swing", PVP, UNSUPPORTED, GUIDE, NODATA, "Swing animation on attack"),
    f(19, "Hotbar Key Buttons", PVP, LIMITED, KEYS, "hotbar", "Visual 1–9 hotbar preview (does not switch slots)"),
    f(20, "KMS — Keyboard & Mouse System", PVP, LIMITED, KEYS, "kms", "Visual keyboard and mouse preview"),
    f(21, "Replay Mod", ADV, UNSUPPORTED, GUIDE, "Replaying a match needs the game's internal data, which Android apps cannot read. Use Android's built-in screen recorder (Quick Settings → Screen record) to record gameplay instead.", "Gameplay replay"),
    f(22, "Dynamic Lights", PERF, REQUIRES, GUIDE, NEEDPACK, "Held-item light support"),
    f(23, "Online AFK", PERF, UNSUPPORTED, GUIDE, "SS Client cannot keep a server session active or send input to Minecraft, so it does not claim to. Servers may also forbid AFK tools; follow your server's rules.", "AFK helper"),
    f(24, "JAVA Chat", CHAT, LIMITED, TEMPLATES, "javachat", "Compose and copy Java chat messages (no Java server bridge)"),
    f(25, "Voice Chat", CHAT, REQUIRES, GUIDE, NEEDSERVER, "In-game voice chat"),
    f(26, "Proximity Chat", CHAT, REQUIRES, GUIDE, NEEDSERVER, "Voice chat with nearby players"),
    f(27, "Direction HUD", GAME, AVAILABLE, COMPASS, "", "Phone compass heading (not Minecraft's facing direction)"),
    f(28, "Coordinates", GAME, LIMITED, WAYPOINTS, "coords", "Save coordinates you type in manually"),
    f(29, "Auto GG", CHAT, LIMITED, TEMPLATES, "gg", "GG message templates to copy (no auto-send)"),
    f(30, "Fast Loading Without Extra Graphics", PERF, AVAILABLE, GUIDE, "Advice only. Lower Render Distance, turn off Fancy Graphics/Particles, avoid heavy shader or texture packs, free up storage and RAM, and keep the game updated. Load times mostly depend on your device.", "Settings to speed up loading"),
    f(31, "60 FPS Shader", PERF, REQUIRES, GUIDE, NEEDPACK + " No shader can guarantee 60 FPS.", "Compatible shader information"),
    f(32, "Left Hand Mod", GAME, REQUIRES, GUIDE, "Check Minecraft → Settings → Controls/Video for hand and control-layout options (they vary by version). A resource pack can also mirror the held-item position. SS Client cannot change game settings itself.", "Left-hand controls"),
    f(33, "Custom Texture", CUST, AVAILABLE, PACKS, "texture", "Pick, preview and open texture packs in Minecraft"),
    f(34, "Custom Skin", CUST, AVAILABLE, PACKS, "skin", "Pick, preview and open skin files in Minecraft"),
    f(35, "Motion Blur", PERF, UNSUPPORTED, GUIDE, NEEDPACK + " Bedrock has no motion blur setting.", "Motion blur"),
    f(36, "Hardware Status", PERF, AVAILABLE, HARDWARE, "", "Phone model, Android, RAM, battery, screen"),
    f(37, "Inventory Status", GAME, LIMITED, TEMPLATES, "inventory", "Manual inventory notes (no live inventory data)"),
    f(38, "Quick Move", GAME, UNSUPPORTED, GUIDE, NOINPUT, "Fast item moving"),
    f(39, "Edit Minecraft Settings", GAME, LIMITED, ANDROIDSETTINGS, "", "Shortcut to Minecraft's Android app settings"),
    f(40, "Chat Template", CHAT, AVAILABLE, TEMPLATES, "chat", "Create, edit, save, delete and copy chat messages"),
    f(41, "Command Template", CHAT, AVAILABLE, TEMPLATES, "cmd", "Save and copy Minecraft commands"),
    f(42, "In-game RP & BP Switching", RPM, LIMITED, PACKS, "pack", "Open .mcpack/.mcaddon files in Minecraft"),
    f(43, "Edit UI", CUST, LIMITED, UISETTINGS, "", "Accent colour, font size, floating logo size and position"),
    f(44, "Weather Changer", WORLD, AVAILABLE, TEMPLATES, "weather", "Weather commands to copy (needs world permission)"),
    f(45, "Time Change", WORLD, AVAILABLE, TEMPLATES, "time", "Time commands to copy (needs world permission)"),
    f(46, "Stopwatch", GAME, AVAILABLE, STOPWATCH, "", "Start, pause, resume, reset"),
    f(47, "Clock", GAME, AVAILABLE, CLOCK, "", "Digital clock, plus optional overlay clock"),
    f(48, "Minimap", WORLD, LIMITED, WAYPOINTS, "waypoints", "Manual waypoints with distance (no live map)"),
    f(49, "PING Counter", PERF, LIMITED, PING, "", "TCP connect-time test (not Minecraft's UDP ping)"),
    f(50, "Reach Counter", PVP, LIMITED, REACH, "", "Distance between two XYZ points in blocks"),
    f(51, "Pin Counter", PVP, AVAILABLE, COUNTER, "pin", "Manual add/remove/reset counter"),
    f(52, "Pot Counter", PVP, AVAILABLE, COUNTER, "pot", "Manual potion counter"),
    f(53, "Armor Counter", PVP, AVAILABLE, COUNTER, "armor4", "Counters for helmet, chestplate, leggings, boots"),
    f(54, "Potion HUD", PVP, LIMITED, TEMPLATES, "potions", "Manual potion effect notes (no live effect data)"),
    f(55, "SSC — Successful Shot Count", PVP, LIMITED, COUNTER, "ssc", "Manual shot counter"),
    f(56, "Entity Counter", GAME, LIMITED, COUNTER, "entity", "Manual entity counter"),
    f(57, "Allow Emoji Only in Proximity", CHAT, LIMITED, TEMPLATES, "emoji", "Emoji picker to copy (no proximity delivery)"),
    f(58, "Remove Lag 100", PERF, AVAILABLE, GUIDE, "Advice only, and no tool can remove 100% of lag. Lower Render Distance and Simulation Distance, turn off Particles, Fancy Leaves and shadows, use a light world, close background apps, keep your phone cool, and update Minecraft.", "Lag reduction advice"),
    f(59, "Custom Language", CUST, AVAILABLE, LANG, "", "English / বাংলা for SS Client's screens"),
    f(60, "Custom Font", CUST, LIMITED, UISETTINGS, "", "SS Client text size (Minecraft's font needs a pack)"),
    f(61, "SS UI", CUST, AVAILABLE, UISETTINGS, "", "SS Blue Glow theme and accent colour"),
    f(62, "Background Music", CUST, LIMITED, MUSIC, "", "Play your own audio file (plays while this app is open)"),
    f(63, "Background Play", PERF, AVAILABLE, OVERLAY, "", "Foreground service that keeps the floating logo running"),
    f(64, "Mouse", PVP, LIMITED, KEYS, "mouse", "Visual left/right mouse button preview"),
    f(65, "RP, BP, World & Skin Pack Manager", RPM, AVAILABLE, PACKS, "all", "File picker, info, preview, open with Minecraft, share"),
    f(66, "Custom One Buttons", CUST, LIMITED, TEMPLATES, "buttons", "Your own copy-text shortcut buttons (Android only)"),
    f(67, "Create Animation", CREATE, AVAILABLE, CREATOR, "anim", "Animation JSON template: edit, validate, export"),
    f(68, "Create Add-on", CREATE, AVAILABLE, CREATOR, "addon", "BP + RP manifests, JSON editor, .mcaddon export"),
    f(69, "Create Emote", CREATE, LIMITED, CREATOR, "emote", "Emote animation JSON helper (not directly playable)"),
    f(70, "SS Client AI", AI, LIMITED, ASSISTANT, "", "Grok-powered assistant (needs your API key)"),
    f(71, "Java Features", ADV, AVAILABLE, GUIDE, "Java-only (do NOT work in Bedrock): Java chat/commands bridge, Java mods such as OptiFine, Replay Mod, Java-only keybinds and server plugins.\n\nBedrock-compatible ideas: resource packs, add-ons, FPS/lag settings advice, command templates, coordinates notes.\n\nSS Client never claims a Java feature works in Bedrock.", "Java-only vs Bedrock-compatible list")
).sortedBy { it.id }
