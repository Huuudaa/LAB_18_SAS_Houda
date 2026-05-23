# LAB 18 : ViewModel et LiveData en Android (Version Enrichie)

Ce projet implémente le **LAB 18** d'Android Jetpack. L'application propose une interface de démonstration haut de gamme (Material 3, Dark Mode par défaut) permettant de comparer en temps réel l'ancienne architecture classique face à la nouvelle architecture réactive basée sur **ViewModel**, **LiveData** et **SavedStateHandle**.

---

## 🎯 Objectifs du LAB

1. **Comprendre l'impact des changements de configuration** : Analyser pourquoi et comment les variables d'instance simples d'une Activity sont détruites lors de la rotation de l'écran.
2. **Identifier les limites de `onSaveInstanceState()`** : Démontrer que cette méthode historique est limitée en taille de données, ne supporte pas le multithreading, et surcharge le code de l'Activity.
3. **Maîtriser ViewModel** : Comprendre comment le framework Android préserve le ViewModel au sein du `ViewModelStore` lors de la ré-instanciation de l'Activity.
4. **Appréhender LiveData** : Observer comment les données réactives et sensibles au cycle de vie (lifecycle-aware) notifient l'interface utilisateur uniquement lorsque celle-ci est active (STARTED/RESUMED).
5. **Utiliser les meilleures pratiques Jetpack 2026** : Utilisation de la version stable `2.10.0` et de `SavedStateHandle` pour résister à la destruction du processus par le système (Process Death).

---

## 🏗️ Architecture & Concepts Internes

L'application est structurée selon le patron de conception **MVVM** (Model-View-ViewModel) :

```mermaid
graph TD
    subgraph View (UI Layer)
        MA[MainActivity]
        AM[activity_main.xml]
    end
    
    subgraph ViewModel (Logic Layer)
        VM[CounterViewModel]
        LD[LiveData / MutableLiveData]
        SSH[SavedStateHandle]
    end
    
    MA -->|Observe getCount| LD
    MA -->|Appelle actions| VM
    VM -->|Met à jour| LD
    VM -->|Sauvegarde état| SSH
    AM -->|Liaison via ViewBinding| MA
```

### Concepts clés mis en évidence :
* **LifecycleOwner** : L'Activity implémente cette interface et sert de point d'ancrage pour l'observation des données LiveData.
* **Observer** : Enregistré auprès du LiveData, il reçoit la nouvelle valeur uniquement si l'Activity est active.
* **ViewModelStore** : Le conteneur interne d'Android qui conserve l'instance de `CounterViewModel` intacte à travers la destruction de l'Activity.
* **setValue vs postValue** : `setValue()` est exécuté immédiatement sur le Main Thread (utilisé pour les boutons), tandis que `postValue()` transmet les données depuis un thread secondaire vers le Main Thread (utilisé dans le test de thread background).

---

## 💻 Fonctionnalités de l'Interface Utilisateur (UI)

L'interface de l'application est divisée en plusieurs panneaux de contrôle interactifs :

1. **Sélecteur de Mode Dynamique** : Bascule entre le mode **Sans ViewModel (Classique)** et le mode **Avec ViewModel (Moderne)**.
2. **Affichage du Compteur** : Un grand compteur numérique interactif avec des boutons stylisés pour incrémenter, décrémenter et réinitialiser.
3. **Panneau de Scénarios** :
   - *Mode Classique* : Un switch pour activer/désactiver la sauvegarde manuelle `onSaveInstanceState()`.
   - *Mode ViewModel* : Un bouton pour exécuter une tâche asynchrone en tâche de fond avec indicateur visuel de chargement (ProgressBar), et un panneau pour copier la commande de test de **Process Death**.
4. **Moniteur d'Instances en Direct** :
   - Affiche en temps réel le Hashcode mémoire de l'instance d'**Activity** actuelle (ex: `MainActivity@e14ab2`).
   - Affiche le Hashcode de l'instance du **ViewModel** (ex: `CounterViewModel@89ca12`).
5. **Terminal de Logs de Cycle de Vie** : Une console noire qui enregistre et affiche au fur et à mesure tous les callbacks système exécutés (`onCreate()`, `onStart()`, `onSaveInstanceState()`, `onDestroy()`, etc.).

---

## 🛠️ Configuration du Projet

### Dépendances Gradle (Module : app)
Le projet utilise Jetpack Lifecycle **2.10.0** et active le **ViewBinding** :

```kotlin
// build.gradle.kts (Section android)
buildFeatures {
    viewBinding = true
}

// build.gradle.kts (Section dependencies)
implementation(libs.lifecycle.viewmodel)
implementation(libs.lifecycle.livedata)
implementation(libs.lifecycle.viewmodel.savedstate)
```

---

## 🧪 Guide de Test des Scénarios Réels

