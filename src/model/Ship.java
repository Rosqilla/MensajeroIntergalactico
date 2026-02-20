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
    private static final double BOOST_CONSUMPTION = 10.0; // Por segundo (aprox 0.167 por frame a 60fps)
    private static final double BOOST_RECHARGE = 20.0; // Por segundo (aprox 0.333 por frame a 60fps)
    private static final double BOOST_ACTIVATION_THRESHOLD = 0.0; // Puede activar hasta llegar a 0%
    private static final double BOOST_RECHARGE_THRESHOLD = 30.0; // Tarda un poco en recuperar boost
    private boolean boostActive;
    private boolean canBoost;
    
    // Constantes de física (movimiento espacial con control fino)
    private static final double THRUST_POWER = 0.35; // Aceleración rápida y satisfactoria
    private static final double REVERSE_MULTIPLIER = 0.6; // Frenar al 60%
    private static final double ROTATION_SPEED = 0.08; // Rotación ágil
    private static final double BOOST_MULTIPLIER = 2.5; // Boost potente
    private static final double FRICTION = 0.96; // Fricción moderada para control
    private static final double EMERGENCY_BRAKE = 0.93; // Freno suave y progresivo (reduce 7% por frame)
    private static final double MAX_SPEED = 8.0; // Velocidad máxima alta
    
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
    
    /**
     * Rota la nave.
     * @param direction -1 izquierda, 1 derecha
     */
    public void rotate(double direction) {
        if (lives <= 0) return;
        rotationAngle += direction * ROTATION_SPEED;
    }
    
    /**
     * Aplica freno de emergencia (reduce velocidad drásticamente).
     */
    public void emergencyBrake() {
        if (lives <= 0) return;
        velocity.x *= EMERGENCY_BRAKE;
        velocity.y *= EMERGENCY_BRAKE;
    }
    
    /**
     * Aplica empuje en la dirección que apunta la nave.
     * @param thrust 1 adelante, -1 atrás, 0 sin empuje
     */
    public void thrust(double thrust) {
        if (lives <= 0) return;
        
        double power = THRUST_POWER * thrust;
        
        // Marcha atrás más lenta
        if (thrust < 0) {
            power *= REVERSE_MULTIPLIER;
        }
        
        // BOOST: siempre empuja hacia adelante automáticamente
        if (boostActive) {
            // Boost fuerza empuje hacia adelante (anula marcha atrás)
            power = THRUST_POWER * BOOST_MULTIPLIER;
        }
        
        // Aplicar aceleración en la dirección de rotación
        velocity.x += Math.cos(rotationAngle) * power;
        velocity.y += Math.sin(rotationAngle) * power;
        
        // Limitar velocidad
        limitSpeed();
    }
    
    /**
     * Limita la velocidad máxima de la nave.
     */
    private void limitSpeed() {
        double currentSpeed = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
        double maxSpeed = MAX_SPEED * (boostActive ? BOOST_MULTIPLIER : 1.0);
        
        // Aplicar reducción de velocidad si lleva paquete pesado
        if (carriedPackage != null && carriedPackage.reducesSpeed()) {
            maxSpeed *= carriedPackage.getSpeedMultiplier();
        }
        
        if (currentSpeed > maxSpeed) {
            velocity.x = (velocity.x / currentSpeed) * maxSpeed;
            velocity.y = (velocity.y / currentSpeed) * maxSpeed;
        }
    }
    
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
    
    /**
     * Actualiza la posición de la nave, boost e inmunidad.
     */
    public void update() {
        // Actualizar inmunidad
        if (immune && System.currentTimeMillis() > immunityEndTime) {
            immune = false;
        }
        
        // Actualizar boost
        if (boostActive && boost > 0) {
            boost -= BOOST_CONSUMPTION / 60.0; // Ajustado para 60 FPS
            if (boost <= 0) {
                boost = 0;
                boostActive = false;
                canBoost = false; // No puede volver a activar hasta recargar al 50%
            }
        } else if (!boostActive && boost < MAX_BOOST) {
            boost += BOOST_RECHARGE / 60.0; // Ajustado para 60 FPS
            if (boost > MAX_BOOST) {
                boost = MAX_BOOST;
            }
            // Permitir boost de nuevo al llegar al 50%
            if (!canBoost && boost >= BOOST_RECHARGE_THRESHOLD) {
                canBoost = true;
            }
        }
        
        // Aplicar fricción suave
        velocity.x *= FRICTION;
        velocity.y *= FRICTION;
        
        // Actualizar posición
        position.x += velocity.x;
        position.y += velocity.y;
    }
    
    /**
     * Recibe un golpe. Reduce vidas y activa inmunidad temporal.
     */
    public void takeHit() {
        if (!immune && lives > 0) {
            lives--;
            immune = true;
            immunityEndTime = System.currentTimeMillis() + IMMUNITY_DURATION_MS;
        }
    }
    
    /**
     * Recibe múltiples golpes (para asteroides medianos).
     */
    public void takeHit(int damage) {
        for (int i = 0; i < damage; i++) {
            takeHit();
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
    
    public double getRotationAngle() {
        return rotationAngle;
    }
    
    public boolean isAlive() {
        return lives > 0;
    }
    
    /**
     * Intenta disparar un proyectil.
     */
    public Projectile shoot() {
        long now = System.currentTimeMillis();
        if (now - lastShotTime >= SHOT_COOLDOWN_MS) {
            lastShotTime = now;
            // Crear proyectil en la punta de la nave
            double tipX = position.x + Math.cos(rotationAngle) * 15;
            double tipY = position.y + Math.sin(rotationAngle) * 15;
            return new Projectile(tipX, tipY, rotationAngle);
        }
        return null;
    }
    
    /**
     * Verifica si puede disparar.
     */
    public boolean canShoot() {
        return System.currentTimeMillis() - lastShotTime >= SHOT_COOLDOWN_MS;
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
}
