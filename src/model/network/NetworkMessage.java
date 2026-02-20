package model.network;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Mensaje de red para comunicación P2P.
 * Formato: TYPE|senderId|key1:value1|key2:value2|...
 */
public class NetworkMessage {
    
    public enum MessageType {
        PEER_ANNOUNCE,   // Anuncio de presencia en la red
        SHIP_UPDATE,     // Actualización de posición/estado de nave
        PEER_LEAVE       // Peer abandonando la red
    }
    
    private final MessageType type;
    private final String senderId;
    private final Map<String, String> data;
    
    public NetworkMessage(MessageType type, String senderId) {
        this.type = type;
        this.senderId = senderId;
        this.data = new HashMap<>();
    }
    
    public void put(String key, String value) {
        data.put(key, value);
    }
    
    public void put(String key, double value) {
        data.put(key, String.valueOf(value));
    }
    
    public void put(String key, int value) {
        data.put(key, String.valueOf(value));
    }
    
    public void put(String key, boolean value) {
        data.put(key, String.valueOf(value));
    }
    
    public String get(String key) {
        return data.get(key);
    }
    
    public double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(data.get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(data.get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(data.get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    public MessageType getType() {
        return type;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    /**
     * Serializa el mensaje a String.
     * Formato: TYPE|senderId|key1:value1|key2:value2|...
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name()).append("|");
        sb.append(senderId);
        
        for (Map.Entry<String, String> entry : data.entrySet()) {
            sb.append("|").append(entry.getKey()).append(":").append(entry.getValue());
        }
        
        return sb.toString();
    }
    
    /**
     * Deserializa un mensaje desde String.
     */
    public static NetworkMessage deserialize(String serialized) {
        try {
            String[] parts = serialized.split("\\|");
            if (parts.length < 2) {
                return null;
            }
            
            MessageType type = MessageType.valueOf(parts[0]);
            String senderId = parts[1];
            
            NetworkMessage msg = new NetworkMessage(type, senderId);
            
            // Parsear datos adicionales
            for (int i = 2; i < parts.length; i++) {
                String[] keyValue = parts[i].split(":", 2);
                if (keyValue.length == 2) {
                    msg.put(keyValue[0], keyValue[1]);
                }
            }
            
            return msg;
        } catch (Exception e) {
            System.err.println("Error deserializando mensaje: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Clase interna para datos de nave serializables.
     */
    public static class ShipData {
        public String shipId;
        public double posX, posY;
        public double velX, velY;
        public double angle;
        public String ownerPeerId;
        public int lives;
        public boolean hasPackage;
        public int colorRGB;
        
        public ShipData() {}
        
        public ShipData(String shipId, double x, double y, double vx, double vy, 
                       double angle, String ownerId, int lives, boolean hasPackage) {
            this.shipId = shipId;
            this.posX = x;
            this.posY = y;
            this.velX = vx;
            this.velY = vy;
            this.angle = angle;
            this.ownerPeerId = ownerId;
            this.lives = lives;
            this.hasPackage = hasPackage;
            this.colorRGB = Color.CYAN.getRGB(); // Color por defecto para remotos
        }
        
        public NetworkMessage toMessage() {
            NetworkMessage msg = new NetworkMessage(MessageType.SHIP_UPDATE, ownerPeerId);
            msg.put("shipId", shipId);
            msg.put("posX", posX);
            msg.put("posY", posY);
            msg.put("velX", velX);
            msg.put("velY", velY);
            msg.put("angle", angle);
            msg.put("lives", lives);
            msg.put("hasPackage", hasPackage);
            msg.put("colorRGB", colorRGB);
            return msg;
        }
        
        public static ShipData fromMessage(NetworkMessage msg) {
            ShipData data = new ShipData();
            data.shipId = msg.get("shipId");
            data.posX = msg.getDouble("posX", 0);
            data.posY = msg.getDouble("posY", 0);
            data.velX = msg.getDouble("velX", 0);
            data.velY = msg.getDouble("velY", 0);
            data.angle = msg.getDouble("angle", 0);
            data.ownerPeerId = msg.getSenderId();
            data.lives = msg.getInt("lives", 3);
            data.hasPackage = msg.getBoolean("hasPackage", false);
            data.colorRGB = msg.getInt("colorRGB", Color.CYAN.getRGB());
            return data;
        }
    }
}
