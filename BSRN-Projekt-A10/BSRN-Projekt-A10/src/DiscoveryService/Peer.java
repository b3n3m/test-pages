package DiscoveryService;

import java.io.Serializable;

/**
 * Repräsentiert einen Chat-Teilnehmer (Peer) im lokalen Netzwerk
 */
public class Peer implements Serializable {
    /** Der Name (Handle) des Peers */
    public String name;
    /** Die aktuelle IP-Adresse des Peers */
    public String ip;
    /** Der aktuelle Port des Peers */
    public int port;
    /** Zeitpunkt des letzten Kontakts (Millisekunden seit Epoch) */
    public long lastSeen;

    /** Standard-Konstruktor für Serialisierung */
    public Peer() {}

    /** Konstruktor für Peer-Objekte. */
    public Peer(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.lastSeen = System.currentTimeMillis(); // Zeit der letzten Entdeckung
    }

    /** Aktualisiert Zeitstempel, wenn Peer wieder gesehen wird */
    public void refresh() {
        this.lastSeen = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return name + " (" + ip + ":" + port + ") [" + lastSeen + "]";
    }
}
