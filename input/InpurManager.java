// InputManager.java
// Simple tool: takes a path to a folder or a .zip and writes workspace/output/input_metadata.json
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class InputManager {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java InputManager <path-to-folder-or-zip>");
            System.exit(1);
        }
        Path input = Paths.get(args[0]);
        Map<String,Object> meta = new LinkedHashMap<>();
        List<String> files = new ArrayList<>();

        if (Files.isDirectory(input)) {
            meta.put("mode", "folder");
            Files.walk(input).filter(Files::isRegularFile).forEach(p -> files.add(input.relativize(p).toString()));
        } else if (args[0].toLowerCase().endsWith(".zip")) {
            meta.put("mode", "zip");
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(input.toFile()))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) files.add(entry.getName());
                }
            }
        } else {
            meta.put("mode", "file");
            files.add(input.getFileName().toString());
        }
        meta.put("files", files);

        Path outdir = Paths.get("workspace/output");
        Files.createDirectories(outdir);
        Path out = outdir.resolve("input_metadata.json");
        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            w.write(mapToJson(meta));
        }
        System.out.println("Wrote " + out.toString());
    }

    // Very small JSON writer for our simple structures
    private static String mapToJson(Map<String,Object> m) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int i=0;
        for (Map.Entry<String,Object> e : m.entrySet()) {
            sb.append("  \"").append(e.getKey()).append("\": ");
            sb.append(valueToJson(e.getValue()));
            if (++i < m.size()) sb.append(",");
            sb.append("\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
    private static String valueToJson(Object v) {
        if (v instanceof String) return "\"" + escape((String)v) + "\"";
        if (v instanceof List) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            List<?> L = (List<?>)v;
            for (int i=0;i<L.size();i++) {
                sb.append("\"").append(escape(String.valueOf(L.get(i)))).append("\"");
                if (i+1 < L.size()) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escape(String.valueOf(v)) + "\"";
    }
    private static String escape(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }
}

        System.out.println("Wrote workspace/output/input_metadata.json");
    }
}

