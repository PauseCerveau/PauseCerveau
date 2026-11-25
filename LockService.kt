package fr.pausecerveau

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings

/**
 * Service de premier plan : compte le temps d'écran (seulement quand l'écran est allumé)
 * et affiche l'écran de pause quand le temps est écoulé.
 */
class LockService : Service() {

    companion object {
        const val ACTION_START = "fr.pausecerveau.START"
        const val ACTION_STOP = "fr.pausecerveau.STOP"
        const val ACTION_LOCK_NOW = "fr.pausecerveau.LOCK_NOW"
        private const val CHANNEL = "pause_cerveau"
        private const val NOTIF_ID = 1

        fun send(ctx: Context, action: String = ACTION_START) {
            ctx.startForegroundService(Intent(ctx, LockService::class.java).setAction(action))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: Prefs
    private lateinit var power: PowerManager
    private var overlay: LockOverlay? = null
    private var remaining = 0L
    private var lastTick = 0L
    private var ticks = 0
    private var shownMinutes = -1L

    private val ticker = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        power = getSystemService(Context.POWER_SERVICE) as PowerManager
        remaining = prefs.remainingMs
        createChannel()
        val n = buildNotification("Pause Cerveau est actif")
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                prefs.running = false
                prefs.locked = false
                overlay?.hide()
                handler.removeCallbacks(ticker)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_LOCK_NOW -> {
                prefs.lockCorrect = 0
                prefs.locked = true
            }
            else -> remaining = prefs.remainingMs
        }
        lastTick = SystemClock.elapsedRealtime()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
        return START_STICKY
    }

    private fun tick() {
        if (!prefs.running) {
            overlay?.hide()
            stopSelf()
            return
        }
        val now = SystemClock.elapsedRealtime()
        val dt = (now - lastTick).coerceIn(0L, 2000L)
        lastTick = now

        if (!Settings.canDrawOverlays(this)) {
            updateNotification("Autorisation manquante : ouvrez Pause Cerveau")
            return
        }

        if (prefs.locked) {
            if (overlay?.isShowing != true) showOverlay()
            return
        }

        // Le temps ne compte que lorsque l'écran est allumé.
        if (power.isInteractive) remaining -= dt
        if (++ticks % 10 == 0) prefs.remainingMs = remaining

        if (remaining <= 0) {
            prefs.remainingMs = 0
            prefs.lockCorrect = 0
            prefs.locked = true
            showOverlay()
            return
        }

        val minutes = (remaining + 59_999) / 60_000
        if (minutes != shownMinutes) {
            shownMinutes = minutes
            updateNotification("Prochaine pause dans $minutes min de temps d’écran")
        }
    }

    private fun showOverlay() {
        if (overlay == null) overlay = LockOverlay(this, prefs) { unlock() }
        runCatching { overlay?.show() }
        shownMinutes = -1
        updateNotification("C’est l’heure de la pause cerveau")
    }

    private fun unlock() {
        prefs.locked = false
        prefs.lockCorrect = 0
        remaining = prefs.intervalMs
        prefs.remainingMs = remaining
        overlay?.hide()
        lastTick = SystemClock.elapsedRealtime()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        if (prefs.running && !prefs.locked) prefs.remainingMs = remaining
        overlay?.hide()
        super.onDestroy()
    }

    // ---------- Notification (obligatoire pour un service en arrière-plan) ----------

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Minuteur Pause Cerveau", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Pause Cerveau")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(text))
    }
}
