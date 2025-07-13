package NetworkService;

import java.io.*;
import java.net.*;

/**
 * Empfängt Bilddaten per UDP und speichert sie ab.
 */
public class imageReceiver implements Runnable {
    private final int port;

    /**
     * Konstruktor
     * @param port UDP-Port, auf dem auf Bilder gewartet wird
     */
    public imageReceiver(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("ImageReceiver läuft auf Port " + port + ", wartet auf IMG-Nachricht...");

            byte[] buffer = new byte[1024];

            while (true) {
                // IMG-Nachricht empfangen
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                System.out.println("Empfangen: " + msg);

                if (msg.startsWith("IMG")) {
                    String[] parts = msg.split(" ");
                    if (parts.length >= 3) {
                        String handle = parts[1];
                        int size = Integer.parseInt(parts[2].trim());

                        // Bilddaten empfangen
                        byte[] imageBuffer = new byte[size];
                        DatagramPacket imagePacket = new DatagramPacket(imageBuffer, imageBuffer.length);
                        socket.receive(imagePacket);

                        File outputDir = new File("empfangene_bilder");
                        if (!outputDir.exists()) outputDir.mkdirs();
                        File output = new File(outputDir, "empfangenes_bild_" + System.currentTimeMillis() + ".jpg");
                        try (FileOutputStream fos = new FileOutputStream(output)) {
                            fos.write(imagePacket.getData(), 0, imagePacket.getLength());
                        }
                        System.out.println("Bild von " + handle + " gespeichert unter: " + output.getAbsolutePath());
                    } else {
                        System.out.println("Fehler: Ungültige IMG-Nachricht");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("ImageReceiver Fehler: " + e.getMessage());
        }
    }
}