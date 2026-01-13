# Application de Gestion de Tâches (Java)

## Description
Application Java simple permettant de gérer une liste de tâches.
Le projet respecte le principe de responsabilité unique (SRP) du modèle SOLID.

## Fonctionnalités
- Ajouter une tâche
- Marquer une tâche comme terminée
- Générer un rapport des tâches terminées et non terminées

## Architecture
- `model` : représentation des données
- `service` : logique métier
- `report` : génération des rapports
- `Application` : point d’entrée

## Exécution
```bash
javac com/gestiontaches/Application.java
java com.gestiontaches.Application
