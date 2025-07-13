package UI;

import java.io.*;   
import java.net.*;
import java.util.*;

public class ChatCLI implements Runnable {
	private final String handle;
	private final int port;
	private boolean running = true;
	private Thread receiveThread;

	public ChatCLI(String handle, int port) {
		this.handle = handle;
		this.port = port;
	}

	@Override
	public void run() {
		try (Scanner scanner = new Scanner(System.in)) {
			printWelcome();
			while (running) {
				System.out.print("> ");
				String input = scanner.nextLine().trim();
				if (input.isEmpty())
					continue;
				handleCommand(input);
			}
		} catch (Exception e) {
			System.err.println("Fehler in CLI: " + e.getMessage());
		} finally {
			running = false;
			if (receiveThread != null)
				receiveThread.interrupt();
		}
	}

	private void printWelcome() {
		System.out.println("Willkommen im Chat-Programm (CLI)!");
		System.out.println("Befehle: ok (Hilfe), JOIN, MSG, IMG, WHO, LEAVE");
	}

	private void handleCommand(String input) {
		try {
			if (input.equalsIgnoreCase("ok")) {
				printHelp();
			} else if (input.startsWith("JOIN")) {
				sendIPC("localhost", 5050, "JOIN " + handle + " " + port);
				System.out.println("[CLI] JOIN gesendet.");
				// Starte Empfangs-Thread (nur 1x)
				if (receiveThread == null) {
					receiveThread = new Thread(() -> {
						while (running) {
							try {
								String[] messages = fetchMessages(handle);
								for (String m : messages) {
									if (!m.equals("NO_MESSAGES") && !m.trim().isEmpty())
										System.out.println("[Empfangen] " + m);
								}
								Thread.sleep(1000);
							} catch (Exception ignored) {
							}
						}
					});
					receiveThread.setDaemon(true);
					receiveThread.start();
				}
			} else if (input.startsWith("MSG")) {
				String[] parts = input.split(" ", 3);
				if (parts.length == 3) {
					String empfaenger = parts[1];
					String nachricht = parts[2];
					sendIPC("localhost", 5051, "MSG " + empfaenger + " " + nachricht);
					System.out.println("[CLI] Nachricht gesendet.");
				} else {
					System.out.println("Format: MSG <Empfänger> <Nachricht>");
				}
			} else if (input.startsWith("IMG")) {
				String[] parts = input.split(" ", 4);
				if (parts.length == 4) {
					String empfaenger = parts[1];
					String ip = parts[2];
					String pfad = parts[3];
					sendIPC("localhost", 5052, "IMG " + empfaenger + " " + ip + " 5002 " + pfad);
					System.out.println("[CLI] Bildauftrag gesendet an ImageSender.");
				} else {
					System.out.println("Format: IMG <Empfänger> <IP> <Bildpfad>");
				}
			} else if (input.startsWith("WHO")) {
				sendIPC("localhost", 5050, "WHO");
				System.out.println("[CLI] WHO gesendet.");
			} else if (input.startsWith("LEAVE")) {
				sendIPC("localhost", 5050, "LEAVE " + handle);
				running = false;
				System.out.println("[CLI] LEAVE gesendet. Chat wird beendet.");
			} else {
				System.out.println("Unbekannter Befehl. ok für Hilfe.");
			}
		} catch (Exception e) {
			System.err.println("Fehler beim Befehl: " + e.getMessage());
		}
	}

	private void sendIPC(String host, int port, String msg) throws IOException {
		try (Socket socket = new Socket(host, port);
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
			out.write(msg + "\n");
			out.flush();
			String reply = in.readLine();
			System.out.println("[IPC-Reply] " + reply);
		}
	}

	private String[] fetchMessages(String handle) {
		try (Socket socket = new Socket("localhost", 5051);
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
			out.write("FETCH " + handle + "\n");
			out.flush();
			String line;
			List<String> msgs = new ArrayList<>();
			while ((line = in.readLine()) != null) {
				msgs.add(line);
			}
			return msgs.toArray(new String[0]);
		} catch (IOException e) {
			return new String[0];
		}
	}

	private void printHelp() {
		System.out.println("Verfügbare Befehle:");
		System.out.println("JOIN <Handle> <Port>");
		System.out.println("MSG <Empfänger> <Nachricht>");
		System.out.println("IMG <Empfänger> <IP> <Bildpfad>");
		System.out.println("WHO");
		System.out.println("LEAVE");
	}

	public static void main(String[] args) {
		new ChatCLI("TestUser", 5010).run();
	}
}
//