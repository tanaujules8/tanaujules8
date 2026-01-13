package com.gestiontaches;

import com.gestiontaches.report.RapportTaches;
import com.gestiontaches.service.GestionTaches;

/**
 * Point d'entrée de l'application.
 * Responsabilité unique : orchestration.
 */
public class Application {

    public static void main(String[] args) {

        GestionTaches gestionTaches = new GestionTaches();
        RapportTaches rapportTaches = new RapportTaches();

        gestionTaches.ajouterTache(
                "Apprendre Java",
                "Comprendre les principes SOLID"
        );

        gestionTaches.ajouterTache(
                "Corriger le projet",
                "Appliquer le principe SRP"
        );

        gestionTaches.marquerTacheCommeTerminee(0);

        rapportTaches.genererRapport(gestionTaches.getTaches());
    }
}
