package model;

import java.awt.geom.Point2D;

/**
 * Representa un asteroide con diferentes tipos y comportamientos.
 */
public class Asteroid {
    public enum AsteroidType {
        SMALL,      // 1 vida de daño, fácil de esquivar
        MEDIUM,     // 2 vidas de daño, puede dividirse
        LARGE,      // Inmóvil o muy lento, obstáculo ambiental
        CHASER      // Persigue a la nave lentamente
    }
    
    private final Point2D.Double position;
    private double velX, velY;
    private final int radius;
    private final AsteroidType type;
    private boolean destroyed;
    private double health;
    private long lastHitTime; // Tiempo del último golpe recibido
    
    // Constantes de tamaño
    private static final int SMALL_RADIUS = 15;
    private static final int MEDIUM_RADIUS = 25;
    private static final int LARGE_RADIUS = 40;
    private static final int CHASER_RADIUS = 20;
    
    // Velocidad del perseguidor
    private static final double CHASER_SPEED = 1.0;
    
    public Asteroid(double x, double y, double velX, double velY, int radius) {
        this.position = new Point2D.Double(x, y);
        this.velX = velX;
        this.velY = velY;
        this.radius = radius;
        // Determinar tipo según radio
        if (radius < 20) {
            this.type = AsteroidType.SMALL;
            this.health = 1;
        } else if (radius < 30) {
            this.type = AsteroidType.MEDIUM;
            this.health = 2;
        } else {
            this.type = AsteroidType.LARGE;
            this.health = Double.MAX_VALUE;
        }
        this.destroyed = false;
        this.lastHitTime = 0; // Inicializar sin golpe
    }
    
    public Asteroid(double x, double y, double velX, double velY) {
        this(x, y, velX, velY, 20);
    }
    
    /**
     * Constructor con tipo específico.
     */
    public Asteroid(double x, double y, double velX, double velY, AsteroidType type) {
        this.position = new Point2D.Double(x, y);
        this.velX = velX;
        this.velY = velY;
        this.type = type;
        this.destroyed = false;
        this.lastHitTime = 0; // Inicializar sin golpe
        
        // Asignar radio y salud según tipo
        switch (type) {
            case SMALL:
                this.radius = SMALL_RADIUS;
                this.health = 1;
                break;
            case MEDIUM:
                this.radius = MEDIUM_RADIUS;
                this.health = 2;
                break;
            case LARGE:
                this.radius = LARGE_RADIUS;
                this.health = 5; // 5 tiros para destruir
                break;
            case CHASER:
                this.radius = CHASER_RADIUS;
                this.health = 3;
                break;
            default:
                this.radius = MEDIUM_RADIUS;
                this.health = 2;
        }
    }
    
    public void update() {
        position.x += velX;
        position.y += velY;
    }
    
    /**
     * Actualiza el asteroide perseguidor moviéndose hacia el objetivo.
     */
    public void updateChaser(double targetX, double targetY) {
        if (type != AsteroidType.CHASER) {
            update();
            return;
        }
        
        // Calcular dirección hacia el objetivo
        double dx = targetX - position.x;
        double dy = targetY - position.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0) {
            velX = (dx / distance) * CHASER_SPEED;
            velY = (dy / distance) * CHASER_SPEED;
        }
        
        update();
    }
    
    /**
     * Recibe daño y verifica si debe destruirse.
     */
    public boolean takeDamage(double damage) {
        lastHitTime = System.currentTimeMillis();
        health -= damage;
        
        if (health <= 0) {
            destroyed = true;
            return true;
        }
        
        return false;
    }
    
    /**
     * Verifica si puede dividirse (solo medianos).
     */
    public boolean canSplit() {
        return type == AsteroidType.MEDIUM && health <= 1 && !destroyed;
    }
    
    /**
     * Obtiene el daño que causa al chocar.
     */
    public int getCollisionDamage() {
        switch (type) {
            case SMALL: return 1;
            case MEDIUM: return 2;
            case LARGE: return 1;
            case CHASER: return 1;
            default: return 1;
        }
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
    
    public double getVelX() {
        return velX;
    }
    
    public double getVelY() {
        return velY;
    }
    
    public int getRadius() {
        return radius;
    }
    
    public AsteroidType getType() {
        return type;
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
    
    public double getHealth() {
        return health;
    }
    
    public boolean wasRecentlyHit() {
        return System.currentTimeMillis() - lastHitTime < 2000; // 2 segundos
    }
    
    /**
     * Verifica si el asteroide está fuera de los límites especificados.
     */
    public boolean isOutOfBounds(int width, int height) {
        return position.x + radius < 0 || position.x - radius > width ||
               position.y + radius < 0 || position.y - radius > height;
    }
}
