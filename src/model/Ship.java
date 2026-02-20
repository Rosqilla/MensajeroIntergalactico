package model;

import java.awt.Color;
import java.awt.geom.Point2D;

/**
 * Representa una nave espacial controlable con vidas y boost.
 * Movimiento arcade simple: 8 direcciones sin rotación.
 * Soporte para red P2P con diferenciación de naves locales/remotas.
 */
public class Ship {
    private final Point2D.Double position;
    private final Point2D.Double velocity;
    private double rotationAngle; // Ángulo de rotación en radianes
    
    // Propiedades P2P
    private final String shipId;
    private final String ownerId;
    private final boolean isRemote;
    private int tintColorRGB;
    
    // Sistema de vidas (arcade)
    private int lives;
    private static final int MAX_LIVES = 5;
    private boolean immune;
    private long immunityEndTime;
    private static final long IMMUNITY_DURATION_MS = 2000; // 2 segundos
    
    // Sistema de boost (no combustible)
    private double boost;
    private static final double MAX_BOOST = 100.0;
    private static final double BOOST_ACTIVATION_THRESHOLD = 0.0; // Puede activar hasta llegar a 0%
    private boolean boostActive;
    private boolean canBoost;
    
    // [MVC] Constantes de física movidas a PhysicsService
    
    // Sistema de paquetes
    private Package carriedPackage;
    
    // Sistema de disparo
    private long lastShotTime;
    private static final long SHOT_COOLDOWN_MS = 300; // Cadencia reducida a la mitad
    
    /**
     * Constructor para nave local (controlada por el jugador).
     */
    public Ship(double x, double y) {
        this(x, y, "LocalShip_" + System.currentTimeMillis(), "Local", false);
    }
    
    /**
     * Constructor para nave remota (de otro peer).
     */
    public Ship(double x, double y, String shipId, String ownerId, boolean isRemote) {
        this.position = new Point2D.Double(x, y);
        this.velocity = new Point2D.Double(0, 0);
        this.shipId = shipId;
        this.ownerId = ownerId;
        this.isRemote = isRemote;
        this.tintColorRGB = isRemote ? generateTintColor(ownerId).getRGB() : 0;
        this.lives = 3;
        this.boost = MAX_BOOST;
        this.immune = false;
        this.immunityEndTime = 0;
        this.boostActive = false;
        this.canBoost = true;
        this.carriedPackage = null;
        this.lastShotTime = 0;
    }
    
    /**
     * Genera un color de tinte único basado en el ownerId.
     */
    private static Color generateTintColor(String ownerId) {
        // Usar hash del ID para generar color consistente
        int hash = ownerId.hashCode();
        
        // Colores vibrantes para naves remotas
        Color[] tints = {
            new Color(255, 100, 100),  // Rojo
            new Color(100, 150, 255),  // Azul
            new Color(100, 255, 100),  // Verde
            new Color(255, 255, 100),  // Amarillo
            new Color(255, 100, 255),  // Magenta
            new Color(100, 255, 255),  // Cian
            new Color(255, 150, 50),   // Naranja
            new Color(200, 100, 255)   // Violeta
        };
        
        return tints[Math.abs(hash) % tints.length];
    }
    
    // [MVC] Métodos de comportamiento movidos a PhysicsService
    // rotate() -> PhysicsService.applyRotation()
    // emergencyBrake() -> PhysicsService.applyBrake()
    // thrust() -> PhysicsService.applyThrust()
    // limitSpeed() -> PhysicsService.limitSpeed()
    
    /**
     * Activa/desactiva el boost.
     */
    public void setBoostActive(boolean active) {
        if (active && canBoost && boost >= BOOST_ACTIVATION_THRESHOLD) {
            boostActive = true;
        } else {
            boostActive = false;
        }
    }
    
    // [MVC] Lógica de actualización movida a PhysicsService.updateShipPhysics()
    
    /**
     * Recibe un golpe. Reduce vidas y activa inmunidad temporal.
     * [MVC] Tiempo inyectado como parámetro.
     */
    public void takeHit(long currentTime) {
        if (!immune && lives > 0) {
            lives--;
            immune = true;
            immunityEndTime = currentTime + IMMUNITY_DURATION_MS;
        }
    }
    
    /**
     * Recibe múltiples golpes (para asteroides medianos).
     * [MVC] Tiempo inyectado como parámetro.
     */
    public void takeHit(int damage, long currentTime) {
        for (int i = 0; i < damage; i++) {
            takeHit(currentTime);
            if (lives <= 0) break;
        }
    }
    
    /**
     * Añade una vida extra.
     */
    public void addLife() {
        if (lives < MAX_LIVES) {
            lives++;
        }
    }
    
    /**
     * Recoge un paquete.
     */
    public void pickupPackage(Package pkg) {
        this.carriedPackage = pkg;
        pkg.setPickedUp(true);
    }
    
    /**
     * Entrega el paquete cargado.
     */
    public Package deliverPackage() {
        Package delivered = this.carriedPackage;
        this.carriedPackage = null;
        return delivered;
    }
    
    /**
     * Verifica si lleva un paquete.
     */
    public boolean hasPackage() {
        return carriedPackage != null;
    }
    
