package model;

import java.awt.Color;

/**
 * Representa un texto flotante temporal que aparece en pantalla.
 */
public class FloatingText {
    private String text;
    private double startX, startY;
    private long creationTime;
    private long duration; // en milisegundos
    private Color color;
    private static final double VELOCITY_Y = -0.06; // px/ms ≈ 1px por frame a 60fps
    
    /**
     * Constructor con tiempo inyectado.
     * [MVC] Tiempo inyectado como parámetro para evitar dependencia directa.
     */
    public FloatingText(String text, double x, double y, long duration, Color color, long currentTime) {
        this.text = text;
        this.startX = x;
        this.startY = y;
        this.creationTime = currentTime;
        this.duration = duration;
        this.color = color;
    }
    
    /**
     * Obtiene la posición Y actual calculada dinámicamente.
     * [MVC] Cálculo sin estado mutable.
     */
    public double getY(long currentTime) {
        long elapsed = currentTime - creationTime;
        return startY + (VELOCITY_Y * elapsed);
    }
    
    public String getText() {
        return text;
    }
    
    public double getX() {
        return startX;
    }
    
    public Color getColor() {
        return color;
    }
    
    /**
     * Verifica si el texto ha expirado.
     * [MVC] Tiempo inyectado como parámetro para evitar dependencia directa.
     */
    public boolean isExpired(long currentTime) {
        return currentTime - creationTime > duration;
    }
    
    /**
     * Obtiene la opacidad actual del texto (0-255).
     * [MVC] Tiempo inyectado como parámetro para evitar dependencia directa.
     */
    public int getAlpha(long currentTime) {
        long elapsed = currentTime - creationTime;
        double progress = (double) elapsed / duration;
        // Desvanecimiento lineal
        return (int) (255 * (1.0 - progress));
    }
}
