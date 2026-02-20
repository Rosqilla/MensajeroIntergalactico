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
    
    // [MVC] Métodos de comportamiento movidos a PhysicsService:
    // update() -> PhysicsService.updateProjectilePhysics()
    // checkBounds() -> PhysicsService (maneja límites si es necesario)
    // deactivate() -> Setters públicos permiten desactivación desde servicios
    
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
    
    // ===== SETTERS PARA SERVICES LAYER =====
    
    /**
     * Establece estado activo del proyectil.
     */
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * Establece posición del proyectil.
     */
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Establece velocidad del proyectil.
     */
    public void setVelocity(double vx, double vy) {
        this.velX = vx;
        this.velY = vy;
    }
    
    /**
     * Obtiene velocidad X.
     */
    public double getVelX() {
        return velX;
    }
    
    /**
     * Obtiene velocidad Y.
     */
    public double getVelY() {
        return velY;
    }
}
