package com.gestiontaches.web;

import com.gestiontaches.model.Tache;
import com.gestiontaches.service.GestionTaches;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WebServer {

    public static void main(String[] args) throws Exception {
        GestionTaches gestionTaches = new GestionTaches();

        gestionTaches.ajouterTache("Apprendre Java", "Comprendre les principes SOLID");
        gestionTaches.ajouterTache("Corriger le projet", "Appliquer le principe SRP");
        gestionTaches.marquerTacheCommeTerminee(0);

        List<Tache> taches = gestionTaches.getTaches();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new ReportHandler(taches));
        server.setExecutor(null);
        System.out.println("Web server started at http://localhost:8080");
        server.start();
    }

    static class ReportHandler implements HttpHandler {

        private final List<Tache> taches;

        ReportHandler(List<Tache> taches) {
            this.taches = taches;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("<!doctype html><html><head><meta charset='utf-8'/><title>Rapport des tâches</title>");
            sb.append("<style>body{font-family:Arial,Helvetica,sans-serif;margin:24px}h1{color:#222}</style>");
            sb.append("</head><body>");
            sb.append("<h1>Rapport des tâches</h1>");

            sb.append("<h2>✔ Tâches terminées</h2><ul>");
            for (Tache t : taches) {
                if (t.estTerminee()) {
                    sb.append("<li><strong>").append(escape(t.getTitre())).append("</strong>: ")
                      .append(escape(t.getDescription())).append("</li>");
                }
            }
            sb.append("</ul>");

            sb.append("<h2>⏳ Tâches non terminées</h2><ul>");
            for (Tache t : taches) {
                if (!t.estTerminee()) {
                    sb.append("<li><strong>").append(escape(t.getTitre())).append("</strong>: ")
                      .append(escape(t.getDescription())).append("</li>");
                }
            }
            sb.append("</ul>");

            sb.append("</body></html>");

            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String escape(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;")
                    .replace(">", "&gt;").replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }
    }
}
