package com.ss.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

val CH_COLORS: List<Int> = listOf(0xFFFFFFFF, 0xFF00A8FF, 0xFF22C55E, 0xFFEF4444, 0xFFF59E0B).map { it.toInt() }

/** Round SS logo with neon glow border. */
class LogoView(c: Context, private val px: Int, private val accent: Int) : View(c) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0B1728.toInt() }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = px * 0.06f; color = accent
        setShadowLayer(px * 0.12f, 0f, 0f, accent)
    }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF2F7FF.toInt(); textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD; textSize = px * 0.38f
    }
    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }
    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(px, px)
    override fun onDraw(canvas: Canvas) {
        val r = px / 2f
        canvas.drawCircle(r, r, r * 0.8f, fill)
        canvas.drawCircle(r, r, r * 0.8f, ring)
        canvas.drawText("SS", r, r - (txt.ascent() + txt.descent()) / 2f, txt)
    }
}

class CrosshairView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        strokeWidth = 3f * ctx.resources.displayMetrics.density
    }
    private val shape = Prefs.int("ch_shape", 0)
    init {
        p.color = CH_COLORS[Prefs.int("ch_color", 0).coerceIn(0, CH_COLORS.size - 1)]
        p.alpha = Prefs.int("ch_opacity", 90).coerceIn(10, 100) * 255 / 100
    }
    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f; val cy = height / 2f
        val r = min(cx, cy) - p.strokeWidth
        when (shape) {
            1 -> { p.style = Paint.Style.FILL; canvas.drawCircle(cx, cy, r * 0.22f, p) }
            2 -> { p.style = Paint.Style.STROKE; canvas.drawCircle(cx, cy, r * 0.7f, p) }
            else -> {
                p.style = Paint.Style.STROKE
                val g = r * 0.25f
                canvas.drawLine(cx - r, cy, cx - g, cy, p); canvas.drawLine(cx + g, cy, cx + r, cy, p)
                canvas.drawLine(cx, cy - r, cx, cy - g, p); canvas.drawLine(cx, cy + g, cx, cy + r, p)
            }
        }
    }
}

/** DVD-screensaver style bouncing logo. */
class DvdView(ctx: Context) : View(ctx) {
    private val d = ctx.resources.displayMetrics.density
    private val size = 72f * d
    private var x = 60f; private var y = 60f
    private var vx = 220f * d; private var vy = 170f * d
    private var hue = 200f; private var last = 0L
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0B1728.toInt() }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = size * 0.07f }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; textSize = size * 0.38f
    }
    override fun onDraw(canvas: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0f else (now - last) / 1e9f
        last = now
        x += vx * dt; y += vy * dt
        var bounced = false
        if (x < 0) { x = 0f; vx = abs(vx); bounced = true } else if (x + size > width) { x = width - size; vx = -abs(vx); bounced = true }
        if (y < 0) { y = 0f; vy = abs(vy); bounced = true } else if (y + size > height) { y = height - size; vy = -abs(vy); bounced = true }
        if (bounced) hue = (hue + 47f) % 360f
        val col = Color.HSVToColor(floatArrayOf(hue, 0.8f, 1f))
        ring.color = col; txt.color = col
        val cx = x + size / 2f; val cy = y + size / 2f
        canvas.drawCircle(cx, cy, size * 0.45f, fill)
        canvas.drawCircle(cx, cy, size * 0.45f, ring)
        canvas.drawText("SS", cx, cy - (txt.ascent() + txt.descent()) / 2f, txt)
        postInvalidateOnAnimation()
    }
}

/**
 * Floating overlay: draggable logo, tap to open the feature panel, optional HUD overlays.
 * Requires the user-granted "Display over other apps" permission.
 */
class OverlayService : Service() {
    companion object {
        const val A_START = "ss.start"
        const val A_STOP = "ss.stop"
        const val A_SHOW = "ss.show"
        const val A_REFRESH = "ss.refresh"
        private const val CH = "ss_overlay"
        @Volatile var running = false

        fun start(c: Context) {
            ContextCompat.startForegroundService(c, Intent(c, OverlayService::class.java).setAction(A_START))
        }
        fun stop(c: Context) { c.stopService(Intent(c, OverlayService::class.java)) }
        fun refresh(c: Context) {
            if (!running) return
            try { c.startService(Intent(c, OverlayService::class.java).setAction(A_REFRESH)) } catch (e: Exception) { }
        }
    }

    private lateinit var wm: WindowManager
    private var logo: View? = null
    private var panel: View? = null
    private val huds = mutableMapOf<String, View>()
    private var hidden = false
    private val handler = Handler(Looper.getMainLooper())

