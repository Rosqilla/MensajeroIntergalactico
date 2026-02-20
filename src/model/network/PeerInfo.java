package model.network;

import java.net.InetAddress;

/**
 * Información de un peer en la red P2P.
 * Contiene identificador único, dirección IP, puerto y timestamp de última actividad.
 */
public class PeerInfo {
    private final String peerId;
    private final InetAddress address;
    private final int port;
    private long lastSeen;
    private String shipId;
    
    public PeerInfo(String peerId, InetAddress address, int port) {
        this.peerId = peerId;
        this.address = address;
        this.port = port;
        this.lastSeen = System.currentTimeMillis();
        this.shipId = null;
    }
    
    public String getPeerId() {
        return peerId;
    }
    
    public InetAddress getAddress() {
        return address;
    }
    
    public int getPort() {
        return port;
    }
    
    public long getLastSeen() {
        return lastSeen;
    }
    
    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }
    
    public String getShipId() {
        return shipId;
    }
    
    public void setShipId(String shipId) {
        this.shipId = shipId;
    }
    
    public boolean isTimeout(long timeoutMs) {
        return System.currentTimeMillis() - lastSeen > timeoutMs;
    }
    
    @Override
    public String toString() {
        return String.format("Peer[%s @ %s:%d]", peerId, address.getHostAddress(), port);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerInfo peerInfo = (PeerInfo) o;
        return peerId.equals(peerInfo.peerId);
    }
    
    @Override
    public int hashCode() {
        return peerId.hashCode();
    }
}
