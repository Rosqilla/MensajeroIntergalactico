package services;

import model.Asteroid;
import model.Projectile;
import model.Ship;
import model.FloatingText;
import java.util.Random;

/**
 * Servicio de generación (spawn) de entidades.
 * Maneja creación de asteroides, paquetes, proyectiles.
 */
public class SpawnService {
    
    private static final double MAP_WIDTH = 2400;
    private static final double MAP_HEIGHT = 1800;
    private static final double CENTER_X = MAP_WIDTH / 2;
    private static final double CENTER_Y = MAP_HEIGHT / 2;
    
    private final Random random;
    
    public SpawnService() {
        this.random = new Random();
    }
    
    /**
     * Genera un asteroide en los bordes del mapa.
     */
    public Asteroid spawnAsteroid(int level) {
        // Spawn desde fuera de pantalla
        double spawnX, spawnY;
        int edge = random.nextInt(4);
        
        switch(edge) {
            case 0: // Arriba
                spawnX = random.nextDouble() * MAP_WIDTH;
                spawnY = -50;
                break;
            case 1: // Derecha
                spawnX = MAP_WIDTH + 50;
                spawnY = random.nextDouble() * MAP_HEIGHT;
                break;
            case 2: // Abajo
                spawnX = random.nextDouble() * MAP_WIDTH;
                spawnY = MAP_HEIGHT + 50;
                break;
            default: // Izquierda
                spawnX = -50;
                spawnY = random.nextDouble() * MAP_HEIGHT;
                break;
        }
        
        // Velocidad dirigida hacia el centro
        double angle = Math.atan2(CENTER_Y - spawnY, CENTER_X - spawnX);
        double speed = 0.5 + random.nextDouble() * 1.5;
        double velX = speed * Math.cos(angle);
        double velY = speed * Math.sin(angle);
        
        // Tipo según nivel
        Asteroid.AsteroidType type = determineAsteroidType(level);
        
        return new Asteroid(spawnX, spawnY, velX, velY, type);
    }
    
    /**
     * Determina el tipo de asteroide según nivel.
     */
    private Asteroid.AsteroidType determineAsteroidType(int level) {
        double roll = random.nextDouble();
        
        if (level >= 7 && roll < 0.1) {
            return Asteroid.AsteroidType.CHASER;
        } else if (level >= 4 && roll < 0.3) {
            return Asteroid.AsteroidType.LARGE;
        } else if (level >= 3 && roll < 0.5) {
            return Asteroid.AsteroidType.MEDIUM;
        } else {
            return Asteroid.AsteroidType.SMALL;
        }
    }
    
    /**
     * Genera un paquete en posición aleatoria cerca del centro.
     * Requiere un planeta objetivo y la lista de planetas disponibles.
     */
    public model.Package spawnPackage(java.util.List<model.Planet> planets) {
        if (planets == null || planets.isEmpty()) return null;
        
        double x = CENTER_X + (random.nextDouble() - 0.5) * 600;
        double y = CENTER_Y + (random.nextDouble() - 0.5) * 600;
        
        // Seleccionar planeta objetivo aleatorio
        model.Planet targetPlanet = planets.get(random.nextInt(planets.size()));
        
        // Tipo según probabilidad
        double roll = random.nextDouble();
        model.Package.PackageType type;
        
        if (roll < 0.05) {
            type = model.Package.PackageType.SPECIAL;
        } else if (roll < 0.20) {
            type = model.Package.PackageType.HEAVY;
        } else if (roll < 0.45) {
            type = model.Package.PackageType.URGENT;
        } else {
            type = model.Package.PackageType.NORMAL;
        }
        
        return new model.Package(x, y, targetPlanet, type, System.currentTimeMillis());
    }
    
    /**
     * Crea un proyectil desde la nave.
     */
    public Projectile createProjectile(Ship ship) {
        if (ship == null) return null;
        
        double angle = ship.getRotationAngle();
        
        // Posición en la punta de la nave
        double projX = ship.getX() + Math.cos(angle) * 15;
        double projY = ship.getY() + Math.sin(angle) * 15;
        
        // Crear proyectil con el constructor estándar (x, y, angle)
        return new Projectile(projX, projY, angle);
    }
    
    /**
     * Genera texto flotante.
     * [MVC] Tiempo inyectado como parámetro.
     */
    public FloatingText createFloatingText(String text, double x, double y, java.awt.Color color, long currentTime) {
        return new FloatingText(text, x, y, 2000L, color, currentTime);
    }
}
