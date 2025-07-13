package NetworkService;

import java.io.*;
import java.net.*;

public class ImageSenderServer {

    private static final int IPC_PORT = 5052;

    public static void main(String[] args) {
        System.out.println("[ImageSenderServer] Lauscht auf Port " + IPC_PORT);
        try (ServerSocket serverSocket = new ServerSocket(IPC_PORT)) {
            imageSender sender = new imageSender();
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client, sender)).start();
            }
        } catch (Exception e) {
            System.err.println("[ImageSenderServer] Fehler: " + e.getMessage());
        }
    }

    private static void handleClient(Socket client, imageSender sender) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))
        ) {
            String request = in.readLine();
            // Erwartet: IMG <Empfaenger> <IP> <Port> <Bildpfad>
            String[] parts = request.split(" ", 5);
            if (parts.length == 5 && parts[0].equals("IMG")) {
                String empfaenger = parts[1];
                String ip = parts[2];
                int port = Integer.parseInt(parts[3]);
                String pfad = parts[4];
                sender.sendImage(ip, port, empfaenger, pfad);
                out.write("IMG_OK\n");
            } else {
                out.write("ERROR\n");
            }
            out.flush();
        } catch (Exception e) {
            System.err.println("[ImageSenderServer] Fehler: " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException e) {}
        }
    }
}

