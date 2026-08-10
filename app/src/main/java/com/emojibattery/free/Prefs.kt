package com.emojibattery.free

import android.content.Context
import android.content.SharedPreferences

enum class Mode(val id: String, val labelRes: Int, val hintRes: Int) {
    OFF("off", R.string.mode_off, R.string.mode_off_hint),
    STATUS("status", R.string.mode_status, R.string.mode_status_hint),
    BOTH("both", R.string.mode_both, R.string.mode_both_hint),
    BUBBLE("bubble", R.string.mode_bubble, R.string.mode_bubble_hint);

    val running: Boolean get() = this != OFF
    val showsIcon: Boolean get() = this == STATUS || this == BOTH
    val showsBubble: Boolean get() = this == BOTH || this == BUBBLE

    companion object {
        fun from(id: String?): Mode = entries.firstOrNull { it.id == id } ?: OFF
    }
}

class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("emoji_battery", Context.MODE_PRIVATE)

    var mode: Mode
        get() = Mode.from(sp.getString(KEY_MODE, Mode.OFF.id))
        set(v) = sp.edit().putString(KEY_MODE, v.id).apply()

    val enabled: Boolean get() = mode.running

    var setId: String
        get() = sp.getString(KEY_SET, "meter") ?: "meter"
        set(v) = sp.edit().putString(KEY_SET, v).apply()

    /** Ve stavovém řádku ukázat číslo místo siluety emoji. */
    var numberIcon: Boolean
        get() = sp.getBoolean(KEY_NUMBER_ICON, false)
        set(v) = sp.edit().putBoolean(KEY_NUMBER_ICON, v).apply()

    var chargingEmoji: Boolean
        get() = sp.getBoolean(KEY_CHARGING, true)
        set(v) = sp.edit().putBoolean(KEY_CHARGING, v).apply()

    var overlayShowPercent: Boolean
        get() = sp.getBoolean(KEY_OVERLAY_PERCENT, true)
        set(v) = sp.edit().putBoolean(KEY_OVERLAY_PERCENT, v).apply()

    /** Velikost textu bubliny v sp. */
    var overlaySize: Int
        get() = sp.getInt(KEY_OVERLAY_SIZE, 22)
        set(v) = sp.edit().putInt(KEY_OVERLAY_SIZE, v).apply()

    /** Neprůhlednost pozadí bubliny v procentech (25–100). */
    var overlayOpacity: Int
        get() = sp.getInt(KEY_OVERLAY_OPACITY, 60)
        set(v) = sp.edit().putInt(KEY_OVERLAY_OPACITY, v).apply()

    var overlayX: Int
        get() = sp.getInt(KEY_OVERLAY_X, 24)
        set(v) = sp.edit().putInt(KEY_OVERLAY_X, v).apply()

    var overlayY: Int
        get() = sp.getInt(KEY_OVERLAY_Y, 240)
        set(v) = sp.edit().putInt(KEY_OVERLAY_Y, v).apply()

    val emojiSet: EmojiSet get() = EmojiSets.byId(setId)

    fun register(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.registerOnSharedPreferenceChangeListener(l)

    fun unregister(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.unregisterOnSharedPreferenceChangeListener(l)

    companion object {
        const val KEY_MODE = "mode"
        const val KEY_SET = "set_id"
        const val KEY_NUMBER_ICON = "number_icon"
        const val KEY_CHARGING = "charging_emoji"
        const val KEY_OVERLAY_PERCENT = "overlay_percent"
        const val KEY_OVERLAY_SIZE = "overlay_size"
        const val KEY_OVERLAY_OPACITY = "overlay_opacity"
        const val KEY_OVERLAY_X = "overlay_x"
        const val KEY_OVERLAY_Y = "overlay_y"
    }
}