    private val TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val TEXT = 0xFFF2F7FF.toInt()
    private val TEXT2 = 0xFF9DB5CF.toInt()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private val accent: Int get() = Prefs.accent

    private fun screen(): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.currentWindowMetrics.bounds; b.width() to b.height()
        } else {
            val d = resources.displayMetrics; d.widthPixels to d.heightPixels
        }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Prefs.init(applicationContext)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        when (i?.action) {
            A_STOP -> { stopSelf(); return START_NOT_STICKY }
            A_SHOW -> { hidden = false; logo?.visibility = View.VISIBLE; startFg() }
            A_REFRESH -> { rebuildLogo(); applyHuds() }
            else -> {
                startFg()
                if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
                if (!running) { running = true; rebuildLogo(); applyHuds() } else applyHuds()
            }
        }
        return START_NOT_STICKY
    }

    private fun startFg() {
        val n = notif()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun notif(): Notification {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CH, "SS Client overlay", NotificationManager.IMPORTANCE_LOW))
        fun pi(a: String, code: Int) = PendingIntent.getService(
            this, code, Intent(this, OverlayService::class.java).setAction(a),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CH)
            .setSmallIcon(R.drawable.ic_ss_logo)
            .setContentTitle("SS Bedrock Client")
            .setContentText(if (hidden) "Floating logo hidden" else "Floating logo active")
            .setContentIntent(open).setOngoing(true)
            .addAction(0, "Show logo", pi(A_SHOW, 1))
            .addAction(0, "Stop", pi(A_STOP, 2))
            .build()
    }

    // ---------------- floating logo ----------------
    private fun rebuildLogo() {
        logo?.let { try { wm.removeView(it) } catch (e: Exception) { } }
        val px = dp(Prefs.int("logo_size", 56))
        val v = LogoView(this, px, accent)
        val (sw, sh) = screen()
        val lp = WindowManager.LayoutParams(
            px, px, TYPE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = (Prefs.int("logo_fx", 20) / 1000f * max(1, sw - px)).toInt()
        lp.y = (Prefs.int("logo_fy", 300) / 1000f * max(1, sh - px)).toInt()
        v.setOnTouchListener(object : View.OnTouchListener {
            var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false
            override fun onTouch(view: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY; moved = false }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                        if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                        if (moved) { lp.x = sx + dx; lp.y = sy + dy; wm.updateViewLayout(view, lp) }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) togglePanel() else {
                            val (w, h) = screen()
                            Prefs.put("logo_fx", (lp.x.coerceIn(0, max(1, w - px)) * 1000f / max(1, w - px)).toInt())
                            Prefs.put("logo_fy", (lp.y.coerceIn(0, max(1, h - px)) * 1000f / max(1, h - px)).toInt())
                        }
                    }
                }
                return true
            }
        })
        v.visibility = if (hidden) View.GONE else View.VISIBLE
        wm.addView(v, lp)
        logo = v
    }

    // ---------------- feature panel ----------------
    private fun rounded(color: Int, r: Int, stroke: Int = 0) = GradientDrawable().apply {
        setColor(color); cornerRadius = r.toFloat(); if (stroke != 0) setStroke(dp(2), stroke)
    }

    private fun tv(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun btn(text: String, onClick: () -> Unit) = tv(text, 13f, 0xFF050B14.toInt(), true).apply {
        background = rounded(accent, dp(10)); gravity = Gravity.CENTER
        setPadding(dp(14), dp(7), dp(14), dp(7)); setOnClickListener { onClick() }
    }

    private fun togglePanel() { if (panel != null) closePanel() else openPanel() }

    private fun closePanel() {
        panel?.let { try { wm.removeView(it) } catch (e: Exception) { } }
        panel = null
    }

    private fun openPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(0xFF0B1728.toInt(), dp(20), accent)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(LogoView(this, dp(38), accent))
        head.addView(tv("  SS BEDROCK CLIENT", 16f, TEXT, true), LinearLayout.LayoutParams(0, WRAP, 1f))
        head.addView(tv("✕", 20f, TEXT).apply { setPadding(dp(12), dp(4), dp(12), dp(4)); setOnClickListener { closePanel() } })
        root.addView(head)

        val search = EditText(this).apply {
            hint = "Search feature"; setHintTextColor(TEXT2); setTextColor(TEXT); setSingleLine()
            background = rounded(0xFF10243D.toInt(), dp(12)); setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        root.addView(search, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val sv = ScrollView(this).apply { addView(list) }
        root.addView(sv, LinearLayout.LayoutParams(MATCH, 0, 1f).apply { topMargin = dp(8) })
        search.doAfterTextChanged { fillList(list, it?.toString() ?: "") }
        fillList(list, "")

        val foot = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        foot.addView(btn("Hide logo") { closePanel(); hidden = true; logo?.visibility = View.GONE; startFg() })
        foot.addView(btn("Stop overlay") { stopSelf() }, LinearLayout.LayoutParams(WRAP, WRAP).apply { leftMargin = dp(10) })
        root.addView(foot, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

        val (sw, sh) = screen()
        val lp = WindowManager.LayoutParams(
            (sw * 0.92f).toInt(), (sh * 0.78f).toInt(), TYPE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER; softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN }
        wm.addView(root, lp)
        panel = root
    }

    private fun fillList(list: LinearLayout, q: String) {
        list.removeAllViews()
        for (cat in Cat.values()) {
            val items = FEATURES.filter {
                it.cat == cat && (q.isBlank() || it.name.contains(q, true) || it.desc.contains(q, true))
            }
            if (items.isEmpty()) continue
            list.addView(tv("${cat.icon}  ${cat.en}", 14f, accent, true).apply { setPadding(0, dp(10), 0, dp(4)) })
            items.forEach { list.addView(row(it)) }
        }
    }

    private fun statusColor(s: Status) = when (s) {
        Status.AVAILABLE -> 0xFF22C55E.toInt()
        Status.LIMITED -> 0xFFF59E0B.toInt()
        Status.REQUIRES -> 0xFF55C7FF.toInt()
        Status.UNSUPPORTED -> 0xFFEF4444.toInt()
    }

    private fun row(f: Feature): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(0xFF10243D.toInt(), dp(12))
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(6) }
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(tv(f.name, 14f, TEXT, true), LinearLayout.LayoutParams(0, WRAP, 1f))
        top.addView(tv(f.status.label, 11f, statusColor(f.status), true))
        card.addView(top)
        card.addView(tv(f.desc, 12f, TEXT2))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        actions.addView(btn(if (f.tool == Tool.GUIDE) "Info" else "Settings") { openFeature(f) })
        f.hud?.let { name ->
            val sw = Switch(this).apply {
                isChecked = Prefs.bool("hud_$name")
                setOnCheckedChangeListener { _, c -> Prefs.put("hud_$name", c); applyHuds() }
            }
            actions.addView(sw, LinearLayout.LayoutParams(WRAP, WRAP).apply { leftMargin = dp(12) })
        }
        card.addView(actions, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(6) })
        return card
    }

    private fun openFeature(f: Feature) {
        closePanel()
        try {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("feature", f.id)
            )
        } catch (e: Exception) { toast(this, "Could not open SS Client: ${e.message}") }
    }

    // ---------------- HUD overlays ----------------
    private fun applyHuds() {
        huds.values.forEach { try { wm.removeView(it) } catch (e: Exception) { } }
        huds.clear()
        val base = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        if (Prefs.bool("hud_crosshair")) {
            val s = dp(Prefs.int("ch_size", 28))
            val lp = WindowManager.LayoutParams(s, s, TYPE, base, PixelFormat.TRANSLUCENT).apply {
                gravity = Gravity.CENTER; x = dp(Prefs.int("ch_x", 0)); y = dp(Prefs.int("ch_y", 0))
            }
            val v = CrosshairView(this); wm.addView(v, lp); huds["crosshair"] = v
        }
        if (Prefs.bool("hud_dvd")) {
            val lp = WindowManager.LayoutParams(MATCH, MATCH, TYPE, base, PixelFormat.TRANSLUCENT)
            val v = DvdView(this); wm.addView(v, lp); huds["dvd"] = v
        }
        if (Prefs.bool("hud_clock")) {
            val v = tv("", 14f, TEXT, true).apply {
                background = rounded(0x99050B14.toInt(), dp(10)); setPadding(dp(10), dp(4), dp(10), dp(4))
            }
            val lp = WindowManager.LayoutParams(WRAP, WRAP, TYPE, base, PixelFormat.TRANSLUCENT)
                .apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = dp(8) }
            wm.addView(v, lp); huds["clock"] = v
            val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val r = object : Runnable {
                override fun run() {
                    if (huds["clock"] !== v) return
                    v.text = fmt.format(Date())
                    handler.postDelayed(this, 1000)
                }
            }
            r.run()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        closePanel()
        huds.values.forEach { try { wm.removeView(it) } catch (e: Exception) { } }
        huds.clear()
        logo?.let { try { wm.removeView(it) } catch (e: Exception) { } }
        logo = null
        running = false
        super.onDestroy()
    }
}
