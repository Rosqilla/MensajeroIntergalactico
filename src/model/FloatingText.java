package model;

import java.awt.Color;

/**
 * Representa un texto flotante temporal que aparece en pantalla.
 */
public class FloatingText {
    private String text;
    private double x, y;
    private long creationTime;
    private long duration; // en milisegundos
    private Color color;
    private double velocityY; // Velocidad de ascenso
    
    public FloatingText(String text, double x, double y, long duration, Color color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.creationTime = System.currentTimeMillis();
        this.duration = duration;
        this.color = color;
        this.velocityY = -1.0; // Asciende lentamente
    }
    
    /**
     * Actualiza la posición del texto.
     */
    public void update() {
        y += velocityY;
    }
    
    /**
     * Verifica si el texto ha expirado.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > duration;
    }
    
    /**
     * Obtiene la opacidad actual del texto (0-255).
     */
    public int getAlpha() {
        long elapsed = System.currentTimeMillis() - creationTime;
        double progress = (double) elapsed / duration;
        // Desvanecimiento lineal
        return (int) (255 * (1.0 - progress));
    }
    
    public String getText() {
        return text;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public Color getColor() {
        return color;
    }
}
