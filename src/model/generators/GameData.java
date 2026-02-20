package model.generators;

import java.awt.Point;
import java.util.List;

/**
 * Contiene todos los datos generados para un nivel.
 */
public class GameData {
    private final List<model.Planet> planets;
    private final List<model.Asteroid> asteroids;
    private final List<model.Package> packages;
    private final Point playerStartPosition;
    
    public GameData(List<model.Planet> planets, List<model.Asteroid> asteroids, 
                    List<model.Package> packages,
                    Point playerStartPosition) {
        this.planets = planets;
        this.asteroids = asteroids;
        this.packages = packages;
        this.playerStartPosition = playerStartPosition;
    }
    
    public List<model.Planet> getPlanets() {
        return planets;
    }
    
    public List<model.Asteroid> getAsteroids() {
        return asteroids;
    }
    
    public List<model.Package> getPackages() {
        return packages;
    }
    
    public Point getPlayerStartPosition() {
        return playerStartPosition;
    }
}
