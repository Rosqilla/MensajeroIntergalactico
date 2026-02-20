package model;

import java.awt.Point;
import java.util.List;

/**
 * Interfaz de solo lectura del modelo de juego para la Vista (patrón MVC).
 * La vista solo puede consultar estado, nunca modificarlo directamente.
 * Las mutaciones como getAndClearExplosions() son gestionadas por el controlador
 * y expuestas aquí como snapshots inmutables.
 */
public interface IGameModel {

    // === Estado de la nave ===
    Ship getPlayerShip();
    List<Ship> getRemoteShips();

    // === Entidades del mundo ===
    List<Planet> getPlanets();
    List<Asteroid> getAsteroids();
    List<Package> getPackages();
    List<Projectile> getProjectiles();
    List<FloatingText> getFloatingTexts();

    // === Explosiones (snapshot, no mutante) ===
    List<Point> getExplosionPositions();
    List<Integer> getExplosionSizes();

    // === Cámara ===
    Point getCameraOffset();

    // === Estado del juego ===
    boolean isGameOver();
    boolean isGameWon();
    int getTotalScore();
    int getRemainingTime(long currentTime);
    int getCurrentLevel();
    List<LevelObjective> getLevelObjectives();
}
