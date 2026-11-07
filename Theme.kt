package com.ss.client

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** SS Blue Glow palette */
object SS {
    val Bg = Color(0xFF050B14)
    val Bg2 = Color(0xFF0B1728)
    val Card = Color(0xFF10243D)
    val Blue = Color(0xFF168BFF)
    val Glow = Color(0xFF00A8FF)
    val High = Color(0xFF55C7FF)
    val Text = Color(0xFFF2F7FF)
    val Text2 = Color(0xFF9DB5CF)
    val Ok = Color(0xFF22C55E)
    val Warn = Color(0xFFF59E0B)
    val Err = Color(0xFFEF4444)
}

val ACCENTS: List<Int> = listOf(0xFF168BFF, 0xFF00A8FF, 0xFF55C7FF, 0xFF22C55E, 0xFFA855F7, 0xFFF59E0B).map { it.toInt() }

@Composable
fun SSTheme(content: @Composable () -> Unit) {
    val accent = Color(Prefs.accent)
    val fs = Prefs.int("font_scale", 100) / 100f
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, fs)) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = accent, onPrimary = SS.Bg, secondary = SS.High,
                background = SS.Bg, onBackground = SS.Text,
                surface = SS.Bg2, onSurface = SS.Text,
                surfaceVariant = SS.Card, onSurfaceVariant = SS.Text2, error = SS.Err
            ),
            content = content
        )
    }
}

@Composable
fun SSLogo(size: Dp) {
    val a = MaterialTheme.colorScheme.primary
    Box(
        Modifier.size(size)
            .shadow(size / 4, CircleShape, ambientColor = a, spotColor = a)
            .background(SS.Bg2, CircleShape)
            .border(size / 24, a, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("SS", color = SS.Text, fontWeight = FontWeight.Black, fontSize = (size.value * 0.38f).sp)
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .background(SS.Card, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) })
}

@Composable
fun PrefSlider(label: String, key: String, def: Int, min: Int, max: Int, onChange: () -> Unit = {}) {
    var v by remember { mutableFloatStateOf(Prefs.int(key, def).toFloat()) }
    Column {
        Text("$label: ${v.toInt()}", color = SS.Text2, fontSize = 13.sp)
        Slider(
            value = v, onValueChange = { v = it },
            valueRange = min.toFloat()..max.toFloat(),
            onValueChangeFinished = { Prefs.put(key, v.toInt()); onChange() }
        )
    }
}

fun Status.color(): Color = when (this) {
    Status.AVAILABLE -> SS.Ok
    Status.LIMITED -> SS.Warn
    Status.REQUIRES -> SS.High
    Status.UNSUPPORTED -> SS.Err
}
