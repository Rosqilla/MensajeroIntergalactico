package services;

import model.Ship;
import model.Asteroid;
import model.Projectile;
import model.Planet;
import java.awt.geom.Point2D;

/**
 * Servicio de física: maneja movimiento, colisiones y fuerzas.
 * Separa la lógica de física del modelo de datos.
 */
public class PhysicsService {
    
    // Constantes de boost (consumo y recarga por frame a ~60fps)
    private static final double BOOST_CONSUMPTION_PER_FRAME = 0.167; // 10.0 por segundo
    private static final double BOOST_RECHARGE_PER_FRAME = 0.333;    // 20.0 por segundo
    private static final double BOOST_RECHARGE_THRESHOLD = 30.0;     // Espera hasta 30% para recargar
    
    private static final double MAP_WIDTH = 2400;
    private static final double MAP_HEIGHT = 1800;
    
    /**
     * Actualiza movimiento de la nave aplicando física.
     */
    public void updateShipPhysics(Ship ship) {
        if (ship == null || ship.isRemote()) return;
        
        Point2D.Double vel = ship.getVelocity();
        Point2D.Double pos = ship.getPosition();
        
        // Aplicar fricción
        vel.x *= 0.96;
        vel.y *= 0.96;
        
        // Actualizar posición
        pos.x += vel.x;
        pos.y += vel.y;
        
        // Wraparound
        if (pos.x < 0) pos.x += MAP_WIDTH;
        if (pos.x > MAP_WIDTH) pos.x -= MAP_WIDTH;
        if (pos.y < 0) pos.y += MAP_HEIGHT;
        if (pos.y > MAP_HEIGHT) pos.y -= MAP_HEIGHT;
    }
    
    /**
     * Aplica empuje a la nave en la dirección especificada.
     */
    public void applyThrust(Ship ship, double thrustValue, boolean boostActive) {
        if (ship == null || ship.getLives() <= 0) return;
        
        double power = 0.35 * thrustValue;
        
        if (thrustValue < 0) {
            power *= 0.6; // Marcha atrás más lenta
        }
        
        if (boostActive) {
            power = 0.35 * 2.5; // Boost fuerza adelante
        }
        
        Point2D.Double vel = ship.getVelocity();
        double angle = ship.getRotationAngle();
        vel.x += Math.cos(angle) * power;
        vel.y += Math.sin(angle) * power;
        
        limitSpeed(ship, boostActive);
    }
    
    /**
     * Aplica rotación a la nave.
     */
    public void applyRotation(Ship ship, double direction) {
        if (ship == null || ship.getLives() <= 0) return;
        double newAngle = ship.getRotationAngle() + direction * 0.08;
        ship.setRotationAngle(newAngle);
    }
    
    /**
     * Aplica freno de emergencia.
     */
    public void applyBrake(Ship ship) {
        if (ship == null || ship.getLives() <= 0) return;
        Point2D.Double vel = ship.getVelocity();
        vel.x *= 0.93;
        vel.y *= 0.93;
    }
    
    /**
     * Actualiza el sistema de boost (consumo y recarga).
     * [BUG FIX] Implementa consumo cuando está activo y recarga cuando no lo está.
     */
    public void updateBoost(Ship ship) {
        if (ship == null || ship.isRemote()) return;
        
        double currentBoost = ship.getBoost();
        
        if (ship.isBoostActive() && currentBoost > 0) {
            // Consumir boost cuando está activo
            currentBoost -= BOOST_CONSUMPTION_PER_FRAME;
            if (currentBoost < 0) currentBoost = 0;
            ship.setBoost(currentBoost);
            
            // Si se agota, desactivar boost automáticamente
            if (currentBoost <= 0) {
                ship.setBoostActive(false);
                ship.setCanBoost(false); // No puede reactivar hasta recuperar
            }
        } else if (!ship.isBoostActive() && currentBoost < 100.0) {
            // Recargar boost cuando no está activo
            // Solo empieza a recargar si cayó por debajo del threshold o ya está recargando
            if (currentBoost < BOOST_RECHARGE_THRESHOLD || ship.canBoost()) {
                currentBoost += BOOST_RECHARGE_PER_FRAME;
                if (currentBoost > 100.0) currentBoost = 100.0;
                ship.setBoost(currentBoost);
                
                // Permitir boost de nuevo cuando llegue al threshold
                if (currentBoost >= BOOST_RECHARGE_THRESHOLD) {
                    ship.setCanBoost(true);
                }
            }
        }
    }
    
