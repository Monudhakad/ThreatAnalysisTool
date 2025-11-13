// TechDetector.java
// Reads workspace/output/input_metadata.json and the extracted folder (if provided) to detect presence of common dependency files.
// Output: workspace/output/detected_tech.json
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TechDetector {
    public static void main(String[] args) throws Exception {
        // args[0] optional: path to the project folder (if omitted, it will only use input_metadata.json)
        Path outdir = Paths.get("workspace/output");
        Files.createDirectories(outdir);
        Path meta = outdir.resolve("input_metadata.json");
        if (!Files.exists(meta)) {
            System.out.println("Missing workspace/output/input_metadata.json. Run InputManager first.");
            System.exit(1);
        }

        String json = new String(Files.readAllBytes(meta));
        // Very naive parse just to detect filenames array
        List<String> files = extractFiles(json);
        Map<String,List<String>> detected = new LinkedHashMap<>();

        // heuristics
        if (containsFile(files, "package.json")) {
            detected.put("Node.js", Arrays.asList("package.json"));
        }
        if (containsFile(files, "pom.xml")) {
            detected.put("Maven (Java)", Arrays.asList("pom.xml"));
        }
        if (containsFile(files, "build.gradle")) {
            detected.put("Gradle (Java/Kotlin)", Arrays.asList("build.gradle"));
        }
        if (containsFile(files, "requirements.txt")) {
            detected.put("Python", Arrays.asList("requirements.txt"));
        }
        if (containsFile(files, "Pipfile")) {
            detected.put("Python (Pipenv)", Arrays.asList("Pipfile"));
        }
        if (detected.isEmpty()) {
            // fallback: list some file types present
            detected.put("Unknown", files.size() > 0 ? Arrays.asList(files.get(0)) : Arrays.asList("no-files-found"));
        }

        Path out = outdir.resolve("detected_tech.json");
        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            w.write(mapToJson(detected));
        }
        System.out.println("Wrote " + out.toString());
    }

    private static boolean containsFile(List<String> files, String name) {
        for (String f : files) {
            if (f.toLowerCase().endsWith(name.toLowerCase()) || f.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private static List<String> extractFiles(String json) {
        List<String> res = new ArrayList<>();
        int idx = json.indexOf("\"files\"");
        if (idx == -1) return res;
        int start = json.indexOf("[", idx);
        int end = json.indexOf("]", start);
        if (start == -1 || end == -1) return res;
        String array = json.substring(start+1, end);
        // split by commas, trim quotes
        String[] parts = array.split(",");
        for (String p : parts) {
            p = p.trim();
            if (p.startsWith("\"") && p.endsWith("\"")) p = p.substring(1, p.length()-1);
            res.add(p);
        }
        return res;
    }

    private static String mapToJson(Map<String,List<String>> m) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int i=0;
        for (Map.Entry<String,List<String>> e : m.entrySet()) {
            sb.append("  \"").append(escape(e.getKey())).append("\": ");
            sb.append(listToJson(e.getValue()));
            if (++i < m.size()) sb.append(",");
            sb.append("\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String listToJson(List<String> L) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i=0;i<L.size();i++) {
            sb.append("\"").append(escape(L.get(i))).append("\"");
            if (i+1 < L.size()) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }
}

