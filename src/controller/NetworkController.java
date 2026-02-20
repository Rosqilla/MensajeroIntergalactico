package controller;

import model.network.NetworkMessage;
import model.network.PeerInfo;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controlador de red P2P usando UDP multicast.
 * Gestiona el descubrimiento de peers y la sincronización de estado.
 */
public class NetworkController {
    
    // Configuración de red
    private static final String MULTICAST_ADDRESS = "230.0.0.1";
    private static final int MULTICAST_PORT = 4446;
    private static final int PEER_TIMEOUT_MS = 30000; // 30 segundos
    private static final int HEARTBEAT_INTERVAL_MS = 5000; // 5 segundos
    
    // Identificación local
    private final String localPeerId;
    private final int localPort;
    
    // Red
    private MulticastSocket socket;
    private InetAddress group;
    private Thread receiverThread;
    private Thread heartbeatThread;
    private volatile boolean running;
    
    // Gestión de peers
    private final Map<String, PeerInfo> peers;
    private final List<NetworkListener> listeners;
    
    // Buffer para mensajes
    private static final int BUFFER_SIZE = 8192;
    
    public NetworkController(int localPort) {
        this.localPort = localPort;
        this.localPeerId = generatePeerId();
        this.peers = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.running = false;
    }
    
    /**
     * Genera un ID único para este peer.
     */
    private String generatePeerId() {
        return "Peer_" + localPort + "_" + System.currentTimeMillis();
    }
    
    /**
     * Inicia el sistema de red P2P.
     */
    public void start() throws IOException {
        if (running) {
            return;
        }
        
        running = true;
        
        // Configurar socket multicast
        socket = new MulticastSocket(MULTICAST_PORT);
        group = InetAddress.getByName(MULTICAST_ADDRESS);
        
        // Unirse al grupo multicast
        SocketAddress groupAddress = new InetSocketAddress(group, MULTICAST_PORT);
        NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        socket.joinGroup(groupAddress, netIf);
        
        // Iniciar threads
        startReceiverThread();
        startHeartbeatThread();
        
        // Anunciar presencia
        announcePresence();
        
        System.out.println("NetworkController iniciado: " + localPeerId + " en puerto " + localPort);
    }
    
