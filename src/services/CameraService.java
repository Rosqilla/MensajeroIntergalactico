package services;

import model.Ship;

/**
 * Servicio de gestión de cámara.
 * Maneja smooth scrolling y seguimiento de la nave del jugador.
 */
public class CameraService {
    
    private static final int MAP_WIDTH = 2400;
    private static final int MAP_HEIGHT = 1800;
    private static final double CAMERA_SMOOTHING = 0.1; // Factor de suavizado
    
    private double cameraX;
    private double cameraY;
    
    public CameraService() {
        this.cameraX = 0;
        this.cameraY = 0;
    }
    
    /**
     * Actualiza la posición de la cámara para seguir al jugador.
     * Usa interpolación para smooth scrolling.
     * 
     * @param playerShip Nave del jugador a seguir
     * @param viewWidth Ancho de la ventana de visualización
     * @param viewHeight Alto de la ventana de visualización
     */
    public void updateCamera(Ship playerShip, int viewWidth, int viewHeight) {
        if (playerShip == null) {
            return;
        }
        
        // Posición ideal de la cámara (nave centrada)
        double targetCameraX = playerShip.getX() - viewWidth / 2.0;
        double targetCameraY = playerShip.getY() - viewHeight / 2.0;
        
        // Limitar cámara a los bordes del mapa
        targetCameraX = Math.max(0, Math.min(targetCameraX, MAP_WIDTH - viewWidth));
        targetCameraY = Math.max(0, Math.min(targetCameraY, MAP_HEIGHT - viewHeight));
        
        // Smooth scrolling (interpolación)
        cameraX += (targetCameraX - cameraX) * CAMERA_SMOOTHING;
        cameraY += (targetCameraY - cameraY) * CAMERA_SMOOTHING;
    }
    
    /**
     * Obtiene posición X de la cámara.
     */
    public double getCameraX() {
        return cameraX;
    }
    
    /**
     * Obtiene posición Y de la cámara.
     */
    public double getCameraY() {
        return cameraY;
    }
    
    /**
     * Establece posición de la cámara directamente.
     */
    public void setCamera(double x, double y) {
        this.cameraX = x;
        this.cameraY = y;
    }
    
    /**
     * Reinicia posición de la cámara.
     */
    public void reset() {
        this.cameraX = 0;
        this.cameraY = 0;
    }
    
    /**
     * Convierte coordenadas del mundo a coordenadas de pantalla.
     */
    public int worldToScreenX(double worldX) {
        return (int)(worldX - cameraX);
    }
    
    /**
     * Convierte coordenadas del mundo a coordenadas de pantalla.
     */
    public int worldToScreenY(double worldY) {
        return (int)(worldY - cameraY);
    }
    
    /**
     * Convierte coordenadas de pantalla a coordenadas del mundo.
     */
    public double screenToWorldX(int screenX) {
        return screenX + cameraX;
    }
    
    /**
     * Convierte coordenadas de pantalla a coordenadas del mundo.
     */
    public double screenToWorldY(int screenY) {
        return screenY + cameraY;
    }
}
