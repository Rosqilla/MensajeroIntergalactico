package model;

/**
 * Gestiona el sistema de combos y puntuación del juego.
 */
public class ScoreManager {
    private int currentCombo;
    private int maxCombo;
    private int score;
    private long lastDeliveryTime;
    private boolean perfectRun;
    private static final long COMBO_TIMEOUT_MS = 10000; // 10 segundos
    
    // Multiplicadores de combo
    private static final double COMBO_2X = 1.2;
    private static final double COMBO_3X = 1.5;
    private static final double COMBO_4X = 2.0;
    
    // Bonificaciones
    private static final int PERFECT_BONUS = 5000;
    private static final int COMBO_MULTIPLIER = 500;
    private static final int TIME_SCORE_MULTIPLIER = 10;
    
    public ScoreManager() {
        this.currentCombo = 0;
        this.maxCombo = 0;
        this.score = 0;
        this.lastDeliveryTime = System.currentTimeMillis();
        this.perfectRun = true;
    }
    
    /**
     * Actualiza el combo comprobando si ha expirado.
     */
    public void update() {
        if (currentCombo > 0) {
            long timeSinceLastDelivery = System.currentTimeMillis() - lastDeliveryTime;
            if (timeSinceLastDelivery > COMBO_TIMEOUT_MS) {
                breakCombo();
            }
        }
    }
    
    /**
     * Registra una entrega y actualiza el combo.
     */
    public void addDelivery() {
        currentCombo++;
        if (currentCombo > maxCombo) {
            maxCombo = currentCombo;
        }
        lastDeliveryTime = System.currentTimeMillis();
    }
    
    /**
     * Rompe el combo (al chocar contra un asteroide).
     */
    public void breakCombo() {
        currentCombo = 0;
        perfectRun = false;
    }
    
    /**
     * Obtiene el multiplicador actual basado en el combo.
     */
    public double getMultiplier() {
        if (currentCombo >= 4) return COMBO_4X;
        if (currentCombo >= 3) return COMBO_3X;
        if (currentCombo >= 2) return COMBO_2X;
        return 1.0;
    }
    
    /**
     * Calcula la puntuación final.
     * @param level Nivel actual
     * @param remainingTime Tiempo restante en segundos
     * @return Puntuación total
     */
    public int calculateFinalScore(int level, int remainingTime) {
        int baseScore = 1000 * level;
        int timeBonus = remainingTime * TIME_SCORE_MULTIPLIER;
        int comboBonus = maxCombo * COMBO_MULTIPLIER;
        int perfectBonus = perfectRun ? PERFECT_BONUS : 0;
        
        return baseScore + timeBonus + comboBonus + perfectBonus;
    }
    
    /**
     * Añade puntos a la puntuación actual.
     */
    public void addScore(int points) {
        score += points;
    }
    
    // Getters
    public int getCurrentCombo() {
        return currentCombo;
    }
    
    public int getMaxCombo() {
        return maxCombo;
    }
    
    public int getScore() {
        return score;
    }
    
    public boolean isPerfectRun() {
        return perfectRun;
    }
    
    public String getComboText() {
        if (currentCombo < 2) return "";
        return "COMBO x" + currentCombo + "!";
    }
    
    public String getMultiplierText() {
        double mult = getMultiplier();
        if (mult == 1.0) return "";
        return String.format("%.1fx", mult);
    }
}
