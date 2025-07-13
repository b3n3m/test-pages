package NetworkService;

import java.io.*;
import java.net.*;

/**
 * Versendet ein Bild per UDP an einen Empfänger.
 */
public class imageSender {

    /**
     * Sendet eine Bilddatei an einen Empfänger.
     * @param receiverIP   Ziel-IP-Adresse (z.B. "127.0.0.1")
     * @param receiverPort Zielport (z.B. 5000)
     * @param handle       Absender/Empfänger-Handle (nur für Protokoll, kann "Empfaenger" sein)
     * @param imagePath    Pfad zur Bilddatei
     * @throws IOException falls Netzwerk-/Dateifehler auftreten
     */
    public void sendImage(String receiverIP, int receiverPort, String handle, String imagePath) throws IOException {
        File imageFile = new File(imagePath);

        if (!imageFile.exists()) {
            System.out.println("Bilddatei nicht gefunden: " + imageFile.getAbsolutePath());
            return;
        }

        long size = imageFile.length();

        // IMG-Nachricht senden
        String imgMessage = "IMG " + handle + " " + size;
        DatagramSocket socket = new DatagramSocket();
        byte[] msgBytes = imgMessage.getBytes("UTF-8");
        DatagramPacket msgPacket = new DatagramPacket(
                msgBytes, msgBytes.length, InetAddress.getByName(receiverIP), receiverPort);
        socket.send(msgPacket);

        // Bilddaten senden
        byte[] buffer = new byte[(int) size];
        try (FileInputStream fis = new FileInputStream(imageFile)) {
            fis.read(buffer);
        }
        DatagramPacket imagePacket = new DatagramPacket(
                buffer, buffer.length, InetAddress.getByName(receiverIP), receiverPort);
        socket.send(imagePacket);

        System.out.println("Bild gesendet an " + receiverIP + ":" + receiverPort);
        socket.close();
    }
}