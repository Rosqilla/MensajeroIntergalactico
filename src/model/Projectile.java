package model;

import java.awt.Color;

/**
 * Representa un proyectil disparado por la nave del jugador.
 */
public class Projectile {
    private double x, y;
    private double velX, velY;
    private double angle; // Ángulo de dirección
    private final Color color;
    private volatile boolean active = true;
    private static final int SIZE = 5;
    private static final double SPEED = 12.0; // Velocidad rápida
    private static final Color PROJECTILE_COLOR = new Color(0, 255, 255); // Cyan brillante
    
    /**
     * Constructor para disparos del jugador.
     */
    public Projectile(double startX, double startY, double angle) {
        this.x = startX;
        this.y = startY;
        this.angle = angle;
        this.velX = Math.cos(angle) * SPEED;
        this.velY = Math.sin(angle) * SPEED;
        this.color = PROJECTILE_COLOR;
    }
    
    /**
     * Constructor antiguo para compatibilidad.
     */
    public Projectile(double startX, double startY, double angle, Color shipColor) {
        this(startX, startY, angle);
    }
    
    public void update(double deltaMs) {
        if (!active) return;
        x += velX;
        y += velY;
    }
    
    public void update() {
        if (!active) return;
        x += velX;
        y += velY;
    }
    
    public void checkBounds(int width, int height) {
        // Sin límites - los proyectiles pueden ir a cualquier parte del mapa
        // Solo se desactivan al impactar con asteroides
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void deactivate() {
        active = false;
    }
    
    public double getX() { return x; }
    public double getY() { return y; }
    public double getAngle() { return angle; }
    public int getSize() { return SIZE; }
    public int getRadius() { return SIZE; }
    public Color getColor() { return color; }
}
