package services;

import model.ScoreManager;

/**
 * Servicio de puntuación y combos.
 * Wrapper sobre ScoreManager para la capa de servicios.
 */
public class ScoreService {
    
    private final ScoreManager scoreManager;
    
    public ScoreService() {
        this.scoreManager = new ScoreManager();
    }
    
    /**
     * Añade puntos por destruir asteroide.
     */
    public void addAsteroidDestroyed(int points) {
        scoreManager.addScore(points);
    }
    
    /**
     * Añade puntos por entrega de paquete y actualiza combo.
     * [MVC] Tiempo inyectado como parámetro.
     */
    public void addDelivery(int basePoints, long currentTime) {
        double multiplier = scoreManager.getMultiplier();
        int finalPoints = (int)(basePoints * multiplier);
        scoreManager.addScore(finalPoints);
        scoreManager.addDelivery(currentTime);
    }
    
    /**
     * Resetea el combo.
     */
    public void resetCombo() {
        scoreManager.breakCombo();
    }
    
    /**
     * Obtiene puntuación total.
     */
    public int getTotalScore() {
        return scoreManager.getScore();
    }
    
    /**
     * Obtiene combo actual.
     */
    public int getCurrentCombo() {
        return scoreManager.getCurrentCombo();
    }
    
    /**
     * Obtiene combo máximo alcanzado.
     */
    public int getMaxCombo() {
        return scoreManager.getMaxCombo();
    }
    
    /**
     * Actualiza el sistema de combos (verifica timeout).
     * [MVC] Tiempo inyectado como parámetro.
     */
    public void update(long currentTime) {
        scoreManager.updateCombo(currentTime);
    }
    
    /**
     * Calcula puntuación final del nivel.
     */
    public int calculateFinalScore(int level, int remainingTime) {
        return scoreManager.calculateFinalScore(level, remainingTime);
    }
    
    /**
     * Obtiene el ScoreManager subyacente (para compatibilidad).
     */
    public ScoreManager getScoreManager() {
        return scoreManager;
    }
}
