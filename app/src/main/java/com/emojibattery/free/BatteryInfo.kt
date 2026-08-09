package com.emojibattery.free

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatteryInfo(val level: Int, val charging: Boolean) {

    companion object {
        fun from(intent: Intent): BatteryInfo {
            val raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val level = if (raw >= 0 && scale > 0) raw * 100 / scale else 0
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            return BatteryInfo(level, charging)
        }

        /** Přečte poslední „sticky“ stav baterie bez registrace posluchače. */
        fun current(context: Context): BatteryInfo {
            val sticky = context.registerReceiver(
                null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            return sticky?.let { from(it) } ?: BatteryInfo(0, false)
        }
    }
}
