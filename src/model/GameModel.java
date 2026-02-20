package model;

import services.LevelGenerator;
import services.GameData;
import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

/**
 * Modelo principal del juego — holder de estado puro (patrón MVC).
 * NO contiene lógica de negocio. Toda la lógica está en {@code controller.GameLogic}.
 * NO llama a {@code System.currentTimeMillis()} — el tiempo se inyecta desde el controlador.
 * Implementa {@link IGameModel} para exponer solo lectura a la vista.
 */
public class GameModel implements IGameModel {
    private Ship playerShip;
    private List<Planet> planets;
    private List<Asteroid> asteroids;
    private List<Package> packages;
    private List<FloatingText> floatingTexts;
    private List<Projectile> projectiles;
    private List<Point> explosionPositions; // Posiciones donde crear explosiones
    private List<Integer> explosionSizes; // Tamaños de las explosiones (radio del asteroide)
    private Point cameraOffset;
    
    // Sistema P2P - Naves remotas
    private Map<String, Ship> remoteShips; // Key: shipId
    
    // Sistema de nivel
    private int currentLevel;
    private LevelGenerator levelGenerator;
    private List<LevelObjective> levelObjectives;
    private boolean gameOver;
    private boolean gameWon;
    
    // Scoring por nivel
    private int damageReceivedThisLevel;
    
    // Sistema de tiempo
    private int levelTime; // Tiempo inicial del nivel en segundos
    private long startTime;
    private long pausedTime;
    private boolean paused;
    private static final int TIME_BONUS_PER_LEVEL = 60; // Segundos bonus al pasar de nivel
    
    // Sistema de puntuación y combos
    private ScoreManager scoreManager;
    
    // Generación de paquetes (constantes usadas solo en GameLogic, campos usados como estado)
    private Random random;

    // Sistema de aparición de asteroides
    private long lastAsteroidSpawn;
    // Cooldown para mensajes de planeta incorrecto
    private long lastIncorrectPlanetMessageTime;
    
    public GameModel(int width, int height, long initTime) {
        this.cameraOffset = new Point(0, 0);
        this.levelTime = 240; // Nivel 1: 240 segundos (4 minutos)
        this.startTime = initTime;
        this.pausedTime = 0;
        this.paused = false;
        this.scoreManager = new ScoreManager();
        this.floatingTexts = new ArrayList<>();
        this.projectiles = new ArrayList<>();
        this.explosionPositions = new ArrayList<>();
        this.explosionSizes = new ArrayList<>();
        this.remoteShips = new ConcurrentHashMap<>(); // Thread-safe para red
        this.random = new Random();
        this.currentLevel = 1;
        this.levelGenerator = new LevelGenerator();
        this.levelObjectives = new ArrayList<>();
        this.gameOver = false;
        this.damageReceivedThisLevel = 0;
        this.lastAsteroidSpawn = initTime;
        
        // Inicializar nivel 1
        loadLevel(1, initTime);
    }
    
    /**
     * Carga un nivel usando el generador.
     * Público para ser invocado desde GameLogic.
     */
    public void loadLevel(int levelNumber, long currentTime) {
        this.currentLevel = levelNumber;
        this.damageReceivedThisLevel = 0;
        
        // Generar nivel
        GameData levelData = levelGenerator.generateLevel(levelNumber);
        
        // Cargar datos
        this.planets = new ArrayList<>(levelData.getPlanets());
        this.asteroids = new ArrayList<>(levelData.getAsteroids());
        
        // Crear objetivos del nivel (2-4 objetivos según nivel)
        generateLevelObjectives();
        
        // Generar paquetes solo para los objetivos
        generatePackagesForObjectives(currentTime);
        
        // [MVC] maxAsteroids eliminado - se gestiona en SpawnService
        
        // Posicionar nave
        Point startPos = levelData.getPlayerStartPosition();
        if (playerShip == null) {
            playerShip = new Ship(startPos.x, startPos.y);
        } else {
            playerShip.setPosition(startPos.x, startPos.y);
            // Reset completo: restaurar vidas y boost al inicio de cada nivel
            playerShip.resetLives(); // Resetear a 3 vidas
            playerShip.refillBoost();
        }
        
        // Reiniciar timer (añadir tiempo bonus si no es nivel 1)
        if (levelNumber > 1) {
            levelTime += TIME_BONUS_PER_LEVEL;
        }
        this.startTime = currentTime;
        this.pausedTime = 0;
        
        // Limpiar proyectiles
        projectiles.clear();
        floatingTexts.clear();
        
        // Mensaje de nivel
        addFloatingText("NIVEL " + levelNumber, startPos.x, startPos.y - 50, Color.YELLOW, currentTime);
    }
    
