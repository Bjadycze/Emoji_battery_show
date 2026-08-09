package com.emojibattery.free

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Přijímač musí být exportovaný, aby mu systém doručil BOOT_COMPLETED.
 * Obě akce jsou chráněné broadcasty, takže je cizí aplikace nemůže podvrhnout.
 * Explicitním intentem na komponentu ale zvenčí zaklepat lze, proto akci
 * kontrolujeme a start služby balíme do runCatching – jinak by pokus o start
 * z pozadí shodil aplikaci výjimkou ForegroundServiceStartNotAllowedException.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        if (!Prefs(context).enabled) return
        runCatching { BatteryMonitorService.start(context) }
    }
}
