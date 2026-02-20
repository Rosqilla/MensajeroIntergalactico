package model;

import java.awt.geom.Point2D;

/**
 * Representa un paquete que debe ser recogido y entregado a un planeta.
 * Tipos: NORMAL, URGENTE, ESPECIAL, PESADO
 */
public class Package {
    public enum PackageType {
        NORMAL,    // +15s
        URGENT,    // +30s, desaparece en 30s
        SPECIAL,   // +1 vida
        HEAVY      // +45s, reduce velocidad
    }
    
    private final Point2D.Double position;
    private final Planet targetPlanet;
    private boolean collected;
    private boolean pickedUp;  // Recogido pero no entregado
    private final PackageType type;
    private long creationTime;
    private static final long URGENT_LIFETIME_MS = 30000; // 30 segundos
    
    public Package(double x, double y, Planet targetPlanet, PackageType type, long currentTime) {
        this.position = new Point2D.Double(x, y);
        this.targetPlanet = targetPlanet;
        this.collected = false;
        this.pickedUp = false;
        this.type = type;
        this.creationTime = currentTime;
    }
    
    public Point2D.Double getPosition() {
        return position;
    }
    
    public double getX() {
        return position.x;
    }
    
    public double getY() {
        return position.y;
    }
    
    public Planet getTargetPlanet() {
        return targetPlanet;
    }
    
    public boolean isCollected() {
        return collected;
    }
    
    public void setCollected(boolean collected) {
        this.collected = collected;
    }
    
    public boolean isPickedUp() {
        return pickedUp;
    }
    
    public void setPickedUp(boolean pickedUp) {
        this.pickedUp = pickedUp;
    }
    
    public PackageType getType() {
        return type;
    }
    
    /**
     * Marca el paquete como recogido.
     */
    public void collect() {
        this.collected = true;
    }
    
    /**
     * Verifica si un paquete urgente ha expirado.
     * [MVC] Tiempo inyectado como parámetro para evitar dependencia directa.
     */
    public boolean isExpired(long currentTime) {
        if (type == PackageType.URGENT) {
            return currentTime - creationTime > URGENT_LIFETIME_MS;
        }
        return false;
    }
    
    /**
     * Obtiene el tiempo restante para paquetes urgentes (en segundos).
     * [MVC] Tiempo inyectado como parámetro para evitar dependencia directa.
     */
    public int getRemainingTime(long currentTime) {
        if (type == PackageType.URGENT) {
            long elapsed = currentTime - creationTime;
            long remaining = URGENT_LIFETIME_MS - elapsed;
            return Math.max(0, (int)(remaining / 1000));
        }
        return -1;
    }
    
    /**
     * Obtiene el bonus de tiempo por entregar este paquete.
     */
    public int getTimeBonus() {
        switch (type) {
            case URGENT: return 30;
            case HEAVY: return 45;
            case NORMAL:
            default: return 15;
        }
    }
    
    /**
     * Verifica si este paquete reduce la velocidad al ser recogido.
     */
    public boolean reducesSpeed() {
        return type == PackageType.HEAVY;
    }
    
    /**
     * Obtiene el multiplicador de velocidad (0.7 para pesado, 1.0 para otros).
     */
    public double getSpeedMultiplier() {
        return type == PackageType.HEAVY ? 0.7 : 1.0;
    }
    
    /**
     * Verifica si este paquete otorga vida extra.
     */
    public boolean grantsExtraLife() {
        return type == PackageType.SPECIAL;
    }
    
    /**
     * Obtiene el color de destino (del planeta objetivo).
     */
    public java.awt.Color getDestinationColor() {
        return targetPlanet != null ? targetPlanet.getColor() : java.awt.Color.WHITE;
    }
    
    /**
     * Obtiene los puntos base por entregar este paquete.
     */
    public int getDeliveryPoints() {
        switch (type) {
            case SPECIAL: return 2000;
            case HEAVY: return 750;
            case URGENT: return 500;
            case NORMAL:
            default: return 300;
        }
    }
}