    /**
     * Detiene el sistema de red.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        
        // Enviar mensaje de salida
        try {
            sendLeaveMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Cerrar threads
        if (receiverThread != null) {
            receiverThread.interrupt();
        }
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }
        
        // Cerrar socket
        if (socket != null && !socket.isClosed()) {
            try {
                SocketAddress groupAddress = new InetSocketAddress(group, MULTICAST_PORT);
                NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                socket.leaveGroup(groupAddress, netIf);
            } catch (Exception e) {
                e.printStackTrace();
            }
            socket.close();
        }
        
        peers.clear();
        System.out.println("NetworkController detenido");
    }
    
    /**
     * Inicia el thread que recibe mensajes.
     */
    private void startReceiverThread() {
        receiverThread = new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String received = new String(packet.getData(), 0, packet.getLength());
                    handleReceivedMessage(received, packet.getAddress());
                    
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error recibiendo mensaje: " + e.getMessage());
                    }
                }
            }
        });
        receiverThread.setName("NetworkReceiver");
        receiverThread.setDaemon(true);
        receiverThread.start();
    }
    
    /**
     * Inicia el thread de heartbeat que anuncia presencia periódicamente.
     */
    private void startHeartbeatThread() {
        heartbeatThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL_MS);
                    
                    // Anunciar presencia
                    announcePresence();
                    
                    // Limpiar peers inactivos
                    cleanupInactivePeers();
                    
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("Error en heartbeat: " + e.getMessage());
                }
            }
        });
        heartbeatThread.setName("NetworkHeartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }
    
    /**
     * Maneja un mensaje recibido.
     */
    private void handleReceivedMessage(String data, InetAddress senderAddress) {
        NetworkMessage msg = NetworkMessage.deserialize(data);
        if (msg == null) {
            return;
        }
        
        // Ignorar mensajes propios
        if (msg.getSenderId().equals(localPeerId)) {
            return;
        }
        
        switch (msg.getType()) {
            case PEER_ANNOUNCE:
                handlePeerAnnounce(msg, senderAddress);
                break;
                
            case SHIP_UPDATE:
                handleShipUpdate(msg);
                break;
                
            case PEER_LEAVE:
                handlePeerLeave(msg);
                break;
        }
    }
    
    /**
     * Maneja un anuncio de peer.
     */
    private void handlePeerAnnounce(NetworkMessage msg, InetAddress address) {
        String peerId = msg.getSenderId();
        int port = msg.getInt("port", localPort);
        
        PeerInfo peer = peers.get(peerId);
        if (peer == null) {
            // Nuevo peer detectado
            peer = new PeerInfo(peerId, address, port);
            peers.put(peerId, peer);
            
            System.out.println("Nuevo peer conectado: " + peer);
            notifyPeerConnected(peer);
        } else {
            // Actualizar timestamp
            peer.updateLastSeen();
        }
    }
    
    /**
     * Maneja una actualización de nave.
     */
    private void handleShipUpdate(NetworkMessage msg) {
        // Actualizar timestamp del peer
        PeerInfo peer = peers.get(msg.getSenderId());
        if (peer != null) {
            peer.updateLastSeen();
        }
        
        // Notificar a listeners
        NetworkMessage.ShipData shipData = NetworkMessage.ShipData.fromMessage(msg);
        notifyShipUpdate(shipData);
    }
    
    /**
     * Maneja la salida de un peer.
     */
    private void handlePeerLeave(NetworkMessage msg) {
        String peerId = msg.getSenderId();
        PeerInfo peer = peers.remove(peerId);
        
        if (peer != null) {
            System.out.println("Peer desconectado: " + peer);
            notifyPeerDisconnected(peer);
        }
    }
    
    /**
     * Limpia peers inactivos (timeout).
     */
    private void cleanupInactivePeers() {
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, PeerInfo> entry : peers.entrySet()) {
            if (entry.getValue().isTimeout(PEER_TIMEOUT_MS)) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (String peerId : toRemove) {
            PeerInfo peer = peers.remove(peerId);
            if (peer != null) {
                System.out.println("Peer timeout: " + peer);
                notifyPeerDisconnected(peer);
            }
        }
    }
    
    /**
     * Anuncia presencia en la red.
     */
    private void announcePresence() {
        NetworkMessage msg = new NetworkMessage(NetworkMessage.MessageType.PEER_ANNOUNCE, localPeerId);
        msg.put("port", localPort);
        sendMessage(msg);
    }
    
    /**
     * Envía mensaje de salida.
     */
    private void sendLeaveMessage() {
        NetworkMessage msg = new NetworkMessage(NetworkMessage.MessageType.PEER_LEAVE, localPeerId);
        sendMessage(msg);
    }
    
    /**
     * Envía una actualización de nave.
     */
    public void sendShipUpdate(NetworkMessage.ShipData shipData) {
        NetworkMessage msg = shipData.toMessage();
        sendMessage(msg);
    }
    
    /**
     * Envía un mensaje a la red multicast.
     */
    private void sendMessage(NetworkMessage msg) {
        if (!running || socket == null) {
            return;
        }
        
        try {
            String data = msg.serialize();
            byte[] buffer = data.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Error enviando mensaje: " + e.getMessage());
        }
    }
    
    // ========== Gestión de Listeners ==========
    
    public void addListener(NetworkListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(NetworkListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyPeerConnected(PeerInfo peer) {
        for (NetworkListener listener : listeners) {
            listener.onPeerConnected(peer);
        }
    }
    
    private void notifyPeerDisconnected(PeerInfo peer) {
        for (NetworkListener listener : listeners) {
            listener.onPeerDisconnected(peer);
        }
    }
    
    private void notifyShipUpdate(NetworkMessage.ShipData shipData) {
        for (NetworkListener listener : listeners) {
            listener.onShipUpdate(shipData);
        }
    }
    
    // ========== Getters ==========
    
    public String getLocalPeerId() {
        return localPeerId;
    }
    
    public int getLocalPort() {
        return localPort;
    }
    
    public Collection<PeerInfo> getPeers() {
        return new ArrayList<>(peers.values());
    }
    
    public int getPeerCount() {
        return peers.size();
    }
    
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Interfaz para escuchar eventos de red.
     */
    public interface NetworkListener {
        void onPeerConnected(PeerInfo peer);
        void onPeerDisconnected(PeerInfo peer);
        void onShipUpdate(NetworkMessage.ShipData shipData);
    }
}
