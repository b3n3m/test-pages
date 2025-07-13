package NetworkService;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Kapselt die Netzwerk-Kommunikation für SLCP-Nachrichten (JOIN, LEAVE, WHO, MSG, IMG usw.)
 */
public class NetworkService {
    private final DatagramSocket socket;
    private final int localPort;

    public NetworkService(int localPort) throws SocketException {
        this.localPort = localPort;
        this.socket = new DatagramSocket(localPort);
        this.socket.setBroadcast(true);
    }

    /**
     * Sendet eine SLCP-Nachricht an die angegebene IP und Port.
     */
    public void sendMessage(String message, InetAddress address, int port) throws IOException {
        byte[] data = message.getBytes("UTF-8");
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }

    /**
     * Wartet auf eine eingehende SLCP-Nachricht (blockierend).
     * Gibt empfangenen Text, Absender-IP und Absender-Port zurück.
     */
    public ReceivedMessage receiveMessage(int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
        InetAddress senderAddress = packet.getAddress();
        int senderPort = packet.getPort();

        return new ReceivedMessage(message, senderAddress, senderPort);
    }

    /**
     * Schliesst den Socket.
     */
    public void close() {
        socket.close();
    }

    /**
     * Parsed eine SLCP-Nachricht in Befehl und Parameter.
     */
    public static ParsedMessage parseMessage(String rawMessage) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < rawMessage.length(); i++) {
            char c = rawMessage.charAt(i);

            if (c == '"' && (i == 0 || rawMessage.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
                continue;
            }

            if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        if (tokens.isEmpty()) {
            return null;
        }

        String command = tokens.get(0).toUpperCase();
        List<String> params = tokens.subList(1, tokens.size());

        return new ParsedMessage(command, params);
    }

    /**
     * Hilfsklasse für empfangene Nachrichten
     */
    public static class ReceivedMessage {
        public final String message;
        public final InetAddress senderAddress;
        public final int senderPort;

        public ReceivedMessage(String message, InetAddress senderAddress, int senderPort) {
            this.message = message;
            this.senderAddress = senderAddress;
            this.senderPort = senderPort;
        }
    }

    /**
     * Hilfsklasse für geparste SLCP-Nachrichten
     */
    public static class ParsedMessage {
        public final String command;
        public final List<String> parameters;

        public ParsedMessage(String command, List<String> parameters) {
            this.command = command;
            this.parameters = parameters;
        }
    }
}


