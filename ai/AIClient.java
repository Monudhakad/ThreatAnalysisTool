// AIClient.java
// Simple heuristic 'AI' that reads cve-results.json and generates ai-analysis.json
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AIClient {
    public static void main(String[] args) throws Exception {
        Path in = Paths.get("workspace/output/cve-results.json");
        if (!Files.exists(in)) {
            System.out.println("Missing cve-results.json. Run cve_fetcher.py first.");
            System.exit(1);
        }
        String raw = new String(Files.readAllBytes(in));
        // naive parse: look for "id" occurrences - we'll generate simple issues array
        List<Map<String,Object>> issues = new ArrayList<>();
        String[] parts = raw.split("\\{");
        for (String p : parts) {
            if (p.contains("\"id\"")) {
                String id = extractField(p, "\"id\"");
                String summary = extractField(p, "\"summary\"");
                String cvssStr = extractField(p, "\"cvss\"");
                Double cvss = null;
                try { cvss = cvssStr != null ? Double.valueOf(cvssStr) : null; } catch (Exception ex) {}
                Map<String,Object> issue = new LinkedHashMap<>();
                issue.put("id", id != null ? id : "UNKNOWN");
                issue.put("title", id != null ? id : "Unknown vulnerability");
                issue.put("summary", summary != null ? summary : "");
                issue.put("cvss", cvss);
                String sev = "Medium";
                if (cvss != null) {
                    if (cvss >= 9.0) sev = "Critical";
                    else if (cvss >= 7.0) sev = "High";
                    else if (cvss >= 4.0) sev = "Medium";
                    else sev = "Low";
                }
                issue.put("severity", sev);
                issue.put("recommendation", "Patch to latest version or follow vendor guidance.");
                issues.add(issue);
            }
        }
        double overall = 0.0;
        for (Map<String,Object> it : issues) {
            Double c = (Double)it.get("cvss");
            overall += (c != null ? c : 5.0);
        }
        if (!issues.isEmpty()) overall = overall / issues.size();
        Map<String,Object> top = new LinkedHashMap<>();
        top.put("issues", issues);
        top.put("overall_score", Math.round(overall * 10.0)/10.0);
        top.put("notes", "This is a mock AI summary: replace with real LLM integration for production.");

        Path out = Paths.get("workspace/output/ai-analysis.json");
        Files.createDirectories(out.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            w.write(mapToJson(top));
        }
        System.out.println("Wrote " + out.toString());
    }

    private static String extractField(String src, String key) {
        int idx = src.indexOf(key);
        if (idx == -1) return null;
        int colon = src.indexOf(":", idx);
        if (colon == -1) return null;
        int start = colon + 1;
        // find next comma or closing brace
        int end = src.indexOf(",", start);
        int b = src.indexOf("}", start);
        if (end == -1 || (b != -1 && b < end)) end = b;
        if (end == -1) end = Math.min(src.length(), start+200);
        String val = src.substring(start, end).trim();
        // strip quotes
        if (val.startsWith("\"") && val.endsWith("\"")) val = val.substring(1, val.length()-1);
        // clean braces
        val = val.replaceAll("[\\}\\{\\\"]", "").trim();
        return val;
    }

    private static String mapToJson(Map<String,Object> m) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int i=0;
        for (Map.Entry<String,Object> e : m.entrySet()) {
            sb.append("  \"").append(escape(e.getKey())).append("\": ");
            if (e.getValue() instanceof List) {
                sb.append(listToJson((List<?>)e.getValue()));
            } else if (e.getValue() instanceof Number) {
                sb.append(String.valueOf(e.getValue()));
            } else {
                sb.append("\"").append(escape(String.valueOf(e.getValue()))).append("\"");
            }
            if (++i < m.size()) sb.append(",");
            sb.append("\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String listToJson(List<?> L) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i=0;i<L.size();i++) {
            Object o = L.get(i);
            if (o instanceof Map) {
                sb.append("    ").append(mapToJsonInline((Map<String,Object>)o));
            } else {
                sb.append("    \"").append(escape(String.valueOf(o))).append("\"");
            }
            if (i+1 < L.size()) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]");
        return sb.toString();
    }

    private static String mapToJsonInline(Map<String,Object> m) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int i=0;
        for (Map.Entry<String,Object> e : m.entrySet()) {
            sb.append("\"").append(escape(e.getKey())).append("\":");
            if (e.getValue() instanceof Number) sb.append(e.getValue());
            else sb.append("\"").append(escape(String.valueOf(e.getValue()))).append("\"");
            if (++i < m.size()) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }
}

