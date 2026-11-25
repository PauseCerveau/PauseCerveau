package fr.pausecerveau

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Écran des parents : réglages, autorisations, démarrage et arrêt. */
class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private var settingsUnlocked = false

    private val ages = listOf(4 to "4–6 ans", 7 to "7–9 ans", 10 to "10–12 ans", 13 to "13 ans et +")
    private val intervals = listOf(
        15 * 60_000L to "15 minutes",
        30 * 60_000L to "30 minutes",
        45 * 60_000L to "45 minutes",
        20_000L to "Test (20 secondes)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onStop() {
        super.onStop()
        settingsUnlocked = false // il faut redonner le code à chaque fois
    }

    private fun render() {
        if (prefs.running && !settingsUnlocked) showStatus() else showSettings()
    }

    // ---------- Petits outils d'interface ----------

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun page(build: LinearLayout.() -> Unit) {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(40), dp(24), dp(32))
            build()
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#FFF8E1"))
            addView(col)
        })
    }

    private fun LinearLayout.text(s: String, size: Float, bold: Boolean = false, top: Int = 12) =
        TextView(this@MainActivity).apply {
            text = s
            textSize = size
            setTextColor(Color.parseColor("#1E2A5A"))
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }.also { addView(it, lp(top)) }

    private fun LinearLayout.button(s: String, top: Int = 12, onClick: () -> Unit) =
        Button(this@MainActivity).apply {
            text = s
            isAllCaps = false
            textSize = 17f
            setOnClickListener { onClick() }
        }.also { addView(it, lp(top)) }

    private fun lp(top: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    // ---------- Écran « actif » ----------

    private fun showStatus() = page {
        text("🧠 Pause Cerveau", 28f, true, 0)
        val who = prefs.name.ifBlank { "votre enfant" }
        val min = prefs.intervalMs / 60_000
        val every = if (min >= 1) "$min minutes" else "${prefs.intervalMs / 1000} secondes"
        text("Pause Cerveau est actif pour $who : une pause toutes les $every de temps d’écran.", 17f)
        if (!Settings.canDrawOverlays(this@MainActivity)) {
            text("⚠️ L’autorisation « Afficher par-dessus les autres applis » est désactivée. L’écran de pause ne peut pas s’afficher.", 16f, true)
            button("Rétablir l’autorisation") { openOverlaySettings() }
        }
        button("Tester une pause maintenant", 28) { LockService.send(this@MainActivity, LockService.ACTION_LOCK_NOW) }
        button("Réglages (code parent)") {
            askPin { settingsUnlocked = true; render() }
        }
    }

    // ---------- Écran des réglages ----------

    private fun showSettings() = page {
        text("🧠 Pause Cerveau", 28f, true, 0)
        text("Après le temps d’écran choisi, toutes les applis sont recouvertes. Il faut répondre juste à 2 questions pour continuer.", 16f)

        text("Prénom de l’enfant", 16f, true, 24)
        val nameInput = EditText(this@MainActivity).apply { setText(prefs.name); hint = "Ex. : Léa" }
        addView(nameInput, lp(4))

        text("Âge", 16f, true, 20)
        val ageGroup = RadioGroup(this@MainActivity)
        val ageIds = ages.associate { (age, label) ->
            val rb = RadioButton(this@MainActivity).apply { id = View.generateViewId(); text = label; textSize = 16f }
            ageGroup.addView(rb)
            if (age == prefs.age) rb.isChecked = true
            rb.id to age
        }
        addView(ageGroup, lp(4))

        text("Pause toutes les…", 16f, true, 20)
        val intGroup = RadioGroup(this@MainActivity)
        val intIds = intervals.associate { (ms, label) ->
            val rb = RadioButton(this@MainActivity).apply { id = View.generateViewId(); text = label; textSize = 16f }
            intGroup.addView(rb)
            if (ms == prefs.intervalMs) rb.isChecked = true
            rb.id to ms
        }
        if (intGroup.checkedRadioButtonId == View.NO_ID) intGroup.check(intIds.keys.first())
        addView(intGroup, lp(4))

        text(if (prefs.pin.isEmpty()) "Code parent (4 chiffres)" else "Nouveau code parent (laisser vide pour garder l’actuel)", 16f, true, 20)
        val pinInput = EditText(this@MainActivity).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "••••"
        }
        addView(pinInput, lp(4))

        if (!Settings.canDrawOverlays(this@MainActivity)) {
            text("Étape obligatoire : autorisez Pause Cerveau à s’afficher par-dessus les autres applis.", 15f, true, 20)
            button("Ouvrir l’autorisation") { openOverlaySettings() }
        }
        button("Désactiver l’économie de batterie pour cette appli", 8) {
            safeStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }

        button(if (prefs.running) "Enregistrer et relancer" else "Démarrer", 24) {
            val pin = pinInput.text.toString().trim()
            when {
                pin.isEmpty() && prefs.pin.isEmpty() -> toast("Choisissez un code parent de 4 chiffres.")
                pin.isNotEmpty() && !Regex("\\d{4}").matches(pin) -> toast("Le code parent doit contenir exactement 4 chiffres.")
                !Settings.canDrawOverlays(this@MainActivity) -> {
                    toast("Autorisez d’abord l’affichage par-dessus les autres applis.")
                    openOverlaySettings()
                }
                ageGroup.checkedRadioButtonId == View.NO_ID -> toast("Choisissez l’âge de l’enfant.")
                else -> {
                    if (pin.isNotEmpty()) prefs.pin = pin
                    prefs.name = nameInput.text.toString().trim()
                    prefs.age = ageIds.getValue(ageGroup.checkedRadioButtonId)
                    prefs.intervalMs = intIds.getValue(intGroup.checkedRadioButtonId)
                    prefs.remainingMs = prefs.intervalMs
                    prefs.locked = false
                    prefs.lockCorrect = 0
                    prefs.running = true
                    LockService.send(this@MainActivity)
                    settingsUnlocked = false
                    toast("Pause Cerveau est lancé.")
                    render()
                }
            }
        }

        if (prefs.running) {
            button("Arrêter Pause Cerveau") {
                LockService.send(this@MainActivity, LockService.ACTION_STOP)
                settingsUnlocked = false
                toast("Pause Cerveau est arrêté.")
                window.decorView.postDelayed({ render() }, 300)
            }
        }
    }

    // ---------- Divers ----------

    private fun askPin(onOk: () -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "••••"
        }
        AlertDialog.Builder(this)
            .setTitle("Code parent")
            .setView(input)
            .setPositiveButton("Valider") { _, _ ->
                if (input.text.toString() == prefs.pin) onOk() else toast("Code incorrect.")
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun openOverlaySettings() {
        safeStart(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            toast("Ce réglage n’existe pas sur cet appareil. Cherchez-le dans Paramètres > Applications > Pause Cerveau.")
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
