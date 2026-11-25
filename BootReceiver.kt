package fr.pausecerveau

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Relance le minuteur après un redémarrage de la tablette. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs(context).running) {
            LockService.send(context)
        }
    }
}
