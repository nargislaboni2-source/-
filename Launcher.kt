package com.ss.client

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

object Launcher {
    const val MC = "com.mojang.minecraftpe"

    fun isInstalled(c: Context) = c.packageManager.getLaunchIntentForPackage(MC) != null

    fun launch(c: Context): Boolean {
        val i = c.packageManager.getLaunchIntentForPackage(MC) ?: return false
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try { c.startActivity(i); true } catch (e: Exception) { false }
    }

    fun overlayIntent(c: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${c.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

fun toast(c: Context, s: String) = Toast.makeText(c, s, Toast.LENGTH_LONG).show()

fun copyText(c: Context, s: String) {
    val cm = c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("SS Client", s))
    toast(c, t("Copied", "কপি হয়েছে"))
}
