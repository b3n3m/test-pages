package NetworkService;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class NetworkServiceServer {
    private static final int IPC_PORT = 5051;
    private static final int LAN_PORT = 5000; // UDP für MSG

    // Nachrichtenwarteschlangen pro Benutzername
    private static final ConcurrentMap<String, Queue<String>> inboxes = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(IPC_PORT)) {
            System.out.println("[NetworkServiceServer] Lauscht auf lokalem Port " + IPC_PORT);

            // Netzwerk-Kommunikation für das LAN
            NetworkService netService = new NetworkService(LAN_PORT);

            // Thread: Empfange UDP-Messages und verteile an Inboxes
            new Thread(() -> {
                while (true) {
                    try {
                        NetworkService.ReceivedMessage recv = netService.receiveMessage(1024);
                        String msg = recv.message.trim();
                        // Parsing: MSG <Empfänger> <Text>
                        NetworkService.ParsedMessage parsed = NetworkService.parseMessage(msg);
                        if (parsed != null && "MSG".equals(parsed.command) && parsed.parameters.size() >= 2) {
                            String empfaenger = parsed.parameters.get(0);
                            String text = parsed.parameters.get(1);
                            inboxes.computeIfAbsent(empfaenger, k -> new ConcurrentLinkedQueue<>())
                                .add("[Von " + recv.senderAddress.getHostAddress() + "] " + text);
                        }
                    } catch (IOException e) {
                        System.err.println("[NetworkServiceServer] UDP-Empfangsfehler: " + e.getMessage());
                    }
                }
            }).start();

            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client, netService)).start();
            }
        } catch (IOException e) {
            System.err.println("[NetworkServiceServer] Fehler beim Start: " + e.getMessage());
        }
    }

    private static void handleClient(Socket client, NetworkService netService) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))
        ) {
            String request = in.readLine();
            if (request == null) return;

            // Unterstütze: MSG <Empfänger> <Text> (Senden)
            if (request.startsWith("MSG")) {
                NetworkService.ParsedMessage msg = NetworkService.parseMessage(request);
                if (msg != null && msg.parameters.size() >= 2) {
                    String empfaenger = msg.parameters.get(0);
                    String text = msg.parameters.get(1);
                    // IP/Port für Demo lokal lassen (sonst Discovery!)
                    InetAddress ip = InetAddress.getByName("127.0.0.1");
                    int port = LAN_PORT;
                    String slcp = "MSG " + empfaenger + " " + text;
                    netService.sendMessage(slcp, ip, port);
                    out.write("MESSAGE_SENT\n");
                } else {
                    out.write("INVALID_MSG_COMMAND\n");
                }
                out.flush();

            // Unterstütze: FETCH <Handle> (Empfang)
            } else if (request.startsWith("FETCH")) {
                String[] parts = request.trim().split("\\s+", 2);
                if (parts.length == 2) {
                    String empfaenger = parts[1];
                    Queue<String> inbox = inboxes.getOrDefault(empfaenger, new LinkedList<>());
                    if (inbox.isEmpty()) {
                        out.write("NO_MESSAGES\n");
                    } else {
                        while (!inbox.isEmpty()) {
                            out.write(inbox.poll() + "\n");
                        }
                    }
                } else {
                    out.write("INVALID_FETCH_COMMAND\n");
                }
                out.flush();

            } else {
                out.write("UNKNOWN_COMMAND\n");
                out.flush();
            }

        } catch (Exception e) {
            System.err.println("[NetworkServiceServer] Fehler: " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException e) {}
        }
    }
}
