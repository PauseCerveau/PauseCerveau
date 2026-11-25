package fr.pausecerveau

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView

/**
 * Écran de pause affiché par-dessus TOUTES les applications.
 * Il disparaît après 2 bonnes réponses, ou avec le code parent.
 */
class LockOverlay(
    private val ctx: Context,
    private val prefs: Prefs,
    private val onUnlocked: () -> Unit
) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var root: View? = null
    private var correct = 0
    private var lastKey: String? = null

    private val blue = Color.parseColor("#2F5DFF")
    private val ink = Color.parseColor("#1E2A5A")
    private val green = Color.parseColor("#1FA463")
    private val red = Color.parseColor("#E5386D")

    private lateinit var star1: TextView
    private lateinit var star2: TextView
    private lateinit var question: TextView
    private lateinit var visual: TextView
    private lateinit var grid: GridLayout
    private lateinit var feedback: TextView
    private lateinit var quizBox: LinearLayout
    private lateinit var doneBox: LinearLayout
    private lateinit var pinBox: LinearLayout
    private lateinit var pinInput: EditText
    private lateinit var pinError: TextView

    val isShowing: Boolean get() = root != null

    fun show() {
        if (root != null) return
        correct = prefs.lockCorrect
        val view = build()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        )
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        wm.addView(view, params)
        root = view
        updateStars()
        nextQuestion()
    }

    fun hide() {
        handler.removeCallbacksAndMessages(null)
        root?.let { runCatching { wm.removeView(it) } }
        root = null
    }

    // ---------- Construction de l'écran ----------

    private fun dp(v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
        if (stroke != null) setStroke(dp(2), stroke)
    }

    private fun label(s: String, sizeSp: Float, bold: Boolean = false) = TextView(ctx).apply {
        text = s
        textSize = sizeSp
        setTextColor(Color.WHITE)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun below(topDp: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(topDp) }

    private fun pill(text: String, bg: Int, fg: Int, stroke: Int? = null) = Button(ctx).apply {
        this.text = text
        isAllCaps = false
        textSize = 16f
        setTextColor(fg)
        stateListAnimator = null
        background = rounded(bg, dp(22), stroke)
        setPadding(dp(18), dp(8), dp(18), dp(8))
    }

    private fun build(): View {
        // Le bouton Retour est ignoré pendant la pause.
        val frame = object : FrameLayout(ctx) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) return true
                return super.dispatchKeyEvent(event)
            }
        }
        frame.setBackgroundColor(blue)

        val scroll = ScrollView(ctx).apply { isFillViewport = true }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(48), dp(24), dp(32))
        }

        // En-tête : étoiles + bouton Parent
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        star1 = label("⭐", 30f)
        star2 = label("⭐", 30f).apply { setPadding(dp(6), 0, 0, 0) }
        header.addView(star1)
        header.addView(star2)
        header.addView(Space(ctx), LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(pill("Parent", Color.TRANSPARENT, Color.WHITE, Color.argb(110, 255, 255, 255)).apply {
            setOnClickListener { showPin(pinBox.visibility != View.VISIBLE) }
        })
        col.addView(header)

        // Code parent (caché par défaut)
        pinBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            background = rounded(Color.WHITE, dp(18))
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        pinBox.addView(label("Code parent", 18f, true).apply { setTextColor(ink) })
        pinInput = EditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "••••"
            setTextColor(ink)
            textSize = 22f
        }
        pinBox.addView(pinInput, below(8))
        pinError = label("", 15f, true).apply { setTextColor(red) }
        pinBox.addView(pinError)
        val pinRow = LinearLayout(ctx).apply { gravity = Gravity.END }
        pinRow.addView(pill("Annuler", Color.TRANSPARENT, ink, Color.LTGRAY).apply {
            setOnClickListener { showPin(false) }
        })
        pinRow.addView(Space(ctx), LinearLayout.LayoutParams(dp(10), 1))
        pinRow.addView(pill("Débloquer", blue, Color.WHITE).apply { setOnClickListener { checkPin() } })
        pinBox.addView(pinRow, below(8))
        col.addView(pinBox, below(16))

        // Questions
        quizBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val who = if (prefs.name.isBlank()) "C’est" else "${prefs.name}, c’est"
        quizBox.addView(label("$who l’heure de la pause cerveau ! Réponds juste à 2 questions pour continuer.", 17f), below(20))
        question = label("", 26f, true)
        quizBox.addView(question, below(16))
        visual = label("", 36f)
        quizBox.addView(visual, below(8))
        grid = GridLayout(ctx).apply { columnCount = 2 }
        quizBox.addView(grid, below(12))
        feedback = label("", 18f, true)
        quizBox.addView(feedback, below(16))
        col.addView(quizBox)

        // Bravo
        doneBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
        }
        doneBox.addView(label("Bravo !", 44f, true))
        doneBox.addView(label("Deux bonnes réponses. Tu peux continuer.", 19f), below(8))
        col.addView(doneBox, below(80))

        scroll.addView(col)
        frame.addView(scroll)
        return frame
    }

    // ---------- Logique du quiz ----------

    private fun updateStars() {
        star1.alpha = if (correct >= 1) 1f else 0.3f
        star2.alpha = if (correct >= 2) 1f else 0.3f
    }

    private fun nextQuestion() {
        val q = Questions.next(prefs.age, lastKey)
        lastKey = q.text + q.visual
        question.text = q.text
        visual.text = q.visual
        visual.visibility = if (q.visual.isEmpty()) View.GONE else View.VISIBLE
        feedback.text = ""
        grid.removeAllViews()

        val buttons = q.options.map { opt ->
            Button(ctx).apply {
                text = opt
                isAllCaps = false
                textSize = 20f
                setTextColor(ink)
                stateListAnimator = null
                background = rounded(Color.WHITE, dp(18))
                minHeight = dp(76)
            }
        }
        buttons.forEach { b ->
            b.setOnClickListener { onAnswer(b, b.text.toString() == q.answer, q.answer, buttons) }
            val lp = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply {
                width = 0
                setMargins(dp(6), dp(6), dp(6), dp(6))
            }
            grid.addView(b, lp)
        }
    }

    private fun onAnswer(chosen: Button, ok: Boolean, good: String, all: List<Button>) {
        all.forEach {
            it.isEnabled = false
            if (it.text.toString() == good) {
                it.background = rounded(green, dp(18)); it.setTextColor(Color.WHITE)
            }
        }
        if (ok) {
            correct++
            prefs.lockCorrect = correct
            updateStars()
            if (correct >= 2) {
                handler.postDelayed({ showDone() }, 700)
            } else {
                feedback.text = "Bonne réponse ! Encore une."
                handler.postDelayed({ nextQuestion() }, 1200)
            }
        } else {
            chosen.background = rounded(red, dp(18)); chosen.setTextColor(Color.WHITE)
            feedback.text = "Pas tout à fait : la bonne réponse était « $good ». Nouvelle question !"
            handler.postDelayed({ nextQuestion() }, 2200)
        }
    }

    private fun showDone() {
        quizBox.visibility = View.GONE
        pinBox.visibility = View.GONE
        doneBox.visibility = View.VISIBLE
        handler.postDelayed({ onUnlocked() }, 1800)
    }

    // ---------- Code parent ----------

    private fun showPin(visible: Boolean) {
        pinBox.visibility = if (visible) View.VISIBLE else View.GONE
        pinError.text = ""
        pinInput.setText("")
        val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (visible) {
            pinInput.requestFocus()
            pinInput.post { imm.showSoftInput(pinInput, InputMethodManager.SHOW_IMPLICIT) }
        } else {
            imm.hideSoftInputFromWindow(pinInput.windowToken, 0)
        }
    }

    private fun checkPin() {
        if (pinInput.text.toString() == prefs.pin) {
            showPin(false)
            onUnlocked()
        } else {
            pinError.text = "Code incorrect. Réessayez."
        }
    }
}
