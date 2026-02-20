package model;

/**
 * Representa un objetivo del nivel: entregar N paquetes a un planeta específico.
 */
public class LevelObjective {
    private final Planet targetPlanet;
    private final int totalPackages;
    private int packagesDelivered;
    
    public LevelObjective(Planet targetPlanet, int totalPackages) {
        this.targetPlanet = targetPlanet;
        this.totalPackages = totalPackages;
        this.packagesDelivered = 0;
    }
    
    public Planet getTargetPlanet() {
        return targetPlanet;
    }
    
    public int getTotalPackages() {
        return totalPackages;
    }
    
    public int getPackagesDelivered() {
        return packagesDelivered;
    }
    
    public void incrementDelivered() {
        packagesDelivered++;
    }
    
    public boolean isCompleted() {
        return packagesDelivered >= totalPackages;
    }
    
    public int getRemaining() {
        return totalPackages - packagesDelivered;
    }
}
