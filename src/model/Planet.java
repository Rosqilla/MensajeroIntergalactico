package model;

import java.awt.Color;
import java.awt.geom.Point2D;

/**
 * Representa un planeta en el espacio con posición, tamaño y nombre.
 */
public class Planet {
    private final Point2D.Double position;
    private final int radius;
    private final Color color;
    private final String name;
    
    public Planet(double x, double y, int radius, Color color, String name) {
        this.position = new Point2D.Double(x, y);
        this.radius = radius;
        this.color = color;
        this.name = name;
    }
    
    public Planet(double x, double y, String name) {
        this(x, y, 30, generateRandomColor(), name);
    }
    
    private static Color generateRandomColor() {
        return new Color(
            (float) Math.random(),
            (float) Math.random(),
            (float) Math.random()
        );
    }
    
    public Point2D.Double getPosition() {
        return position;
    }
    
    public double getX() {
        return position.x;
    }
    
    public double getY() {
        return position.y;
    }
    
    public int getRadius() {
        return radius;
    }
    
    public Color getColor() {
        return color;
    }
    
    public String getName() {
        return name;
    }
}
