# Waka

Application Android de gestion de projets personnels et professionnels, **100 % hors-ligne** : aucun backend, aucune API externe. Toute la logique (budget, dépendances entre tâches, sécurité) tourne en local, stockée dans une base Room chiffrée sur l'appareil.

## Le principe

- **Un projet** = un nom, une échéance, et un statut privé ou non. Il ne porte jamais de montant directement.
- **Un objectif** = une tâche budgétée à l'intérieur d'un projet (titre + montant cible dans la devise de son choix). C'est la somme de ses objectifs qui donne le coût total d'un projet.
- **Les versements** s'accumulent sur un objectif jusqu'à atteindre son montant cible, qui se complète alors automatiquement, pas de case à cocher manuelle pour les objectifs budgétés.
- **Un versement qui dépasse** ce qu'il reste sur l'objectif visé déborde automatiquement sur les objectifs suivants du même projet ; s'il dépasse ce qu'il reste sur l'ensemble d'entre eux, il est refusé en bloc avec un message clair.
- **Le taux de change est fixe et non modifiable** : 1 € = 655 XAF. Toute conversion affichée (accueil, agrégats, versements) applique ce taux, jamais l'inverse d'une somme déjà convertie.
- **Les dépendances entre objectifs** verrouillent visuellement une tâche tant que son prérequis n'est pas complété (avec détection de cycle à la création).
- **Les projets privés** sont protégés par code PIN et/ou empreinte digitale : masqués (`•••`) sur l'accueil jusqu'à révélation individuelle, et verrouillés à l'ouverture.
- **Les rappels** (AlarmManager, survivent au redémarrage) se programment directement sur un projet ou un objectif, pas de bloc-notes libre dans l'app.

## Stack technique

| Domaine | Choix |
|---|---|
| Langage / UI | Kotlin, Jetpack Compose (Material 3) |
| Navigation | Navigation Compose, routes type-safe (`kotlinx.serialization`) |
| Persistance | Room (SQLite), migration destructive tant que le schéma évolue en dev |
| Préférences | DataStore (devise d'affichage), `EncryptedSharedPreferences` (PIN, biométrie) |
| Sécurité | Hash PIN en PBKDF2 + sel, `BiometricPrompt`, verrouillage temporaire anti-brute-force |
| Async / état | Coroutines, `StateFlow`, MVVM |
| Rappels | `AlarmManager` + `BroadcastReceiver`, notification avec son personnalisé |
| DI | Conteneur manuel (`AppContainer`), pas de Hilt |

## Architecture du code

```
app/src/main/java/com/propentatech/waka/
├── data/
│   ├── local/            entités Room, DAOs, Converters, WakaDatabase
│   ├── prefs/             AppPreferences (DataStore, devise d'affichage)
│   └── repository/        ProjectRepository, ReminderRepository
├── domain/                logique métier pure, sans dépendance Android :
│                           ProjectProgressCalculator, ProjectAggregator,
│                           DependencyEngine (verrouillage + anti-cycle),
│                           ContributionCascade (répartition des versements),
│                           SavingsEstimator, ReminderTiming
├── security/               PinHasher, SecurityPreferences, PinAuthController,
│                           BiometricAuthenticator
├── notifications/          ReminderScheduler, ReminderReceiver, BootReceiver,
│                           NotificationHelper
├── model/                  Currency (+ conversion), RepeatType
├── ui/
│   ├── components/         composants partagés (WakaFormSheet, WakaField,
│   │                       WakaSegmentedControl, AuthGateSheet, WakaTopBar…)
│   ├── format/              formatage d'affichage (montants, échéances)
│   ├── navigation/          Route (routes type-safe)
│   ├── screens/             un dossier par écran (home, projectdetail, lock,
│   │                       reminders, settings), chacun Screen + ViewModel
│   └── theme/                palette dérivée du logo (navy / vert / or / rouge)
├── AppContainer.kt          conteneur de dépendances (instanciation paresseuse)
├── WakaApplication.kt
└── MainActivity.kt          NavHost + barre de navigation basse
```

`ProjectDetailScreen` est un seul écran récursif : il s'auto-adapte pour afficher soit la liste des objectifs d'un projet, soit le détail budgétaire d'un objectif, selon le nœud ouvert.

## Modèle de données (Room)

Un seul arbre auto-référencé (`ProjectItem`, `parentId` nul = projet racine) porte projets et objectifs. Autour de lui :

- **Contribution**, un versement, rattaché à un `ProjectItem`, dans sa propre devise.
- **TaskDependency**, `taskId` dépend de `dependsOnTaskId` ; table séparée de l'arbre parent/enfant.
- **Reminder**, toujours rattaché à un `ProjectItem` (projet ou objectif), jamais de note libre.

Aucun champ « complété » n'est stocké pour un objectif budgété : sa complétion se déduit à la lecture (`somme des versements ≥ montant cible`), pour n'avoir qu'une seule source de vérité.

## Prise en main

**Prérequis** : Android Studio récent, JDK 17+, SDK Android avec la plateforme 37 installée.

```bash
./gradlew assembleDebug     # compiler
./gradlew installDebug      # installer sur un appareil/émulateur connecté
```

`minSdk` 24 (Android 7.0), `compileSdk`/`targetSdk` 37/36.

## Sécurité

- PIN haché en PBKDF2 + sel aléatoire (jamais stocké en clair), lui-même conservé dans `EncryptedSharedPreferences` (Android Keystore).
- Verrouillage temporaire après 5 échecs, partagé entre l'écran de déverrouillage d'un projet et la ré-authentification des Paramètres.
- Modifier/supprimer le PIN, ou (dés)activer la biométrie, exige une ré-authentification préalable.
- Les projets privés sont masqués sur l'accueil ; chaque révélation est individuelle et ne survit pas à la navigation.
