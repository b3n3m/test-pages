package DiscoveryService;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

import NetworkService.Config; // WICHTIG: Import für deine Config!

public class DiscoveryServerIPC {

	private final int DISCOVERY_PORT; // UDP für LAN (aus Config)
	private final int IPC_PORT; // Lokaler IPC-Port (aus Config)
	private Map<String, Peer> peers = new ConcurrentHashMap<>();
	private final long peerTimeoutMs = 60_000; // 1 min Inaktivitäts-Timeout

	// Neuer Konstruktor: Ports aus Config laden
	public DiscoveryServerIPC() {
		Config config = null;
		try {
			config = new Config();
		} catch (IOException e) {
			System.err.println("[DiscoveryServer] Konnte Config nicht laden, nutze Defaults!");
		}
		if (config != null) {
			DISCOVERY_PORT = config.whoisPort;
			IPC_PORT = config.ipcDiscoveryPort;
		} else {
			DISCOVERY_PORT = 4000;
			IPC_PORT = 5050;
		}
	}

	public static void main(String[] args) {
		new DiscoveryServerIPC().start();
	}

	public void start() {
		// Starte UDP-Broadcast-Listener (wie bisher)
		new Thread(this::startUDPListener).start();
		// Starte lokalen IPC-Server für GUI/CLI-Prozesse
		new Thread(this::startIPCListener).start();
	}

	// Empfängt JOIN/LEAVE/WHO per UDP-Broadcast (wie gehabt)
	private void startUDPListener() {
		try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
			byte[] buffer = new byte[1024];
			System.out.println("[DiscoveryServer] Lauscht auf UDP-Port " + DISCOVERY_PORT);

			while (true) {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8").trim();

				if (message.startsWith("JOIN")) {
					handleJoin(message, packet.getAddress().getHostAddress(), packet.getPort());
				} else if (message.startsWith("LEAVE")) {
					handleLeave(message);
				} else if (message.startsWith("WHO")) {
					handleWhoUDP(packet, socket);
				}
				cleanupOldPeers();
			}
		} catch (BindException be) {
			System.err.println("[DiscoveryServer] Port " + DISCOVERY_PORT + " ist belegt! (UDP)");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Lokaler TCP-Server für IPC (z.B. für GUI/CLI)
	private void startIPCListener() {
		try (ServerSocket serverSocket = new ServerSocket(IPC_PORT)) {
			System.out.println("[DiscoveryServer] IPC-Server läuft auf Port " + IPC_PORT);
			while (true) {
				Socket client = serverSocket.accept();
				new Thread(() -> handleIPCClient(client)).start();
			}
		} catch (BindException be) {
			System.err.println("[DiscoveryServer] Port " + IPC_PORT + " ist belegt! (TCP)");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Behandle lokale IPC-Anfragen (JOIN, WHO, LEAVE)
	private void handleIPCClient(Socket client) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))) {
			String request = in.readLine();
			if (request == null)
				return;
			if (request.startsWith("JOIN")) {
				// JOIN <Handle> <Port>
				String[] parts = request.split(" ");
				if (parts.length == 3) {
					handleJoin(request, "127.0.0.1", Integer.parseInt(parts[2]));
					out.write("JOIN_OK\n");
				} else {
					out.write("JOIN_FAIL\n");
				}
			} else if (request.startsWith("LEAVE")) {
				handleLeave(request);
				out.write("LEAVE_OK\n");
			} else if (request.trim().equals("WHO")) {
				StringBuilder response = new StringBuilder("KNOWNUSERS");
				boolean first = true;
				for (Peer peer : peers.values()) {
					if (!first)
						response.append(", ");
					response.append(peer.name).append(" ").append(peer.ip).append(" ").append(peer.port);
					first = false;
				}
				out.write(response + "\n");
			} else {
				out.write("UNKNOWN_COMMAND\n");
			}
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
			}
		}
	}

	// JOIN-Nachricht (UDP oder IPC)
	private void handleJoin(String message, String ip, int port) {
		try {
			String[] parts = message.split(" ");
			if (parts.length != 3)
				return;
			String handle = parts[1];
			Peer peer = new Peer(handle, ip, port);
			peers.put(handle, peer);
			System.out.println("[Discovery] Peer hinzugefügt/aktualisiert: " + handle + "@" + ip + ":" + port);
		} catch (Exception e) {
			System.err.println("[Discovery] Fehler beim Parsen von JOIN " + message);
		}
	}

	// LEAVE-Nachricht
	private void handleLeave(String message) {
		try {
			String[] parts = message.split(" ");
			if (parts.length != 2)
				return;
			String handle = parts[1];
			peers.remove(handle);
			System.out.println("[Discovery] Peer entfernt: " + handle);
		} catch (Exception e) {
			System.out.println("[Discovery] Fehler beim Parsen von LEAVE: " + message);
		}
	}

	// WHO per UDP (wie gehabt)
	private void handleWhoUDP(DatagramPacket requestPacket, DatagramSocket socket) {
		if (peers.isEmpty())
			return;
		StringBuilder response = new StringBuilder("KNOWNUSERS");
		boolean first = true;
		for (Peer peer : peers.values()) {
			if (!first)
				response.append(", ");
			response.append(peer.name).append(" ").append(peer.ip).append(" ").append(peer.port);
			first = false;
		}
		byte[] respData = response.toString().getBytes();
		try {
			DatagramPacket responsePacket = new DatagramPacket(respData, respData.length, requestPacket.getAddress(),
					requestPacket.getPort());
			socket.send(responsePacket);
			System.out.println("[Discovery] KNOWNUSERS gesendet an " + requestPacket.getAddress().getHostAddress());
		} catch (Exception e) {
			System.out.println("[Discovery] Fehler beim Senden von KNOWNUSERS");
		}
	}

	// Entfernt inaktive Peers
	private void cleanupOldPeers() {
		long now = System.currentTimeMillis();
		peers.values().removeIf(peer -> (now - peer.lastSeen) > peerTimeoutMs);
	}

}
