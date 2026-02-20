package services;

import model.Ship;
import model.Asteroid;
import model.Planet;
import java.util.List;

/**
 * Servicio de detección y resolución de colisiones.
 * Centraliza toda la lógica de colisiones del juego.
 */
public class CollisionService {
    
    private final PhysicsService physics;
    private final SpawnService spawner;
    
    public CollisionService(PhysicsService physics, ScoreService scores, SpawnService spawner) {
        this.physics = physics;
        this.spawner = spawner;
    }
    
    /**
     * Resultado de colisión nave-asteroide.
     */
    public static class ShipAsteroidResult {
        public boolean collision;
        public boolean shipDestroyed;
        
        public ShipAsteroidResult(boolean collision, boolean shipDestroyed) {
            this.collision = collision;
            this.shipDestroyed = shipDestroyed;
        }
    }
    
    /**
     * Resultado de colisión nave-planeta (entrega).
     */
    public static class DeliveryResult {
        public boolean delivered;
        public Planet targetPlanet;
        public model.Package packageDelivered;
        public int pointsEarned;
        public int timeBonus;
    }
    
    /**
     * Detecta todas las colisiones nave-asteroides.
     * [MVC] Tiempo inyectado como parámetro.
     */
    public ShipAsteroidResult checkShipAsteroidCollisions(Ship ship, List<Asteroid> asteroids, long currentTime) {
        if (ship == null || ship.isImmune() || asteroids == null) {
            return new ShipAsteroidResult(false, false);
        }
        
        for (Asteroid asteroid : asteroids) {
            if (physics.checkShipAsteroidCollision(ship, asteroid)) {
                // Usar takeHit con tiempo inyectado
                ship.takeHit(currentTime);
                
                boolean shipDestroyed = ship.getLives() <= 0;
                return new ShipAsteroidResult(true, shipDestroyed);
            }
        }
        
        return new ShipAsteroidResult(false, false);
    }
}