    /**
     * Obtiene el paquete cargado.
     */
    public Package getCarriedPackage() {
        return carriedPackage;
    }
    
    // Getters
    public Point2D.Double getPosition() {
        return position;
    }
    
    public double getX() {
        return position.x;
    }
    
    public double getY() {
        return position.y;
    }
    
    public Point2D.Double getVelocity() {
        return velocity;
    }
    
    public int getLives() {
        return lives;
    }
    
    public void resetLives() {
        this.lives = 3; // Resetear a 3 vidas
    }
    
    public double getBoost() {
        return boost;
    }
    
    public double getMaxBoost() {
        return MAX_BOOST;
    }
    
    public boolean isBoostActive() {
        return boostActive;
    }
    
    public boolean canBoost() {
        return canBoost && boost >= BOOST_ACTIVATION_THRESHOLD;
    }
    
    public boolean isImmune() {
        return immune;
    }
    
    /**
     * Verifica y actualiza el estado de inmunidad.
     * [MVC] Tiempo inyectado como parámetro.
     */
    public boolean isImmune(long currentTime) {
        if (immune && currentTime >= immunityEndTime) {
            immune = false;
        }
        return immune;
    }
    
    public double getRotationAngle() {
        return rotationAngle;
    }
    
    public boolean isAlive() {
        return lives > 0;
    }
    
    /**
     * Intenta disparar un proyectil.
     * [MVC] Tiempo inyectado como parámetro.
     */
    public Projectile shoot(long currentTime) {
        if (currentTime - lastShotTime >= SHOT_COOLDOWN_MS) {
            lastShotTime = currentTime;
            // Crear proyectil en la punta de la nave
            double tipX = position.x + Math.cos(rotationAngle) * 15;
            double tipY = position.y + Math.sin(rotationAngle) * 15;
            return new Projectile(tipX, tipY, rotationAngle);
        }
        return null;
    }
    
    /**
     * Verifica si puede disparar.
     * [MVC] Tiempo inyectado como parámetro.
     */
    public boolean canShoot(long currentTime) {
        return currentTime - lastShotTime >= SHOT_COOLDOWN_MS;
    }
    

    
    /**
     * Establece la posición de la nave.
     */
    public void setPosition(double x, double y) {
        position.x = x;
        position.y = y;
    }
    
    /**
     * Recarga el boost completamente.
     */
    public void refillBoost() {
        boost = MAX_BOOST;
        canBoost = true;
    }
    
    // ========== Métodos P2P ==========
    
    public String getShipId() {
        return shipId;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public boolean isRemote() {
        return isRemote;
    }
    
    public int getTintColorRGB() {
        return tintColorRGB;
    }
    
    public Color getTintColor() {
        return new Color(tintColorRGB);
    }
    
    /**
     * Actualiza estado desde datos de red (para naves remotas).
     */
    public void updateFromNetwork(double x, double y, double vx, double vy, double angle, int lives, boolean hasPackage) {
        if (!isRemote) {
            return; // Solo actualizar naves remotas
        }
        
        // Interpolación suave para compensar latencia
        position.x = position.x * 0.7 + x * 0.3;
        position.y = position.y * 0.7 + y * 0.3;
        velocity.x = vx;
        velocity.y = vy;
        rotationAngle = angle;
        this.lives = lives;
        
        // Nota: Los paquetes de naves remotas no se sincronizan completamente
        // Solo se muestra si tienen uno o no
    }
    
    /**
     * Obtiene velocidad X.
     */
    public double getVelX() {
        return velocity.x;
    }
    
    /**
     * Obtiene velocidad Y.
     */
    public double getVelY() {
        return velocity.y;
    }
    
    // ===== SETTERS PARA SERVICES LAYER =====
    
    /**
     * Establece número de vidas.
     */
    public void setLives(int lives) {
        this.lives = Math.max(0, Math.min(lives, MAX_LIVES));
    }
    
    /**
     * Establece estado de inmunidad.
     */
    public void setImmune(boolean immune) {
        this.immune = immune;
    }
    
    /**
     * Establece tiempo de fin de inmunidad.
     */
    public void setImmunityEndTime(long immunityEndTime) {
        this.immunityEndTime = immunityEndTime;
    }
    
    /**
     * Obtiene tiempo de fin de inmunidad.
     */
    public long getImmunityEndTime() {
        return immunityEndTime;
    }
    
    /**
     * Establece velocidad.
     */
    public void setVelocity(double vx, double vy) {
        this.velocity.x = vx;
        this.velocity.y = vy;
    }
    
    /**
     * Establece ángulo de rotación en radianes.
     */
    public void setRotationAngle(double angle) {
        this.rotationAngle = angle;
    }
    
    /**
     * Establece paquete transportado.
     */
    public void setCarriedPackage(model.Package pkg) {
        this.carriedPackage = pkg;
    }
    
    /**
     * [BUG FIX] Establece el nivel de boost.
     */
    public void setBoost(double boost) {
        this.boost = Math.max(0, Math.min(100.0, boost));
    }
    
    /**
     * [BUG FIX] Establece si puede activar el boost.
     */
    public void setCanBoost(boolean canBoost) {
        this.canBoost = canBoost;
    }
}
