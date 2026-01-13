package com.gestiontaches.model;

/**
 * Représente une tâche.
 * Responsabilité unique : stocker les données d'une tâche.
 */
public class Tache {

    private String titre;
    private String description;
    private boolean terminee;

    public Tache(String titre, String description) {
        this.titre = titre;
        this.description = description;
        this.terminee = false;
    }

    public void marquerCommeTerminee() {
        this.terminee = true;
    }

    public void definirTerminee(boolean terminee) {
        this.terminee = terminee;
    }

    public void basculerTerminee() {
        this.terminee = !this.terminee;
    }

    public boolean estTerminee() {
        return terminee;
    }

    public String getTitre() {
        return titre;
    }

    public String getDescription() {
        return description;
    }
}