### Scénario 1 : Rotation sans ViewModel (Comportement Classique défaillant)
1. Activez le mode **Sans ViewModel**.
2. Décochez l'option **Activer onSaveInstanceState()**.
3. Incrémentez le compteur jusqu'à **10**.
4. Tournez l'écran du simulateur (`Ctrl + F11` ou `Ctrl + F12`).
5. **Résultat** : Le compteur retombe à **0**. La console de logs montre que la première instance d'Activity a été détruite (`onDestroy`) et qu'une nouvelle instance avec un nouveau Hashcode a été créée (`onCreate`). La variable locale `count` a été effacée.

### Scénario 2 : Rotation avec onSaveInstanceState() (Solution Limitée)
1. Restez en mode **Sans ViewModel**.
2. Cochez l'option **Activer onSaveInstanceState()**.
3. Incrémentez le compteur jusqu'à **7**.
4. Tournez l'écran.
5. **Résultat** : Le compteur affiche toujours **7** (restauré avec succès).
6. **Limitation visible** : La console de logs affiche le passage par `onSaveInstanceState()` puis `onRestoreInstanceState()`. Les types primitifs sont préservés, mais si une opération asynchrone était en cours, elle a été interrompue ou a causé une fuite mémoire.

### Scénario 3 : Rotation parfaite avec ViewModel + LiveData
1. Basculez sur le mode **Avec ViewModel**.
2. Incrémentez le compteur à **15**.
3. Tournez l'écran plusieurs fois.
4. **Résultat** : Le compteur reste intact à **15** sans avoir besoin de code de restauration dans l'Activity.
5. **Observation du Moniteur** : Le Hashcode de `MainActivity` change à chaque rotation (nouvelle instance), mais celui de `CounterViewModel` reste rigoureusement le même ! Le ViewModel est resté en mémoire.

### Scénario 4 : Thread Background asynchrone (postValue)
1. En mode **Avec ViewModel**, cliquez sur le bouton **Thread Background (+1)**.
2. Une barre de progression apparaît pendant **1 seconde** (simulant un téléchargement ou appel API).
3. Tournez rapidement l'écran *pendant* cette seconde.
4. **Résultat** : L'écran tourne, l'Activity se reconstruit, et dès que la seconde est écoulée, le compteur s'incrémente de 1 de manière sécurisée sans crash.
5. **Explication technique** : La tâche de fond envoie la donnée via `postValue()`. LiveData, sensible au cycle de vie, attend que la nouvelle instance de l'Activity soit à l'état `STARTED`/`RESUMED` pour lui notifier la modification de valeur.

### Scénario 5 : Résistance au Process Death (SavedStateHandle)
1. En mode **Avec ViewModel**, incrémentez le compteur à **25**.
2. Copiez la commande affichée sur l'application dans votre terminal de développement :
   ```bash
   adb shell am kill com.example.lab_18_sas_houda
   ```
3. Exécutez la commande pour tuer le processus de l'application en arrière-plan.
4. Relancez l'application depuis le lanceur Android.
5. **Résultat** : Bien que le processus ait été entièrement tué par le système (effaçant le ViewModel de la RAM), le compteur affiche toujours **25**. Le `SavedStateHandle` a sauvegardé et restauré la donnée automatiquement via le mécanisme persistant du système d'exploitation.

---

## 📊 Tableau Comparatif

| Critère | Version SANS ViewModel (Partie 1) | Version AVEC ViewModel + LiveData (Partie 2) |
| :--- | :--- | :--- |
| **Survie à la Rotation** | ❌ Perdue (Sauf si sauvegardée manuellement) |  Survit nativement (grâce au `ViewModelStore`) |
| **Méthode de Restauration** | `onSaveInstanceState(Bundle)` | `SavedStateHandle` (Automatique) |
| **Mise à Jour UI** | ❌ Manuelle (Risque d'oublis ou d'incohérences) |  Automatique et réactive (via `LiveData.observe()`) |
| **Gestion Multi-thread** | ❌ Risque de crashs/leaks si l'Activity meurt |  Totalement sécurisée (via `postValue()`) |
| **Sensibilité au Cycle de Vie** | ❌ Non (Mise à jour en arrière-plan possible) |  Oui (Pas de notification si l'Activity est inactive) |
| **Architecture du Code** | ❌ Code mélangé (Logique métier + UI dans l'Activity)|  Code propre et modulaire (MVVM) |
| **Support de Données Complexes**| ❌ Restreint aux types primitifs du Bundle |  Compatible avec tout type de données complexes / Objects |

---

## 🏁 Conclusion

Ce LAB démontre de manière concrète pourquoi le couple **ViewModel + LiveData** est devenu le standard absolu d'architecture sous Android depuis l'avènement des composants Jetpack. Il résout les plus grands défis du développement mobile Android : la gestion des configurations d'écran et la sécurité vis-à-vis du cycle de vie de l'application.
