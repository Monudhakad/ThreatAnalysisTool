// InputManager.java - minimal stub
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import com.google.gson.Gson;
import java.util.*;

public class InputManager {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java InputManager <path-to-zip-or-folder>");
            System.exit(1);
        }
        Path input = Paths.get(args[0]);
        Map<String,Object> meta = new HashMap<>();
        if (Files.isDirectory(input)) {
            meta.put("mode", "folder");
            List<String> files = new ArrayList<>();
            Files.walk(input).filter(Files::isRegularFile).forEach(p -> files.add(input.relativize(p).toString()));
            meta.put("files", files);
        } else if (args[0].endsWith(".zip")) {
            meta.put("mode", "zip");
            List<String> files = new ArrayList<>();
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(input.toFile()))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    files.add(entry.getName());
                }
            }
            meta.put("files", files);
        } else {
            meta.put("mode", "file");
            meta.put("files", Arrays.asList(input.getFileName().toString()));
        }
        // write JSON
        Gson g = new Gson();
        Files.createDirectories(Paths.get("workspace/output"));
        try (Writer w = Files.newBufferedWriter(Paths.get("workspace/output/input_metadata.json"))) {
            g.toJson(meta, w);
        }
        System.out.println("Wrote workspace/output/input_metadata.json");
    }
}

