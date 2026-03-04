# Évolution de l'application TODO

Ce document décrit les étapes pour faire évoluer l'application de gestion de tâches (TODO app) en une application robuste, maintenable et riche en fonctionnalités, en suivant les meilleures pratiques de développement Android.

## Objectif

L'objectif est de refactoriser l'application existante pour intégrer une architecture moderne (MVVM), de nouvelles fonctionnalités, et d'assurer sa qualité à travers des tests unitaires.

---

## 1. Architecture MVVM (Model-View-ViewModel)

L'architecture MVVM est un patron de conception qui sépare la logique métier de l'interface utilisateur.

- **View (Vue)** : Représente l'UI (Activity, Fragment). Son rôle est d'afficher les données et de notifier le ViewModel des interactions utilisateur. La vue observe les changements de données dans le ViewModel.
- **ViewModel** : Contient la logique de présentation et gère l'état de la vue. Il expose les données à la vue via des `LiveData` ou des `StateFlow`. Le ViewModel ne doit jamais avoir de référence directe à la vue (Activity/Fragment).
- **Model (Modèle)** : Représente les données et la logique métier de l'application. Il est géré via un `Repository` qui abstrait les sources de données.

**Action :**
- Créez un `ViewModel` pour chaque écran (par exemple, `TaskListViewModel`, `TaskDetailViewModel`).
- Utilisez `Flow` pour exposer les données de manière réactive.
- Assurez-vous que les vues (composables) observent ces données et mettent à jour l'UI en conséquence.

---

## 2. Module de Données Dédié et Repository

Pour une meilleure séparation des responsabilités, la gestion des données doit être isolée dans son propre module Gradle.

- **Créer un module de données** : Créez un nouveau module Android Library (par exemple, `:data`).

- **Pattern Repository** : Le `Repository` est le seul point d'entrée pour accéder aux données de l'application. Il est responsable de récupérer les données depuis différentes sources (base de données, réseau, cache en mémoire) et de les fournir au reste de l'application.

- **Visibilité `internal`** : Les implémentations concrètes des sources de données (`RoomDatabase`, `InMemoryDataSource`) doivent avoir une visibilité `internal`. Seules les interfaces (par exemple, `TaskDataSource`) et le `Repository` seront publics. Cela garantit que la couche UI ne peut pas accéder directement aux sources de données.

**Action :**
1. Créez un nouveau module `:data`.
2. Définissez une interface `TaskRepository`.
3. Implémentez `TaskRepositoryImpl` dans ce module.
4. Rendez les classes de base de données et autres sources de données `internal`.

---

## 3. Sources de Données : InMemory, Room et FireStore

L'application devra gérer trois sources de données interchangeables.

>> Ajouter une menu dans la toolbar permettant de changer de source de données

- **InMemoryDataSource** : Une source de données simple qui stocke les tâches en mémoire. Utile pour les tests et le développement rapide. C'est probablement ce que vous avez déjà.

- **RoomDataSource** : Une source de données persistante utilisant la bibliothèque [Room](https://developer.android.com/training/data-storage/room). Room est un ORM (Object-Relational Mapper) qui facilite la gestion d'une base de données SQLite.

- **FirestoreDataSource** : Une source de données distante utilisant [Cloud Firestore](https://firebase.google.com/docs/firestore) pour synchroniser les tâches en temps réel entre les appareils.

**Action :**
1. Définissez une interface commune `TaskDataSource`.
2. Créez `InMemoryTaskDataSource` et `RoomTaskDataSource` qui implémentent cette interface.
3. Intégrez la dépendance Room.
4. Créez l'entité `Task` (`@Entity`), le DAO `TaskDao` (`@Dao`) et la base de données `AppDatabase` (`@Database`).
5. Le `Repository` utilisera l'une de ces sources de données, idéalement choisie via l'injection de dépendances.

---

## 4. Nouvelles Fonctionnalités

Pour enrichir l'application, plusieurs fonctionnalités seront ajoutées.

- **Internationalisation** : Utiliser les fichiers de ressources (strings.xml), pour traduire votre application en plusieurs langues (français/anglais).

- **Recherche de tâches** : Ajoutez une barre de recherche pour filtrer les tâches par titre ou description.

- **Gestion d'adresses** : Permettez à l'utilisateur d'associer des coordonnées GPS à une tâche.

- **Ajout de photos** : Permettez à l'utilisateur de joindre une ou plusieurs photos à une tâche, prises avec l'appareil photo ou sélectionnées depuis la galerie. Les photos seront concervées en local.

- **Système de rappel** : Mettez en place un système de rappels personnalisables pour chaque tâche.

- **Affichage sur une carte** : Affichez les tâches qui ont des coordonnées GPS sur une carte (par exemple, en utilisant Google Maps).

- **Deeplinks** : Implémentez des deeplinks pour permettre d'ouvrir directement une tâche spécifique depuis une URL ou une notification.


## 5. Notifications

Les notifications rappelleront à l'utilisateur l'échéance d'une tâche.

- **Date limite** : Ajoutez un champ `dueDate` (date limite) à votre modèle de tâche.

- **WorkManager** : Utilisez [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) pour planifier une tâche en arrière-plan qui vérifiera périodiquement les tâches approchant de leur date limite.

- **NotificationManager** : Lorsque `WorkManager` détecte une tâche pertinente, il déclenche une notification pour l'utilisateur.

---

## 6. Idées de Fonctionnalités Supplémentaires

Pour aller plus loin, voici quelques idées :

- **Thèmes** : Ajout d'un mode sombre (Dark Theme).
- **Catégories/Tags** : Permettre à l'utilisateur de classer ses tâches par catégories.
- **Priorités** : Définir des niveaux de priorité pour les tâches (haute, moyenne, basse).
- **Partage** : Partager une tâche via d'autres applications (email, SMS, etc.).

---

## 7. Tests Unitaires

Les tests sont essentiels pour garantir la qualité et la stabilité de l'application.

- **ViewModel Tests** : Testez la logique du `ViewModel`. Vérifiez que les `Flow` sont mis à jour correctement en réponse aux événements.

- **Repository Tests** : Testez le `Repository` en utilisant des "fakes" (fausses implémentations) des sources de données pour s'assurer qu'il gère correctement les données.

- **Room DAO Tests** : Testez vos requêtes `TaskDao` pour vous assurer qu'elles fonctionnent comme prévu.

**Action :**
- Ajoutez les dépendances de test (JUnit, Mockito/MockK, Turbine pour les Flows).
- Écrivez des tests unitaires pour chaque `ViewModel` et pour le `Repository`.
