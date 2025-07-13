package NetworkService;

public class ImageReceiverServer {
    public static void main(String[] args) {
        int port = 5002; // Passe ggf. auf euren Bild-Empfangs-Port an (aus Config!)
        imageReceiver receiver = new imageReceiver(port);
        receiver.run();
    }
}