    /**
     * Genera objetivos aleatorios del nivel.
     */
    private void generateLevelObjectives() {
        levelObjectives.clear();
        
        // TODOS los planetas deben tener entregas (uno por planeta)
        List<Planet> shuffledPlanets = new ArrayList<>(planets);
        java.util.Collections.shuffle(shuffledPlanets);
        
        for (Planet planet : shuffledPlanets) {
            int packageCount = 2 + random.nextInt(3); // 2-4 paquetes por objetivo
            levelObjectives.add(new LevelObjective(planet, packageCount));
        }
    }
    
    /**
     * Genera paquetes solo para los objetivos del nivel.
     */
    private void generatePackagesForObjectives(long currentTime) {
        packages = new ArrayList<>();
        
        // Generar paquetes NORMALES requeridos para objetivos
        for (LevelObjective objective : levelObjectives) {
            Planet targetPlanet = objective.getTargetPlanet();
            
            for (int i = 0; i < objective.getTotalPackages(); i++) {
                // Posición cerca del centro
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = 150 + random.nextDouble() * 200;
                double x = 1200 + Math.cos(angle) * distance;
                double y = 900 + Math.sin(angle) * distance;
                
                // Solo paquetes NORMAL para objetivos
                packages.add(new Package(x, y, targetPlanet, Package.PackageType.NORMAL, currentTime));
            }
        }
        
        // Generar paquetes opcionales (URGENTE y PESADO) para puntos extra
        int bonusPackages = 2 + random.nextInt(3); // 2-4 paquetes bonus
        for (int i = 0; i < bonusPackages; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 200 + random.nextDouble() * 300;
            double x = 1200 + Math.cos(angle) * distance;
            double y = 900 + Math.sin(angle) * distance;
            
            // Elegir planeta aleatorio
            Planet targetPlanet = planets.get(random.nextInt(planets.size()));
            
            // Solo URGENTE o PESADO para bonus
            Package.PackageType type = random.nextBoolean() ? Package.PackageType.URGENT : Package.PackageType.HEAVY;
            packages.add(new Package(x, y, targetPlanet, type, currentTime));
        }
    }
    
    /**
     * Verifica que haya suficientes paquetes para cada objetivo y regenera si faltan.
     */
    // ========== Lógica de negocio EXTRAÍDA a controller.GameLogic ==========
    
    // ========== Métodos de estado puro (setters para GameLogic) ==========

    /**
     * Añade un texto flotante al mundo.
     * Público para ser invocado desde GameLogic.
     */
    public void addFloatingText(String text, double x, double y, Color color, long currentTime) {
        floatingTexts.add(new FloatingText(text, x, y, 2000, color, currentTime));
    }

    /**
     * Añade una explosión visual (posición + radio).
     */
    public void addExplosion(Point position, int radius) {
        explosionPositions.add(position);
        explosionSizes.add(radius);
    }

    /**
     * Rompe el combo actual.
     */
    public void breakCombo() {
        scoreManager.breakCombo();
    }

