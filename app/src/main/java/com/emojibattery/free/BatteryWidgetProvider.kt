package com.emojibattery.free

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

class BatteryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        render(context, manager, ids, BatteryInfo.current(context), Prefs(context))
    }

    companion object {
        private const val EMOJI_PX = 192

        fun renderAll(context: Context, info: BatteryInfo, prefs: Prefs) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, BatteryWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) render(context, manager, ids, info, prefs)
        }

        fun render(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            info: BatteryInfo,
            prefs: Prefs
        ) {
            val set = prefs.emojiSet
            val glyph = set.forState(info, prefs.chargingEmoji)
            val bitmap = if (set.drawn) {
                EmojiRenderer.battery(
                    info.level, info.charging && prefs.chargingEmoji, EMOJI_PX, colored = true
                )
            } else {
                EmojiRenderer.emoji(glyph, EMOJI_PX)
            }

            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val views = RemoteViews(context.packageName, R.layout.widget_battery).apply {
                setImageViewBitmap(R.id.widget_emoji, bitmap)
                setTextViewText(R.id.widget_percent, "${info.level} %")
                setViewVisibility(
                    R.id.widget_bolt,
                    if (info.charging) View.VISIBLE else View.GONE
                )
                setOnClickPendingIntent(R.id.widget_root, open)
            }
            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }
}