    /**
     * Limita la velocidad máxima.
     */
    private void limitSpeed(Ship ship, boolean boostActive) {
        Point2D.Double vel = ship.getVelocity();
        double currentSpeed = Math.sqrt(vel.x * vel.x + vel.y * vel.y);
        double maxSpeed = 8.0 * (boostActive ? 2.5 : 1.0);
        
        if (ship.getCarriedPackage() != null && ship.getCarriedPackage().reducesSpeed()) {
            maxSpeed *= ship.getCarriedPackage().getSpeedMultiplier();
        }
        
        if (currentSpeed > maxSpeed) {
            vel.x = (vel.x / currentSpeed) * maxSpeed;
            vel.y = (vel.y / currentSpeed) * maxSpeed;
        }
    }
    
    /**
     * Actualiza movimiento de asteroides.
     */
    public void updateAsteroidPhysics(Asteroid asteroid) {
        if (asteroid == null || asteroid.isDestroyed()) return;
        
        // Actualizar posición usando velocidad actual
        double newX = asteroid.getX() + asteroid.getVelX();
        double newY = asteroid.getY() + asteroid.getVelY();
        asteroid.setPosition(newX, newY);
        
        // Wraparound
        if (newX < -50) asteroid.setPosition(MAP_WIDTH + 50, newY);
        if (newX > MAP_WIDTH + 50) asteroid.setPosition(-50, newY);
        if (newY < -50) asteroid.setPosition(newX, MAP_HEIGHT + 50);
        if (newY > MAP_HEIGHT + 50) asteroid.setPosition(newX, -50);
    }
    
    /**
     * Actualiza movimiento de proyectiles.
     */
    public void updateProjectilePhysics(Projectile projectile) {
        if (projectile == null || !projectile.isActive()) return;
        
        // Actualizar posición usando velocidad
        double newX = projectile.getX() + projectile.getVelX();
        double newY = projectile.getY() + projectile.getVelY();
        projectile.setPosition(newX, newY);
        
        // Desactivar si sale del mapa
        if (newX < 0 || newX > MAP_WIDTH || newY < 0 || newY > MAP_HEIGHT) {
            projectile.setActive(false);
        }
    }
    
    /**
     * Detecta colisión entre nave y asteroide.
     */
    public boolean checkShipAsteroidCollision(Ship ship, Asteroid asteroid) {
        if (ship == null || asteroid == null || ship.isImmune()) return false;
        
        double dx = ship.getX() - asteroid.getX();
        double dy = ship.getY() - asteroid.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        return distance < (20 + asteroid.getRadius());
    }
    
    /**
     * Detecta colisión entre proyectil y asteroide.
     */
    public boolean checkProjectileAsteroidCollision(Projectile projectile, Asteroid asteroid) {
        if (projectile == null || !projectile.isActive() || asteroid == null) return false;
        
        double dx = projectile.getX() - asteroid.getX();
        double dy = projectile.getY() - asteroid.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        return distance < (5 + asteroid.getRadius());
    }
    
    /**
     * Detecta colisión entre nave y planeta.
     */
    public boolean checkShipPlanetCollision(Ship ship, Planet planet) {
        if (ship == null || planet == null) return false;
        
        double dx = ship.getX() - planet.getX();
        double dy = ship.getY() - planet.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        return distance < (20 + planet.getRadius());
    }
    
    /**
     * Detecta colisión entre nave y paquete.
     */
    public boolean checkShipPackageCollision(Ship ship, model.Package pkg) {
        if (ship == null || pkg == null) return false;
        
        double dx = ship.getX() - pkg.getX();
        double dy = ship.getY() - pkg.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        return distance < 30;
    }
}
