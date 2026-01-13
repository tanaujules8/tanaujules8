package com.gestiontaches.report;

import com.gestiontaches.model.Tache;
import java.util.List;

/**
 * Génère des rapports sur les tâches.
 * Responsabilité unique : affichage et reporting.
 */
public class RapportTaches {

    public void genererRapport(List<Tache> taches) {

        System.out.println("=================================");
        System.out.println("      RAPPORT DES TÂCHES");
        System.out.println("=================================");

        System.out.println("\n✔ Tâches terminées :");
        for (Tache tache : taches) {
            if (tache.estTerminee()) {
                afficherTache(tache);
            }
        }

        System.out.println("\n⏳ Tâches non terminées :");
        for (Tache tache : taches) {
            if (!tache.estTerminee()) {
                afficherTache(tache);
            }
        }
    }

    private void afficherTache(Tache tache) {
        System.out.println("- " + tache.getTitre() + " : " + tache.getDescription());
    }
}
