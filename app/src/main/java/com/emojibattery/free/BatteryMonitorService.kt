package com.emojibattery.free

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat

class BatteryMonitorService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefs: Prefs
    private var info = BatteryInfo(0, false)
    private var bubble: TextView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var postedChannel: String? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val next = BatteryInfo.from(intent)
            if (next != info) {
                info = next
                refresh()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        prefs.register(this)
        createChannels()

        val sticky = ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        sticky?.let { info = BatteryInfo.from(it) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channel = channelFor(prefs.mode)
        ServiceCompat.startForeground(
            this, NOTIF_ID, buildNotification(channel), foregroundType()
        )
        postedChannel = channel
        refresh()
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(batteryReceiver) }
        prefs.unregister(this)
        removeBubble()
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        if (key == Prefs.KEY_OVERLAY_X || key == Prefs.KEY_OVERLAY_Y) return
        refresh()
    }

    // --- vykreslení všech výstupů ------------------------------------------------

    private fun refresh() {
        val mode = prefs.mode
        if (!mode.running) {
            stopSelf()
            return
        }
        val channel = channelFor(mode)
        val notification = buildNotification(channel)

        if (postedChannel != null && postedChannel != channel) {
            // Kanál u již vydaného oznámení nelze změnit, je nutné ho vydat znovu.
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            ServiceCompat.startForeground(this, NOTIF_ID, notification, foregroundType())
        } else {
            getSystemService(NotificationManager::class.java).notify(NOTIF_ID, notification)
        }
        postedChannel = channel

        syncBubble()
        BatteryWidgetProvider.renderAll(this, info, prefs)
    }

    private fun channelFor(mode: Mode): String =
        if (mode.showsIcon) CHANNEL_ID else CHANNEL_SILENT

    private fun foregroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        val visible = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        // IMPORTANCE_MIN = oznámení zůstane v roletce, ale ve stavovém řádku se ikona neukáže.
        val silent = NotificationChannel(
            CHANNEL_SILENT,
            getString(R.string.channel_silent_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.channel_silent_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        manager.createNotificationChannel(visible)
        manager.createNotificationChannel(silent)
    }

    private fun buildNotification(channel: String): android.app.Notification {
        val set = prefs.emojiSet
        val glyph = set.forState(info, prefs.chargingEmoji)
        val charging = info.charging && prefs.chargingEmoji
        val icon = when {
            prefs.numberIcon -> {
                if (set.drawn) EmojiRenderer.batteryWithNumber(info.level, charging, ICON_PX, colored = false)
                else EmojiRenderer.emojiWithNumber(glyph, info.level, ICON_PX)
            }
            set.drawn -> EmojiRenderer.battery(info.level, charging, ICON_PX, colored = false)
            else -> EmojiRenderer.emoji(glyph, ICON_PX)
        }

        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val state = if (info.charging) getString(R.string.state_charging)
        else getString(R.string.state_discharging)

        return NotificationCompat.Builder(this, channel)
            .setSmallIcon(IconCompat.createWithBitmap(icon))
            .setContentTitle("$glyph  ${info.level} %")
            .setContentText("$state · ${getString(set.nameRes)}")
            .setContentIntent(open)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    // --- plovoucí bublina --------------------------------------------------------

    private fun syncBubble() {
        val wanted = prefs.mode.showsBubble && Settings.canDrawOverlays(this)
        if (!wanted) {
            removeBubble()
            return
        }
        if (bubble == null) addBubble()
        val set = prefs.emojiSet
        val glyph = set.forState(info, prefs.chargingEmoji)
        bubble?.apply {
            text = bubbleText(set, glyph)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.overlaySize.toFloat())
            background = ContextCompat.getDrawable(context, R.drawable.bubble_bg)
                ?.mutate()
                ?.apply { alpha = prefs.overlayOpacity.coerceIn(0, 100) * 255 / 100 }
        }
    }

    /** U kreslené sady se do textu vloží bitmapa, jinak stačí emoji. */
    private fun bubbleText(set: EmojiSet, glyph: String): CharSequence {
        val suffix = if (prefs.overlayShowPercent) " ${info.level}%" else ""
        if (!set.drawn) return glyph + suffix

        val px = (prefs.overlaySize * resources.displayMetrics.scaledDensity * 1.5f).toInt()
        val bitmap = EmojiRenderer.battery(
            info.level, info.charging && prefs.chargingEmoji, px, colored = true
        )
        val drawable = BitmapDrawable(resources, bitmap).apply { setBounds(0, 0, px, px) }
        val text = SpannableString("\u200B$suffix")
        text.setSpan(
            ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM),
            0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return text
    }

    private fun addBubble() {
        val wm = getSystemService(WindowManager::class.java) ?: return
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.overlayX
            y = prefs.overlayY
        }

        val tv = TextView(this).apply {
            setTextColor(Color.WHITE)
            setShadowLayer(6f, 0f, 1f, Color.BLACK)
            setPadding(24, 12, 24, 12)
        }
        attachDrag(tv, wm, lp)

        runCatching { wm.addView(tv, lp) }
            .onSuccess { bubble = tv; bubbleParams = lp }
    }

    private fun attachDrag(view: View, wm: WindowManager, lp: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x; startY = lp.y
                    touchX = event.rawX; touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (event.rawX - touchX).toInt()
                    lp.y = startY + (event.rawY - touchY).toInt()
                    runCatching { wm.updateViewLayout(view, lp) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    prefs.overlayX = lp.x
                    prefs.overlayY = lp.y
                    true
                }
                else -> false
            }
        }
    }

    private fun removeBubble() {
        bubble?.let { v ->
            runCatching { getSystemService(WindowManager::class.java)?.removeView(v) }
        }
        bubble = null
        bubbleParams = null
    }

    companion object {
        const val CHANNEL_ID = "battery_status"
        const val CHANNEL_SILENT = "battery_status_silent"
        const val NOTIF_ID = 1001
        private const val ICON_PX = 96

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, BatteryMonitorService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BatteryMonitorService::class.java))
        }
    }
}
