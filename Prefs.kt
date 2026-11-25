package fr.pausecerveau

import android.content.Context

/** Réglages et état de l'application, conservés sur l'appareil. */
class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("pause_cerveau", Context.MODE_PRIVATE)

    var name: String
        get() = p.getString("name", "") ?: ""
        set(v) = p.edit().putString("name", v).apply()

    /** Tranche d'âge : 4 (4–6), 7 (7–9), 10 (10–12), 13 (13+). */
    var age: Int
        get() = p.getInt("age", 7)
        set(v) = p.edit().putInt("age", v).apply()

    /** Temps d'écran autorisé entre deux pauses, en millisecondes. */
    var intervalMs: Long
        get() = p.getLong("interval_ms", 15 * 60_000L)
        set(v) = p.edit().putLong("interval_ms", v).apply()

    var pin: String
        get() = p.getString("pin", "") ?: ""
        set(v) = p.edit().putString("pin", v).apply()

    var running: Boolean
        get() = p.getBoolean("running", false)
        set(v) = p.edit().putBoolean("running", v).commit().let { }

    /** Temps d'écran restant avant la prochaine pause. */
    var remainingMs: Long
        get() = p.getLong("remaining_ms", intervalMs)
        set(v) = p.edit().putLong("remaining_ms", v).apply()

    /** L'écran est-il actuellement bloqué ? (survit à un redémarrage) */
    var locked: Boolean
        get() = p.getBoolean("locked", false)
        set(v) = p.edit().putBoolean("locked", v).commit().let { }

    /** Bonnes réponses déjà données pendant la pause en cours. */
    var lockCorrect: Int
        get() = p.getInt("lock_correct", 0)
        set(v) = p.edit().putInt("lock_correct", v).apply()
}
