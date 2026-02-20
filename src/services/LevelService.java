package services;

import model.*;
import java.util.List;

/**
 * Servicio de gestión de niveles.
 * Maneja generación, carga y progresión de niveles.
 */
public class LevelService {
    
    private final LevelGenerator generator;
    private int currentLevel;
    
    public LevelService() {
        this.generator = new LevelGenerator();
        this.currentLevel = 1;
    }
    
    /**
     * Genera un nivel completo.
     */
    public GameData generateLevel(int levelNumber) {
        this.currentLevel = levelNumber;
        return generator.generateLevel(levelNumber);
    }
    
    /**
     * Avanza al siguiente nivel.
     */
    public int advanceLevel() {
        currentLevel++;
        return currentLevel;
    }
    
    /**
     * Obtiene el tiempo límite del nivel.
     */
    public int getLevelTimeLimit(int level) {
        // 240 segundos nivel 1, reduce 30s cada nivel
        return Math.max(120, 240 - ((level - 1) * 30));
    }
    
    /**
     * Obtiene bono de tiempo por entrega.
     */
    public int getDeliveryTimeBonus() {
        return 15; // +15 segundos por entrega
    }
    
    /**
     * Obtiene cantidad de asteroides para el nivel.
     */
    public int getAsteroidCount(int level) {
        return 5 + level * 3; // 8, 11, 14, 17, 20...
    }
    
    /**
     * Obtiene cantidad de objetivos del nivel.
     */
    public int getObjectiveCount(int level) {
        return Math.min(2 + level / 2, 5); // 2-5 objetivos
    }
    
    /**
     * Determina si el nivel está completado.
     */
    public boolean isLevelComplete(List<LevelObjective> objectives) {
        return objectives.stream().allMatch(LevelObjective::isCompleted);
    }
    
    /**
     * Calcula puntuación base del nivel.
     */
    public int getLevelBaseScore(int level) {
        return level * 1000; // 1000 puntos por nivel
    }
    
    public int getCurrentLevel() {
        return currentLevel;
    }
    
    public void setCurrentLevel(int level) {
        this.currentLevel = level;
    }
}
