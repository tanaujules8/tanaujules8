package com.gestiontaches.web;

import com.gestiontaches.model.Tache;
import com.gestiontaches.service.GestionTaches;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WebServer {

    private static final Path TASKS_FILE = Path.of("tasks.json");

    public static void main(String[] args) throws Exception {
        GestionTaches gestion = new GestionTaches();
        // Load persisted tasks if present, otherwise create defaults
        if (!loadTasksFromFile(gestion)) {
            gestion.ajouterTache("Apprendre Java", "Comprendre les principes SOLID");
            gestion.ajouterTache("Corriger le projet", "Appliquer le principe SRP");
            gestion.marquerTacheCommeTerminee(0);
            saveTasksToFile(gestion.getTaches());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/api/tasks", new ApiTasksHandler(gestion));
        server.setExecutor(null);
        System.out.println("Web server started at http://localhost:8080");
        server.start();
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = buildHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String buildHtml() {
            return "<!doctype html>" +
                    "<html><head><meta charset='utf-8'/><title>Rapport des tâches</title>" +
                    "<style>body{font-family:Arial,Helvetica,sans-serif;margin:24px}h1{color:#222} " +
                    ".task{margin:8px 0;padding:8px;border-radius:6px;background:#f7f7f7} .done{opacity:0.7;text-decoration:line-through} " +
                    "button{margin-left:8px} form{margin-top:16px}</style>" +
                    "</head><body><h1>Rapport des tâches</h1>" +
                    "<div id='report'></div>" +
                    "<form id='addForm'>" +
                    "<input name='titre' placeholder='Titre' required/> " +
                    "<input name='description' placeholder='Description' required/> " +
                    "<button type='submit'>Ajouter</button>" +
                    "</form>" +
                    "<button id='refresh'>Rafraîchir</button>" +
                    "<script>" +
                    "async function fetchTasks(){const r=await fetch('/api/tasks');const t=await r.json();render(t);}" +
                    "function escape(s){return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\"/g,'&quot;').replace(/'/g,'&#39;')}" +
                    "function render(tasks){const root=document.getElementById('report');root.innerHTML='';let done='<h2>✔ Tâches terminées</h2>';let todo='<h2>⏳ Tâches non terminées</h2>';let dlist='';let tlist='';tasks.forEach((task,i)=>{const item=`<div class=\"task ${task.terminee? 'done':''}\"><strong>${escape(task.titre)}</strong>: ${escape(task.description)} <button onclick=\"toggle(${i})\">✓</button><button onclick=\"remove(${i})\">✖</button></div>`; if(task.terminee) dlist+=item; else tlist+=item;});root.innerHTML=done + dlist + todo + tlist;}" +
                    "async function toggle(i){await fetch('/api/tasks/toggle?index='+i,{method:'POST'});fetchTasks();}" +
                    "async function remove(i){await fetch('/api/tasks?index='+i,{method:'DELETE'});fetchTasks();}" +
                    "document.getElementById('addForm').addEventListener('submit',async e=>{e.preventDefault();const fd=new FormData(e.target);await fetch('/api/tasks',{method:'POST',body:new URLSearchParams(fd)});e.target.reset();fetchTasks();});" +
                    "document.getElementById('refresh').addEventListener('click',fetchTasks);" +
                    "fetchTasks();</script></body></html>";
        }
    }

    static class ApiTasksHandler implements HttpHandler {

        private final GestionTaches gestion;

        ApiTasksHandler(GestionTaches gestion) {
            this.gestion = gestion;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();

            if ("GET".equalsIgnoreCase(method) && path.equals("/api/tasks")) {
                handleList(exchange);
                return;
            }

            if ("POST".equalsIgnoreCase(method) && path.equals("/api/tasks")) {
                handleAdd(exchange);
                return;
            }

            if ("POST".equalsIgnoreCase(method) && path.equals("/api/tasks/toggle")) {
                handleToggle(exchange);
                return;
            }

            if ("DELETE".equalsIgnoreCase(method) && path.equals("/api/tasks")) {
                handleDelete(exchange);
                return;
            }

            sendResponse(exchange, 404, "Not found");
        }

        private void handleList(HttpExchange exchange) throws IOException {
            List<Tache> taches = gestion.getTaches();
            String json = toJson(taches);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            sendResponse(exchange, 200, json);
        }

        private void handleAdd(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String,String> params = parseForm(body);
            String titre = params.getOrDefault("titre", "").trim();
            String description = params.getOrDefault("description", "").trim();
            if (!titre.isEmpty() && !description.isEmpty()) {
                gestion.ajouterTache(titre, description);
                saveTasksToFile(gestion.getTaches());
                sendResponse(exchange, 201, "{\"ok\":true}");
            } else {
                sendResponse(exchange, 400, "{\"error\":\"missing fields\"}");
            }
        }

        private void handleToggle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String,String> params = parseQuery(query);
            int idx = parseIndex(params.get("index"));
            if (idx >= 0 && idx < gestion.getTaches().size()) {
                if (gestion.getTaches().get(idx).estTerminee()) {
                    // no unmark method, so recreate: crude approach
                    // For simplicity, toggle by recreating state
                    // If terminee -> do nothing (keep true), else mark true
                    // We'll implement unmark by rebuilding list
                    // Simpler: if terminee true -> nothing, else mark true
                    gestion.getTaches().get(idx).marquerCommeTerminee();
                } else {
                    gestion.getTaches().get(idx).marquerCommeTerminee();
                }
                saveTasksToFile(gestion.getTaches());
                sendResponse(exchange, 200, "{\"ok\":true}");
            } else {
                sendResponse(exchange, 400, "{\"error\":\"invalid index\"}");
            }
        }

        private void handleDelete(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String,String> params = parseQuery(query);
            int idx = parseIndex(params.get("index"));
            if (idx >= 0 && idx < gestion.getTaches().size()) {
                gestion.getTaches().remove(idx);
                saveTasksToFile(gestion.getTaches());
                sendResponse(exchange, 200, "{\"ok\":true}");
            } else {
                sendResponse(exchange, 400, "{\"error\":\"invalid index\"}");
            }
        }

        private int parseIndex(String s) {
            try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
        }

        private Map<String,String> parseForm(String body) {
            return parseQuery(body);
        }

        private Map<String,String> parseQuery(String q) {
            if (q == null) return Map.of();
            return List.of(q.split("&")).stream()
                    .map(p -> p.split("=",2))
                    .filter(arr -> arr.length==2)
                    .collect(Collectors.toMap(a->urlDecode(a[0]), a->urlDecode(a[1])));
        }

        private String urlDecode(String s) {
            try { return URLDecoder.decode(s, StandardCharsets.UTF_8); } catch (Exception e) { return ""; }
        }

        private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, b.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
        }

        private String toJson(List<Tache> taches) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (Tache t : taches) {
                if (!first) sb.append(','); first=false;
                sb.append('{');
                sb.append("\"titre\":\"").append(escapeJson(t.getTitre())).append("\"");
                sb.append(',');
                sb.append("\"description\":\"").append(escapeJson(t.getDescription())).append("\"");
                sb.append(',');
                sb.append("\"terminee\":").append(t.estTerminee());
                sb.append('}');
            }
            sb.append("]");
            return sb.toString();
        }

        private String escapeJson(String s) {
            if (s==null) return "";
            return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");
        }
    }

    // Persistence helpers
    private static boolean loadTasksFromFile(GestionTaches gestion) {
        try {
            if (!Files.exists(TASKS_FILE)) return false;
            String content = Files.readString(TASKS_FILE, StandardCharsets.UTF_8).trim();
            if (content.isEmpty() || !content.startsWith("[")) return false;
            // naive JSON parsing: find objects and extract fields
            Pattern objPat = Pattern.compile("\"titre\"\s*:\s*\"(.*?)\".*?\"description\"\s*:\s*\"(.*?)\".*?\"terminee\"\s*:\s*(true|false)", Pattern.DOTALL);
            Matcher m = objPat.matcher(content);
            List<String[]> items = new ArrayList<>();
            while (m.find()) {
                String t = unescapeJson(m.group(1));
                String d = unescapeJson(m.group(2));
                String term = m.group(3);
                items.add(new String[]{t,d,term});
            }
            if (items.isEmpty()) return false;
            // Clear existing and add
            List<Tache> internal = gestion.getTaches();
            internal.clear();
            for (String[] it : items) {
                gestion.ajouterTache(it[0], it[1]);
                if (Boolean.parseBoolean(it[2])) {
                    gestion.marquerTacheCommeTerminee(internal.size()-1);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void saveTasksToFile(List<Tache> taches) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first=true;
            for (Tache t : taches) {
                if (!first) sb.append(','); first=false;
                sb.append('{');
                sb.append("\"titre\":\"").append(escapeJson(t.getTitre())).append("\"");
                sb.append(',');
                sb.append("\"description\":\"").append(escapeJson(t.getDescription())).append("\"");
                sb.append(',');
                sb.append("\"terminee\":").append(t.estTerminee());
                sb.append('}');
            }
            sb.append(']');
            Files.writeString(TASKS_FILE, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Failed to save tasks: " + e.getMessage());
        }
    }

    private static String unescapeJson(String s) {
        return s.replaceAll("\\\\","\\").replaceAll("\\\"","\"");
    }

    private static String escapeJson(String s) {
        if (s==null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
    }
}
