package com.gestiontaches.service;

import com.gestiontaches.model.Tache;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère les opérations métier liées aux tâches.
 * Responsabilité unique : gestion de la liste des tâches.
 */
public class GestionTaches {

    private List<Tache> taches;

    public GestionTaches() {
        this.taches = new ArrayList<>();
    }

    public void ajouterTache(String titre, String description) {
        taches.add(new Tache(titre, description));
    }

    public void marquerTacheCommeTerminee(int index) {
        if (index >= 0 && index < taches.size()) {
            taches.get(index).marquerCommeTerminee();
        }
    }

    public void basculerTache(int index) {
        if (index >= 0 && index < taches.size()) {
            taches.get(index).basculerTerminee();
        }
    }

    public List<Tache> getTaches() {
        return taches;
    }
}
