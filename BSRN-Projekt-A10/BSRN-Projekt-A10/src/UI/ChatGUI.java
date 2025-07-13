package UI;

import NetworkService.Config;    

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatGUI extends JFrame {
	private JTextArea chatArea = new JTextArea();
	private JTextField tfHandle = new JTextField(12);
	private JTextField tfPort = new JTextField(6);
	private JButton btnJoin = new JButton("JOIN");
	private JButton btnWho = new JButton("WHO");
	private JButton btnLeave = new JButton("LEAVE");
	private JTextField tfEmpfaenger = new JTextField(10);
	private JTextField tfMsg = new JTextField(20);
	private JButton btnSend = new JButton("MSG senden");
	private JTextField tfImagePath = new JTextField(18);
	private JButton btnBrowse = new JButton("Durchsuchen...");
	private JButton btnSendImg = new JButton("IMG senden");
	private JLabel statusLabel = new JLabel("Nicht verbunden");

	private String currentHandle;
	private int currentPort;
	private Thread receiveThread;

	public ChatGUI(Config config) {
		setTitle("BSRN Chat-GUI - " + config.handle);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 450);
		setLocationRelativeTo(null);

		chatArea.setEditable(false);

		// Panel oben für JOIN
		JPanel joinPanel = new JPanel();
		joinPanel.add(new JLabel("Name:"));
		tfHandle.setText(config.handle);
		joinPanel.add(tfHandle);
		joinPanel.add(new JLabel("Port:"));
		tfPort.setText(String.valueOf(config.port));
		joinPanel.add(tfPort);
		joinPanel.add(btnJoin);

		// Panel Mitte für Chat
		JPanel chatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		chatPanel.add(new JLabel("Empfänger:"));
		chatPanel.add(tfEmpfaenger);
		chatPanel.add(new JLabel("Nachricht:"));
		chatPanel.add(tfMsg);
		chatPanel.add(btnSend);

		// Panel unten für Bild
		JPanel imgPanel = new JPanel();
		imgPanel.add(new JLabel("Bild:"));
		tfImagePath.setEditable(false);
		imgPanel.add(tfImagePath);
		imgPanel.add(btnBrowse);
		imgPanel.add(btnSendImg);

		// Panel rechts für Steuerung
		JPanel controlPanel = new JPanel(new GridLayout(3, 1, 4, 4));
		controlPanel.add(btnWho);
		controlPanel.add(btnLeave);

		// Haupt-Layout
		setLayout(new BorderLayout());
		add(joinPanel, BorderLayout.NORTH);
		add(new JScrollPane(chatArea), BorderLayout.CENTER);

		JPanel southPanel = new JPanel(new GridLayout(3, 1));
		southPanel.add(chatPanel);
		southPanel.add(imgPanel);
		southPanel.add(statusLabel);
		add(southPanel, BorderLayout.SOUTH);

		add(controlPanel, BorderLayout.EAST);

		setChatEnabled(false);

		// --- Action Listener ---
		btnJoin.addActionListener(this::doJoin);
		btnWho.addActionListener(this::doWho);
		btnLeave.addActionListener(this::doLeave);
		btnSend.addActionListener(this::doSendMsg);
		btnBrowse.addActionListener(this::doBrowse);
		btnSendImg.addActionListener(this::doSendImg);
	}

	private void setChatEnabled(boolean enabled) {
		tfEmpfaenger.setEnabled(enabled);
		tfMsg.setEnabled(enabled);
		btnSend.setEnabled(enabled);
		tfImagePath.setEnabled(enabled);
		btnBrowse.setEnabled(enabled);
		btnSendImg.setEnabled(enabled);
		btnWho.setEnabled(enabled);
		btnLeave.setEnabled(enabled);
	}

	private void doJoin(ActionEvent e) {
		String handle = tfHandle.getText().trim();
		String portStr = tfPort.getText().trim();
		if (handle.isEmpty() || portStr.isEmpty()) {
			appendMsg("[Fehler] Name und Port müssen angegeben werden!");
			return;
		}
		int port = Integer.parseInt(portStr);
		try {
			String reply = sendIPC("localhost", 5050, "JOIN " + handle + " " + port);
			appendMsg("[System] JOIN gesendet: " + reply);
			setChatEnabled(true);
			tfHandle.setEnabled(false);
			tfPort.setEnabled(false);
			btnJoin.setEnabled(false);
			statusLabel.setText("Verbunden als: " + handle);
			currentHandle = handle;
			currentPort = port;

			// --- Empfangs-Thread starten ---
			if (receiveThread == null) {
				receiveThread = new Thread(() -> {
					while (currentHandle != null) {
						try {
							String[] msgs = fetchMessages(currentHandle);
							for (String m : msgs) {
								if (!m.equals("NO_MESSAGES") && !m.trim().isEmpty()) {
									SwingUtilities.invokeLater(() -> appendMsg("[Empfangen] " + m));
								}
							}
							Thread.sleep(1000);
						} catch (Exception ignored) {
						}
					}
				});
				receiveThread.setDaemon(true);
				receiveThread.start();
			}
		} catch (Exception ex) {
			appendMsg("[Fehler] JOIN nicht möglich: " + ex.getMessage());
		}
	}

	private void doWho(ActionEvent e) {
		try {
			String reply = sendIPC("localhost", 5050, "WHO");
			appendMsg("[System] WHO-Antwort: " + reply);
		} catch (Exception ex) {
			appendMsg("[Fehler] WHO nicht möglich: " + ex.getMessage());
		}
	}

	private void doLeave(ActionEvent e) {
		if (currentHandle == null)
			return;
		try {
			String reply = sendIPC("localhost", 5050, "LEAVE " + currentHandle);
			appendMsg("[System] LEAVE gesendet: " + reply);
			setChatEnabled(false);
			tfHandle.setEnabled(true);
			tfPort.setEnabled(true);
			btnJoin.setEnabled(true);
			statusLabel.setText("Nicht verbunden");
			currentHandle = null;
			currentPort = 0;
			if (receiveThread != null) {
				receiveThread.interrupt();
				receiveThread = null;
			}
		} catch (Exception ex) {
			appendMsg("[Fehler] LEAVE nicht möglich: " + ex.getMessage());
		}
	}

	private void doSendMsg(ActionEvent e) {
		String empfaenger = tfEmpfaenger.getText().trim();
		String nachricht = tfMsg.getText().trim();
		if (empfaenger.isEmpty() || nachricht.isEmpty()) {
			appendMsg("[Fehler] Empfänger und Nachricht angeben!");
			return;
		}
		try {
			String reply = sendIPC("localhost", 5051, "MSG " + empfaenger + " " + nachricht);
			appendMsg("[Ich → " + empfaenger + "]: " + nachricht + " (" + reply + ")");
		} catch (Exception ex) {
			appendMsg("[Fehler beim Senden: " + ex.getMessage() + "]");
		}
		tfMsg.setText("");
	}

	private void doBrowse(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			tfImagePath.setText(file.getAbsolutePath());
		}
	}

	private void doSendImg(ActionEvent e) {
		String empfaenger = tfEmpfaenger.getText().trim();
		String pfad = tfImagePath.getText().trim();
		if (empfaenger.isEmpty() || pfad.isEmpty()) {
			appendMsg("[Fehler] Empfänger und Bild auswählen!");
			return;
		}
		// Ziel-IP und -Port fest verdrahtet auf 127.0.0.1 und 5002
		try {
			String reply = sendIPC("localhost", 5052, "IMG " + empfaenger + " 127.0.0.1 5002 " + pfad);
			appendMsg("[Ich → " + empfaenger + "]: Bildauftrag gesendet (" + pfad + "), Antwort: " + reply);
		} catch (Exception ex) {
			appendMsg("[Fehler beim Bildversand: " + ex.getMessage() + "]");
		}
		tfImagePath.setText("");
	}

	// --- IPC-Methode ---
	private String sendIPC(String host, int port, String msg) throws IOException {
		try (Socket socket = new Socket(host, port);
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
			out.write(msg + "\n");
			out.flush();
			return in.readLine();
		}
	}

	// --- Nachrichten-Fetcher ---
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

	public void appendMsg(String msg) {
		chatArea.append(msg + "\n");
	}

	public static void main(String[] args) throws Exception {
		Config config = new Config(); // Passe ggf. Pfad/Parameter an!
		SwingUtilities.invokeLater(() -> new ChatGUI(config).setVisible(true));
	}
}
