package fr.pausecerveau

import kotlin.random.Random

data class Question(
    val text: String,
    val answer: String,
    val options: List<String>,
    val visual: String = ""
)

/** Banque de questions par tranche d'âge (calculs générés + questions fixes). */
object Questions {
    private fun r(a: Int, b: Int) = Random.nextInt(a, b + 1)

    /** La bonne réponse et 3 nombres proches, mélangés. */
    private fun near(ans: Int, spread: Int, n: Int = 3): List<String> {
        val s = linkedSetOf(ans)
        var guard = 0
        while (s.size < n + 1 && guard++ < 200) {
            val v = ans + r(-spread, spread)
            if (v >= 0) s.add(v)
        }
        var k = 1
        while (s.size < n + 1) s.add(ans + spread + k++)
        return s.map { it.toString() }.shuffled()
    }

    private fun calc(text: String, ans: Int, spread: Int, visual: String = "") =
        Question(text, ans.toString(), near(ans, spread), visual)

    private fun fixed(text: String, ans: String, vararg wrong: String) =
        Question(text, ans, (listOf(ans) + wrong).shuffled())

    private val bank: Map<Int, List<() -> Question>> = mapOf(
        4 to listOf<() -> Question>(
            { val a = r(0, 5); val b = r(1, 4); calc("Combien font $a + $b ?", a + b, 2) },
            {
                val n = r(2, 8)
                val e = listOf("🍎", "🐟", "⭐", "🚗", "🐞", "🎈").random()
                calc("Combien en vois-tu ?", n, 2, e.repeat(n))
            },
            {
                val a = r(1, 9); var b = r(1, 9); while (b == a) b = r(1, 9)
                Question("Quel nombre est le plus grand ?", maxOf(a, b).toString(),
                    listOf(a.toString(), b.toString()).shuffled())
            },
            {
                val a = r(3, 9); val b = r(1, a)
                calc("J’ai $a bonbons, j’en mange $b. Combien en reste-t-il ?", a - b, 2)
            },
            { fixed("Quel animal fait « miaou » ?", "Le chat", "Le chien", "La vache", "Le canard") },
            { fixed("Combien de pattes a un chien ?", "4", "2", "6", "8") },
            { fixed("Quelle forme a 3 côtés ?", "Le triangle", "Le carré", "Le rond", "L’étoile") },
            { fixed("Que prend-on quand il pleut ?", "Un parapluie", "Des lunettes de soleil", "Un maillot de bain", "Une pelle") },
            { fixed("Quel est le petit de la vache ?", "Le veau", "Le poussin", "Le chaton", "L’agneau") },
            { fixed("De quelle couleur est le ciel quand il fait beau ?", "Bleu", "Vert", "Rose", "Marron") },
            { fixed("Combien de jours y a-t-il dans une semaine ?", "7", "5", "10", "3") }
        ),
        7 to listOf<() -> Question>(
            { val a = r(12, 60); val b = r(5, 39); calc("Combien font $a + $b ?", a + b, 10) },
            { val a = r(30, 99); val b = r(5, a - 5); calc("Combien font $a − $b ?", a - b, 10) },
            { val a = r(2, 5); val b = r(2, 10); calc("Combien font $a × $b ?", a * b, 5) },
            { val n = r(6, 45); calc("Quel est le double de $n ?", n * 2, 6) },
            { fixed("Quelle est la capitale de la France ?", "Paris", "Lyon", "Marseille", "Montpellier") },
            { fixed("Combien de mois y a-t-il dans une année ?", "12", "10", "11", "14") },
            { fixed("Quel est le pluriel de « cheval » ?", "Des chevaux", "Des chevals", "Des chevaus", "Des chevalles") },
            { fixed("Lequel est un mammifère ?", "La baleine", "Le requin", "Le thon", "La pieuvre") },
            { fixed("Combien de minutes y a-t-il dans une heure ?", "60", "100", "30", "24") },
            { fixed("Lequel de ces mots est un verbe ?", "Courir", "Maison", "Rapide", "Soleil") },
            { fixed("Quel est le contraire de « grand » ?", "Petit", "Gros", "Haut", "Long") },
            { fixed("Sur quelle planète vivons-nous ?", "La Terre", "Mars", "La Lune", "Vénus") }
        ),
        10 to listOf<() -> Question>(
            { val a = r(6, 9); val b = r(3, 10); calc("Combien font $a × $b ?", a * b, 8) },
            { val b = r(3, 9); val q = r(3, 12); calc("Combien font ${b * q} ÷ $b ?", q, 3) },
            { val n = r(4, 40) * 2; calc("Quelle est la moitié de $n ?", n / 2, 6) },
            {
                val a = r(12, 99); val m = listOf(10, 100).random()
                val opts = linkedSetOf(a * m, a * m * 10, a * m / 10, a + m).map { it.toString() }.shuffled()
                Question("Combien font $a × $m ?", (a * m).toString(), opts)
            },
            { fixed("Quel est le plus long fleuve de France ?", "La Loire", "La Seine", "Le Rhône", "La Garonne") },
            { fixed("Combien de côtés a un hexagone ?", "6", "5", "7", "8") },
            { fixed("Quel organe pompe le sang dans le corps ?", "Le cœur", "Les poumons", "Le foie", "Le cerveau") },
            { fixed("Quelle est la capitale de l’Espagne ?", "Madrid", "Barcelone", "Lisbonne", "Séville") },
            { fixed("Quel est le participe passé de « prendre » ?", "Pris", "Prendu", "Prit", "Prené") },
            { fixed("En quelle année commence la Révolution française ?", "1789", "1515", "1914", "1848") },
            { fixed("Quel gaz les plantes absorbent-elles pour vivre ?", "Le dioxyde de carbone", "L’hélium", "L’oxygène pur", "L’azote") },
            { fixed("Quel océan borde l’ouest de la France ?", "L’océan Atlantique", "L’océan Pacifique", "L’océan Indien", "La mer du Nord") }
        ),
        13 to listOf<() -> Question>(
            {
                val p = listOf(10, 20, 25, 50).random(); val n = r(2, 20) * 20
                calc("Combien font $p % de $n ?", n * p / 100, 10)
            },
            { val x = r(2, 30); val a = r(3, 40); calc("Résous : x + $a = ${x + a}", x, 5) },
            { val x = r(2, 12); val a = r(2, 9); calc("Résous : ${a}x = ${a * x}", x, 3) },
            { val n = r(11, 16); calc("Combien fait ${n}² ?", n * n, 15) },
            { fixed("Quelle est la formule chimique de l’eau ?", "H₂O", "CO₂", "O₂", "NaCl") },
            { fixed("Qui a écrit « Les Misérables » ?", "Victor Hugo", "Émile Zola", "Molière", "Jules Verne") },
            { fixed("Quelle est la capitale de l’Australie ?", "Canberra", "Sydney", "Melbourne", "Perth") },
            { fixed("Que vaut la somme des angles d’un triangle ?", "180°", "90°", "360°", "270°") },
            { fixed("Quelle est la plus petite unité du vivant ?", "La cellule", "L’atome", "L’organe", "Le tissu") },
            { fixed("En quelle année l’homme a-t-il marché sur la Lune ?", "1969", "1959", "1975", "1989") },
            { fixed("Quel mot est un synonyme de « rapide » ?", "Véloce", "Lent", "Lourd", "Calme") }
        )
    )

    /** Une nouvelle question pour cet âge, différente de la précédente. */
    fun next(age: Int, previousKey: String?): Question {
        val list = bank[age] ?: bank.getValue(7)
        var q: Question
        var guard = 0
        do {
            q = list.random()()
        } while (q.text + q.visual == previousKey && guard++ < 10)
        return q
    }
}