    // --- Setters necesarios para GameLogic ---
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }
    public void setGameWon(boolean gameWon) { this.gameWon = gameWon; }
    public void setCurrentLevel(int level) { this.currentLevel = level; }
    public void setLevelTime(int seconds) { this.levelTime = seconds; }
    public void setStartTime(long t) { this.startTime = t; }
    public void setPausedTime(long t) { this.pausedTime = t; }
    public void setPausedFlag(boolean p) { this.paused = p; }
    public void setDamageReceivedThisLevel(int d) { this.damageReceivedThisLevel = d; }
    public void setPlayerShip(Ship ship) { this.playerShip = ship; }
    public void setLastAsteroidSpawn(long t) { this.lastAsteroidSpawn = t; }
    public void setLastIncorrectPlanetMessageTime(long t) { this.lastIncorrectPlanetMessageTime = t; }

    public long getLastAsteroidSpawn() { return lastAsteroidSpawn; }
    public long getLastIncorrectPlanetMessageTime() { return lastIncorrectPlanetMessageTime; }
    public int getDamageReceivedThisLevel() { return damageReceivedThisLevel; }

    public void resetScoreManager() { this.scoreManager = new ScoreManager(); }
    

    

    
    // ========== Getters / Setters de estado ==========

    @Override
    public Point getCameraOffset() {
        return cameraOffset;
    }
    
    public void setScreenDimensions(int width, int height) {
        // [MVC] screenWidth/screenHeight eliminados - no se usan
    }
    
    /**
     * Obtiene el tiempo restante en segundos.
     * Tiempo inyectado desde el controlador (sin System.currentTimeMillis).
     */
    @Override
    public int getRemainingTime(long currentTime) {
        if (paused) {
            return (int)((levelTime * 1000 - pausedTime) / 1000);
        }
        long elapsed = currentTime - startTime;
        long remaining = levelTime * 1000 - elapsed;
        return Math.max(0, (int)(remaining / 1000));
    }
    
    /**
     * Pausa/reanuda el tiempo (con tiempo inyectado).
     */
    public void setPaused(boolean pause, long currentTime) {
        if (pause && !paused) {
            pausedTime = currentTime - startTime;
            paused = true;
        } else if (!pause && paused) {
            startTime = currentTime - pausedTime;
            paused = false;
        }
    }
    
    @Override
    public Ship getPlayerShip() {
        return playerShip;
    }
    
    @Override
    public List<Planet> getPlanets() {
        return planets;
    }
    
    @Override
    public List<Asteroid> getAsteroids() {
        return asteroids;
    }
    
    @Override
    public List<Package> getPackages() {
        return packages;
    }
    
    @Override
    public List<FloatingText> getFloatingTexts() {
        return floatingTexts;
    }
    
    public ScoreManager getScoreManager() {
        return scoreManager;
    }
    
    @Override
    public List<Projectile> getProjectiles() {
        return projectiles;
    }
    
    /**
     * Explosiones como snapshot de solo lectura (IGameModel).
     */
    @Override
    public List<Point> getExplosionPositions() {
        return new ArrayList<>(explosionPositions);
    }

    @Override
    public List<Integer> getExplosionSizes() {
        return new ArrayList<>(explosionSizes);
    }

    /**
     * Consume las explosiones pendientes (solo llamado desde el controlador).
     */
    public void clearExplosions() {
        explosionPositions.clear();
        explosionSizes.clear();
    }
    
    @Override
    public int getCurrentLevel() {
        return currentLevel;
    }
    
    @Override
    public List<LevelObjective> getLevelObjectives() {
        return levelObjectives;
    }
    
    @Override
    public boolean isGameOver() {
        return gameOver;
    }
    
    @Override
    public boolean isGameWon() {
        return gameWon;
    }
    
    @Override
    public int getTotalScore() {
        return scoreManager != null ? scoreManager.getScore() : 0;
    }
    
    /**
     * Registra daño recibido para el cálculo de score.
     */
    public void registerDamage(int damage) {
        damageReceivedThisLevel += damage;
    }
    
    // ========== Métodos P2P para Naves Remotas ==========
    
    /**
     * Añade o actualiza una nave remota.
     */
    public void addOrUpdateRemoteShip(String shipId, String ownerId, double x, double y, 
                                      double vx, double vy, double angle, int lives, boolean hasPackage) {
        Ship remoteShip = remoteShips.get(shipId);
        
        if (remoteShip == null) {
            // Crear nueva nave remota
            remoteShip = new Ship(x, y, shipId, ownerId, true);
            remoteShips.put(shipId, remoteShip);
            System.out.println("Nueva nave remota añadida: " + shipId);
        } else {
            // Actualizar nave existente
            remoteShip.updateFromNetwork(x, y, vx, vy, angle, lives, hasPackage);
        }
    }
    
    /**
     * Elimina una nave remota.
     */
    public void removeRemoteShip(String shipId) {
        Ship removed = remoteShips.remove(shipId);
        if (removed != null) {
            System.out.println("Nave remota eliminada: " + shipId);
        }
    }
    
    /**
     * Elimina todas las naves de un peer específico.
     */
    public void removeRemoteShipsByOwner(String ownerId) {
        remoteShips.entrySet().removeIf(entry -> {
            if (entry.getValue().getOwnerId().equals(ownerId)) {
                System.out.println("Nave remota eliminada por desconexión: " + entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Obtiene todas las naves remotas.
     */
    @Override
    public List<Ship> getRemoteShips() {
        return new ArrayList<>(remoteShips.values());
    }
    
    /**
     * Limpia todas las naves remotas.
     */
    public void clearRemoteShips() {
        remoteShips.clear();
    }
}
