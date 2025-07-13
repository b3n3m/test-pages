package NetworkService;

import java.io.*;
import java.util.*;

public class Config {
    public String handle;
    public int port;
    public int whoisPort;
    public String autoreply;
    public String imagePath;
    public int ipcDiscoveryPort;
    public int ipcNetworkPort;
    public int ipcImageSenderPort;

    public Config() throws IOException {
        this("config.toml");
    }

    public Config(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("[Config] Datei " + filename + " nicht gefunden – Standardwerte werden angelegt!");
            createDefaultConfig(file);
        }
        Map<String, String> values = readConfig(filename);
        handle = values.getOrDefault("handle", "Max");
        port = Integer.parseInt(values.getOrDefault("port", "5010"));
        whoisPort = Integer.parseInt(values.getOrDefault("whoisport", "4000"));
        autoreply = values.getOrDefault("autoreply", "Hey, ich bin gerade AFK!");
        imagePath = values.getOrDefault("imagepath", "downloads/");
        ipcDiscoveryPort = Integer.parseInt(values.getOrDefault("ipcdiscoveryport", "5050"));
        ipcNetworkPort = Integer.parseInt(values.getOrDefault("ipcnetworkport", "5051"));
        ipcImageSenderPort = Integer.parseInt(values.getOrDefault("ipcimagesenderport", "5052"));
    }

    private void createDefaultConfig(File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("handle = \"Max\"");
            writer.println("port = 5010");
            writer.println("whoisport = 4000");
            writer.println("autoreply = \"Hey, ich bin gerade AFK!\"");
            writer.println("imagepath = \"downloads/\"");
            writer.println("ipcdiscoveryport = 5050");
            writer.println("ipcnetworkport = 5051");
            writer.println("ipcimagesenderport = 5052");
        }
        System.out.println("[Config] Default-config.toml erstellt!");
    }

    private Map<String, String> readConfig(String filePath) throws IOException {
        Map<String, String> config = new HashMap<>();
        File file = new File(filePath);
        Scanner scanner = new Scanner(new FileReader(file));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim().replaceAll("^\"|\"$", "");
                config.put(key, value);
            }
        }
        scanner.close();
        return config;
    }
}
