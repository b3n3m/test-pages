package NetworkService;

import java.net.InetAddress;
 
public class TestNetworkService {
    public static void main(String[] args) throws Exception {
        NetworkService net = new NetworkService(5001);

        // JOIN senden (Broadcast)
        String joinMsg = "JOIN Alice 5001";
        net.sendMessage(joinMsg, InetAddress.getByName("255.255.255.255"), 4000);

        // WHO senden (Broadcast)
        net.sendMessage("WHO", InetAddress.getByName("255.255.255.255"), 4000);

        // Auf Antwort warten
        NetworkService.ReceivedMessage response = net.receiveMessage(1024);
        System.out.println("Antwort: " + response.message);

        // Nachricht parsen
        NetworkService.ParsedMessage msg = NetworkService.parseMessage(response.message);
        if (msg != null) {
            System.out.println("Befehl: " + msg.command);
            for (int i = 0; i < msg.parameters.size(); i++) {
                System.out.println("Parameter " + i + ": " + msg.parameters.get(i));
            }
        }

        net.close();
    }
}
