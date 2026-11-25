# Pause Cerveau (Android)

Après le temps d’écran choisi (15 min par défaut), un écran bleu recouvre **toutes les applications**.
L’enfant doit répondre juste à **2 questions** adaptées à son âge pour continuer.
Une mauvaise réponse affiche la bonne et une nouvelle question arrive.
Le temps ne compte que quand l’écran est allumé.

## Installer l’application

1. Installez **Android Studio** (gratuit) : https://developer.android.com/studio
2. Ouvrez Android Studio > **Open** > choisissez le dossier `PauseCerveau`.
   Laissez-le télécharger ce qu’il faut (quelques minutes la première fois).
3. Sur la tablette : Paramètres > À propos > tapez 7 fois sur « Numéro de build »
   pour activer le mode développeur, puis activez **Débogage USB**.
4. Branchez la tablette en USB et cliquez sur ▶ **Run**.

Sans câble : menu **Build > Build App Bundle(s) / APK(s) > Build APK(s)**,
puis copiez le fichier `app-debug.apk` sur la tablette et ouvrez-le
(il faudra autoriser « Installer des applis inconnues »).

## Premier lancement (parent)

1. Autorisez les notifications.
2. Touchez **Ouvrir l’autorisation** et activez « Afficher par-dessus les autres applis ».
3. Touchez **Désactiver l’économie de batterie** et choisissez « Ne pas optimiser »
   (sinon certaines tablettes, Samsung et Xiaomi surtout, arrêtent le minuteur).
4. Remplissez prénom, âge, intervalle et code parent, puis **Démarrer**.
5. Essayez **Tester une pause maintenant**.

Le bouton **Parent** sur l’écran de pause permet de débloquer avec le code.

## Limites à connaître

- Entre deux pauses, un enfant débrouillé peut aller dans Paramètres et retirer
  l’autorisation ou désinstaller l’appli. Pour l’en empêcher, utilisez
  **Google Family Link** (gratuit) pour bloquer l’accès aux Paramètres et au Play Store.
- Ne fonctionne pas sur iPhone/iPad : Apple n’autorise pas ce type de blocage.

## Modifier les questions

Tout est dans `app/src/main/java/fr/pausecerveau/Questions.kt`.
Pour ajouter une question fixe, copiez une ligne comme :
`{ fixed("Ta question ?", "Bonne réponse", "Faux 1", "Faux 2", "Faux 3") },`
