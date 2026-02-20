package model;

import model.generators.LevelGenerator;
import model.generators.GameData;
import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

/**
 * Modelo principal del juego que gestiona la nave, planetas, asteroides y paquetes.
 */
public class GameModel {
    private Ship playerShip;
    private List<Planet> planets;
    private List<Asteroid> asteroids;
    private List<Package> packages;
    private List<FloatingText> floatingTexts;
    private List<Projectile> projectiles;
    private List<Point> explosionPositions; // Posiciones donde crear explosiones
    private List<Integer> explosionSizes; // Tamaños de las explosiones (radio del asteroide)
    private Point cameraOffset;
    private Point targetCameraOffset;
    private int screenWidth;
    private int screenHeight;
    
    // Sistema P2P - Naves remotas
    private Map<String, Ship> remoteShips; // Key: shipId
    
    // Sistema de nivel
    private int currentLevel;
    private LevelGenerator levelGenerator;
    private List<LevelObjective> levelObjectives;
    private boolean gameOver;
    private boolean gameWon;
    private static final int MAX_LEVELS = 5;
    
    // Scoring por nivel
    private long levelStartTime;
    private int damageReceivedThisLevel;
    
    // Sistema de tiempo
    private int levelTime; // Tiempo inicial del nivel en segundos
    private long startTime;
    private long pausedTime;
    private boolean paused;
    private static final int TIME_BONUS_PER_LEVEL = 60; // Segundos bonus al pasar de nivel
    
    // Sistema de puntuación y combos
    private ScoreManager scoreManager;
    
    // Generación de paquetes
    private Random random;
    private static final double MIN_SPAWN_DISTANCE = 200; // Distancia mínima de la nave
    private static final double MAX_SPAWN_DISTANCE = 400; // Distancia máxima de la nave
    private static final int PICKUP_RADIUS = 40; // Radio para recoger paquetes
    
    // Sistema de aparición de asteroides
    private long lastAsteroidSpawn;
    private static final long ASTEROID_SPAWN_INTERVAL = 1500; // 1.5 segundos
    private int maxAsteroids; // Máximo de asteroides en pantalla    
    // Cooldown para mensajes de planeta incorrecto
    private long lastIncorrectPlanetMessageTime;    
    // Probabilidades de tipos de paquetes
    private static final double SPECIAL_PACKAGE_CHANCE = 0.05; // 5%
    private static final double URGENT_PACKAGE_CHANCE = 0.25; // 25%
    private static final double HEAVY_PACKAGE_CHANCE = 0.15; // 15%
    // NORMAL: 55% (resto)
    
    // Suavidad de la cámara (ajustada para movimiento tipo Asteroids)
    private static final double CAMERA_SMOOTHNESS = 0.15;
    
    public GameModel(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.cameraOffset = new Point(0, 0);
        this.targetCameraOffset = new Point(0, 0);
        this.levelTime = 240; // Nivel 1: 240 segundos (4 minutos)
        this.startTime = System.currentTimeMillis();
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
        this.lastAsteroidSpawn = System.currentTimeMillis();
        
        // Inicializar nivel 1
        loadLevel(1);
    }
    
    /**
     * Carga un nivel usando el generador.
     */
    private void loadLevel(int levelNumber) {
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
        generatePackagesForObjectives();
        
        // Configurar máximo de asteroides según nivel
        maxAsteroids = 10 + (currentLevel * 5); // Aumenta con el nivel
        
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
        this.startTime = System.currentTimeMillis();
        this.pausedTime = 0;
        
        // Limpiar proyectiles
        projectiles.clear();
        floatingTexts.clear();
        
        // Mensaje de nivel
        addFloatingText("NIVEL " + levelNumber, startPos.x, startPos.y - 50, Color.YELLOW);
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
    private void generatePackagesForObjectives() {
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
                packages.add(new Package(x, y, targetPlanet, Package.PackageType.NORMAL));
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
            packages.add(new Package(x, y, targetPlanet, type));
        }
    }
    
    /**
     * Verifica que haya suficientes paquetes para cada objetivo y regenera si faltan.
     */
    private void checkAndRegeneratePackages() {
        for (LevelObjective objective : levelObjectives) {
            if (objective.isCompleted()) {
                continue; // No regenerar para objetivos completados
            }
            
            Planet targetPlanet = objective.getTargetPlanet();
            
            // Contar paquetes NORMALES disponibles (no entregados) para este planeta
            long availablePackages = packages.stream()
                .filter(pkg -> !pkg.isCollected() && 
                              pkg.getTargetPlanet() == targetPlanet && 
                              pkg.getType() == Package.PackageType.NORMAL)
                .count();
            
            // Calcular cuántos paquetes faltan
            int needed = objective.getRemaining();
            int toGenerate = (int)(needed - availablePackages);
            
            // Regenerar paquetes faltantes (solo NORMAL)
            for (int i = 0; i < toGenerate; i++) {
                // Generar cerca de la nave pero a distancia segura
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = MIN_SPAWN_DISTANCE + random.nextDouble() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
                double x = playerShip.getX() + Math.cos(angle) * distance;
                double y = playerShip.getY() + Math.sin(angle) * distance;
                
                // Solo paquetes NORMAL para objetivos
                packages.add(new Package(x, y, targetPlanet, Package.PackageType.NORMAL));
                addFloatingText("¡Nuevo paquete!", x, y, Color.CYAN);
            }
        }
    }
    
    /**
     * Avanza al siguiente nivel.
     */
    public void nextLevel() {
        // Calcular score del nivel
        int levelScore = calculateLevelScore();
        scoreManager.addScore(levelScore);
        addFloatingText("¡Nivel Completado! +" + levelScore, 
                       playerShip.getX(), playerShip.getY(), new Color(255, 215, 0));
        
        // Verificar si completó el último nivel
        if (currentLevel >= MAX_LEVELS) {
            gameWon = true;
            gameOver = true;
            return;
        }
        
        // Cargar siguiente nivel
        loadLevel(currentLevel + 1);
    }
    
    /**
     * Genera asteroides según el nivel actual.
     */
    private void generateAsteroidsForLevel(int level) {
        // Ya no se usa - reemplazado por LevelGenerator
    }
    
    /**
     * Genera asteroides periódicamente desde los bordes del mapa.
     */
    private void spawnAsteroidsOverTime() {
        long currentTime = System.currentTimeMillis();
        
        // Verificar si es tiempo de generar
        if (currentTime - lastAsteroidSpawn < ASTEROID_SPAWN_INTERVAL) {
            return;
        }
        
        lastAsteroidSpawn = currentTime;
        
        // Generar más asteroides según el nivel (aumenta con niveles)
        int count = 1 + currentLevel / 2;
        for (int i = 0; i < count; i++) {
            double x, y, velX, velY;
            
            // Velocidad base según nivel
            double baseSpeed = 1.5 + (currentLevel * 0.3);
            
            // 70% de probabilidad de aparecer desde bordes, 30% entre planetas
            if (random.nextDouble() < 0.7 || planets.isEmpty()) {
                // Desde los bordes (como antes)
                int edge = random.nextInt(4);
                switch (edge) {
                    case 0: // Arriba
                        x = random.nextDouble() * 2400;
                        y = -50;
                        velX = (random.nextDouble() - 0.5) * baseSpeed;
                        velY = baseSpeed;
                        break;
                    case 1: // Derecha
                        x = 2450;
                        y = random.nextDouble() * 1800;
                        velX = -baseSpeed;
                        velY = (random.nextDouble() - 0.5) * baseSpeed;
                        break;
                    case 2: // Abajo
                        x = random.nextDouble() * 2400;
                        y = 1850;
                        velX = (random.nextDouble() - 0.5) * baseSpeed;
                        velY = -baseSpeed;
                        break;
                    default: // Izquierda
                        x = -50;
                        y = random.nextDouble() * 1800;
                        velX = baseSpeed;
                        velY = (random.nextDouble() - 0.5) * baseSpeed;
                        break;
                }
            } else {
                // Entre planetas - crear trayectoria que cruce entre dos planetas aleatorios
                Planet planet1 = planets.get(random.nextInt(planets.size()));
                Planet planet2 = planets.get(random.nextInt(planets.size()));
                
                // Posición inicial cerca de un planeta
                double angleOffset = random.nextDouble() * 2 * Math.PI;
                double distance = planet1.getRadius() + 100 + random.nextDouble() * 100;
                x = planet1.getX() + Math.cos(angleOffset) * distance;
                y = planet1.getY() + Math.sin(angleOffset) * distance;
                
                // Velocidad apuntando hacia el otro planeta
                double dx = planet2.getX() - x;
                double dy = planet2.getY() - y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 0) {
                    velX = (dx / dist) * baseSpeed;
                    velY = (dy / dist) * baseSpeed;
                } else {
                    velX = (random.nextDouble() - 0.5) * baseSpeed * 2;
                    velY = (random.nextDouble() - 0.5) * baseSpeed * 2;
                }
            }
            
            // Tipo según nivel
            Asteroid.AsteroidType type = determineAsteroidType(currentLevel);
            
            // Verificar que no aparezca muy cerca de la nave
            double distanceToShip = Math.sqrt(
                Math.pow(x - playerShip.getX(), 2) +
                Math.pow(y - playerShip.getY(), 2)
            );
            
            // Solo añadir si está lo suficientemente lejos de la nave (mínimo 200px)
            if (distanceToShip > 200) {
                asteroids.add(new Asteroid(x, y, velX, velY, type));
            }
        }
    }
    
    /**
     * Determina el tipo de asteroide según el nivel.
     * Nivel 1-2: Solo pequeños y medianos
     * Nivel 3+: Todos los tipos incluyendo grandes
     */
    private Asteroid.AsteroidType determineAsteroidType(int level) {
        double rand = random.nextDouble();
        
        // Nivel 1-2: Solo SMALL y MEDIUM
        if (level <= 2) {
            if (rand < 0.6) {
                return Asteroid.AsteroidType.SMALL;
            } else {
                return Asteroid.AsteroidType.MEDIUM;
            }
        }
        
        // Nivel 3+: Todos los tipos
        // Nivel 7+: puede aparecer Chaser (10%)
        if (level >= 7 && rand < 0.1) {
            return Asteroid.AsteroidType.CHASER;
        }
        
        // Distribución normal con más grandes en niveles altos
        if (rand < 0.4) {
            return Asteroid.AsteroidType.SMALL;
        } else if (rand < 0.6) {
            return Asteroid.AsteroidType.MEDIUM;
        } else {
            return Asteroid.AsteroidType.LARGE;
        }
    }
    
    /**
     * Genera paquetes iniciales cerca de la nave.
     */
    private void generateInitialPackages(int count) {
        // Ya no se usa - reemplazado por LevelGenerator
    }
    
    /**
     * Genera un nuevo paquete cerca de la nave con tipo aleatorio.
     */
    private void generatePackageNearShip() {
        // Ya no se usa - los paquetes se generan solo al inicio del nivel
    }
    
    /**
     * Calcula el score del nivel completado.
     */
    private int calculateLevelScore() {
        int score = 0;
        
        // Base: 100 * nivel
        score += 100 * currentLevel;
        
        // Bonus por tiempo restante
        int timeRemaining = getRemainingTime();
        if (timeRemaining > 0) {
            score += timeRemaining * 10; // +10 por segundo
        }
        
        // Bonus por fuel (si la nave tiene fuel system)
        // score += (int)playerShip.getFuel(); // +1 por unidad
        
        // Penalización por daño recibido
        score -= damageReceivedThisLevel * 5; // -5 por punto de daño
        
        return Math.max(0, score);
    }
    
    /**
     * Determina el tipo de paquete basándose en probabilidades.
     */
    private Package.PackageType determinePackageType() {
        double roll = random.nextDouble();
        
        if (roll < SPECIAL_PACKAGE_CHANCE) {
            return Package.PackageType.SPECIAL; // 5%
        } else if (roll < SPECIAL_PACKAGE_CHANCE + URGENT_PACKAGE_CHANCE) {
            return Package.PackageType.URGENT; // 25%
        } else if (roll < SPECIAL_PACKAGE_CHANCE + URGENT_PACKAGE_CHANCE + HEAVY_PACKAGE_CHANCE) {
            return Package.PackageType.HEAVY; // 15%
        } else {
            return Package.PackageType.NORMAL; // 55%
        }
    }
    
    /**
     * Actualiza todas las entidades del juego.
     */
    public void update() {
        // Si game over, no actualizar
        if (gameOver) {
            return;
        }
        
        // Verificar condiciones de Game Over
        if (playerShip.getLives() <= 0 || getRemainingTime() <= 0) {
            triggerGameOver();
            return;
        }
        
        // Actualizar nave
        if (playerShip != null) {
            playerShip.update();
            
            // Calcular objetivo de cámara para centrar la nave
            targetCameraOffset.x = screenWidth / 2 - (int)playerShip.getX();
            targetCameraOffset.y = screenHeight / 2 - (int)playerShip.getY();
            
            // Interpolar suavemente hacia el objetivo (reduce mareo)
            cameraOffset.x = (int)(cameraOffset.x + (targetCameraOffset.x - cameraOffset.x) * CAMERA_SMOOTHNESS);
            cameraOffset.y = (int)(cameraOffset.y + (targetCameraOffset.y - cameraOffset.y) * CAMERA_SMOOTHNESS);
        }
        
        // Actualizar asteroides (perseguidores siguen a la nave)
        for (Asteroid asteroid : new ArrayList<>(asteroids)) {
            asteroid.update();
        }
        
        // Actualizar proyectiles
        projectiles.removeIf(p -> !p.isActive());
        for (Projectile projectile : new ArrayList<>(projectiles)) {
            projectile.update();
            projectile.checkBounds(screenWidth * 2, screenHeight * 2);
        }
        
        // Power-ups eliminados del juego
        
        // Verificar colisiones
        checkCollisions();
        
        // Actualizar sistema de combos
        scoreManager.update();
        
        // Actualizar textos flotantes
        floatingTexts.removeIf(FloatingText::isExpired);
        for (FloatingText text : floatingTexts) {
            text.update();
        }
        
        // Verificar recogida de paquetes
        if (!playerShip.hasPackage()) {
            checkPackagePickup();
        }
        
        // Verificar entrega de paquetes
        if (playerShip.hasPackage()) {
            checkPackageDelivery();
        }
        
        // Eliminar paquetes urgentes expirados
        packages.removeIf(pkg -> !pkg.isCollected() && pkg.isExpired());
        
        // Verificar y regenerar paquetes faltantes
        checkAndRegeneratePackages();
        
        // Generar asteroides periódicamente
        spawnAsteroidsOverTime();
    }
    
    /**
     * Activa el estado de Game Over.
     */
    private void triggerGameOver() {
        gameOver = true;
        addFloatingText("GAME OVER", playerShip.getX(), playerShip.getY(), Color.RED);
    }
    
    /**
     * Reinicia el juego desde el principio.
     */
    public void restartGame() {
        // Desactivar Game Over
        gameOver = false;
        gameWon = false;
        
        // Reiniciar nivel y tiempo
        currentLevel = 1;
        levelTime = 240;
        startTime = System.currentTimeMillis();
        pausedTime = 0;
        paused = false;
        damageReceivedThisLevel = 0;
        
        // Reiniciar score
        scoreManager = new ScoreManager();
        
        // Reiniciar score
        scoreManager = new ScoreManager();
        
        // Limpiar todas las entidades
        floatingTexts.clear();
        projectiles.clear();
        
        // Crear nueva nave en posición inicial
        playerShip = null; // Forzar creación de nueva nave
        
        // Cargar nivel 1
        loadLevel(1);
    }
    
    /**
     * Verifica si la nave puede recoger un paquete.
     */
    private void checkPackagePickup() {
        for (Package pkg : packages) {
            if (!pkg.isCollected() && !pkg.isPickedUp() && !pkg.isExpired()) {
                double distance = Math.sqrt(
                    Math.pow(playerShip.getX() - pkg.getX(), 2) +
                    Math.pow(playerShip.getY() - pkg.getY(), 2)
                );
                
                if (distance < PICKUP_RADIUS) {
                    playerShip.pickupPackage(pkg);
                    // Mostrar feedback visual
                    String message = pkg.getType() == Package.PackageType.HEAVY ? "¡PESADO!" : "¡Recogido!";
                    addFloatingText(message, pkg.getX(), pkg.getY(), Color.CYAN);
                    break;
                }
            }
        }
    }
    
    /**
     * Verifica si la nave puede entregar un paquete.
     */
    private void checkPackageDelivery() {
        Package carried = playerShip.getCarriedPackage();
        if (carried == null) return;
        
        Planet target = carried.getTargetPlanet();
        
        // Verificar distancia a TODOS los planetas
        for (Planet planet : planets) {
            double distance = Math.sqrt(
                Math.pow(playerShip.getX() - planet.getX(), 2) +
                Math.pow(playerShip.getY() - planet.getY(), 2)
            );
            
            if (distance < planet.getRadius() + 20) {
                // Está cerca de un planeta
                if (planet != target) {
                    // Planeta incorrecto - mostrar feedback solo una vez cada 2 segundos
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastIncorrectPlanetMessageTime >= 2000) {
                        addFloatingText("¡Planeta incorrecto!", planet.getX(), planet.getY() - 40, Color.RED);
                        addFloatingText("Destino: " + target.getName(), planet.getX(), planet.getY() - 20, Color.YELLOW);
                        lastIncorrectPlanetMessageTime = currentTime;
                    }
                    return; // NO entregar
                }
                
                // Planeta correcto - proceder con entrega
                carried.setCollected(true);
                playerShip.deliverPackage();
                
                // Calcular puntos según tipo
                int points = 10;
                switch (carried.getType()) {
                    case NORMAL: points = 10; break;
                    case URGENT: points = 30; break; // Bonus extra
                    case SPECIAL: points = 50; break;
                    case HEAVY: points = 20; break; // Bonus extra
                }
                scoreManager.addScore(points);
                addFloatingText("+" + points, target.getX(), target.getY(), Color.GREEN);
                
                // Añadir vida si es especial
                if (carried.grantsExtraLife()) {
                    playerShip.addLife();
                    addFloatingText("+1 VIDA!", target.getX(), target.getY() + 20, new Color(255, 215, 0));
                }
                
                // Actualizar combo
                scoreManager.addDelivery();
                
                // Solo paquetes NORMAL cuentan para objetivos
                if (carried.getType() == Package.PackageType.NORMAL) {
                    for (LevelObjective objective : levelObjectives) {
                        if (objective.getTargetPlanet() == target && !objective.isCompleted()) {
                            objective.incrementDelivered();
                            
                            // Verificar si completó el objetivo
                            if (objective.isCompleted()) {
                                addFloatingText("¡Objetivo " + target.getName() + " completado!", 
                                              target.getX(), target.getY() - 40, Color.CYAN);
                            }
                            
                            break;
                        }
                    }
                } else {
                    // Paquetes bonus (URGENTE, PESADO) dan mensaje especial
                    addFloatingText("¡BONUS!", target.getX(), target.getY() - 40, Color.YELLOW);
                }
                
                // Verificar si completó todos los objetivos (SIEMPRE después de cada entrega)
                boolean allCompleted = true;
                for (LevelObjective objective : levelObjectives) {
                    if (!objective.isCompleted()) {
                        allCompleted = false;
                        break;
                    }
                }
                
                if (allCompleted) {
                    addFloatingText("¡NIVEL COMPLETADO!", playerShip.getX(), playerShip.getY() - 30, Color.YELLOW);
                    // Dar 2 segundos antes de pasar al siguiente nivel
                    new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            nextLevel();
                        }
                    }, 2000);
                }
                
                return; // Salir después de intentar entrega
            }
        }
    }
    
    /**
     * Añade un texto flotante al mundo.
     */
    private void addFloatingText(String text, double x, double y, Color color) {
        floatingTexts.add(new FloatingText(text, x, y, 2000, color));
    }
    
    /**
     * Rompe el combo actual (llamar al chocar con asteroide).
     */
    public void breakCombo() {
        scoreManager.breakCombo();
    }
    
    /**
     * Dispara un proyectil desde la nave.
     */
    public void shootProjectile() {
        if (playerShip != null && playerShip.canShoot()) {
            Projectile projectile = playerShip.shoot();
            if (projectile != null) {
                projectiles.add(projectile);
            }
        }
    }
    
    /**
     * Verifica todas las colisiones del juego.
     */
    private void checkCollisions() {
        if (playerShip == null || !playerShip.isAlive()) {
            return;
        }
        
        // Colisión nave-asteroides
        for (Asteroid asteroid : asteroids) {
            if (asteroid.isDestroyed()) continue;
            
            double distance = Math.hypot(
                playerShip.getX() - asteroid.getX(),
                playerShip.getY() - asteroid.getY()
            );
            
            if (distance < 15 + asteroid.getRadius()) { // 15 = radio nave
                int damage = asteroid.getCollisionDamage();
                playerShip.takeHit(damage);
                registerDamage(damage);
                breakCombo();
                // Solo mostrar mensaje la primera vez (cuando no era inmune)
                if (!playerShip.isImmune()) {
                    addFloatingText("¡IMPACTO!", playerShip.getX(), playerShip.getY(), Color.RED);
                }
            }
        }
        
        // Colisión proyectiles-asteroides
        for (Projectile projectile : new ArrayList<>(projectiles)) {
            if (!projectile.isActive()) continue;
            
            for (Asteroid asteroid : new ArrayList<>(asteroids)) {
                if (asteroid.isDestroyed()) continue;
                
                double distance = Math.hypot(
                    projectile.getX() - asteroid.getX(),
                    projectile.getY() - asteroid.getY()
                );
                
                if (distance < projectile.getRadius() + asteroid.getRadius()) {
                    projectile.deactivate();
                    
                    // Dañar asteroide
                    boolean destroyed = asteroid.takeDamage(1);
                    
                    if (destroyed) {
                        // Agregar explosión con tamaño del asteroide
                        explosionPositions.add(new Point((int)asteroid.getX(), (int)asteroid.getY()));
                        explosionSizes.add(asteroid.getRadius());
                        
                        // Calcular puntos según tipo de asteroide
                        int points = 0;
                        switch (asteroid.getType()) {
                            case SMALL:
                                points = 10;
                                break;
                            case MEDIUM:
                                points = 25;
                                break;
                            case LARGE:
                                points = 50;
                                break;
                            case CHASER:
                                points = 30;
                                break;
                        }
                        
                        // Añadir puntos al marcador
                        scoreManager.addScore(points);
                        addFloatingText("+" + points, asteroid.getX(), asteroid.getY(), Color.YELLOW);
                        
                        // Dividir mediano en dos pequeños
                        if (asteroid.canSplit()) {
                            splitAsteroid(asteroid);
                        }
                    }
                    
                    break;
                }
            }
        }
        
        // Eliminar asteroides destruidos
        asteroids.removeIf(Asteroid::isDestroyed);
    }
    

    
    /**
     * Divide un asteroide mediano en dos pequeños.
     */
    private void splitAsteroid(Asteroid parent) {
        double x = parent.getX();
        double y = parent.getY();
        
        // Crear dos asteroides pequeños con velocidades opuestas
        asteroids.add(new Asteroid(x, y, 2, 1, Asteroid.AsteroidType.SMALL));
        asteroids.add(new Asteroid(x, y, -2, -1, Asteroid.AsteroidType.SMALL));
    }
    

    

    
    public Point getCameraOffset() {
        return cameraOffset;
    }
    
    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }
    
    /**
     * Obtiene el tiempo restante en segundos.
     */
    public int getRemainingTime() {
        if (paused) {
            return (int)((levelTime * 1000 - pausedTime) / 1000);
        }
        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = levelTime * 1000 - elapsed;
        return Math.max(0, (int)(remaining / 1000));
    }
    
    /**
     * Verifica si se acabó el tiempo.
     */
    public boolean isTimeUp() {
        return getRemainingTime() <= 0;
    }
    
    /**
     * Verifica si el tiempo es crítico (<30 segundos).
     */
    public boolean isCriticalTime() {
        return getRemainingTime() < 30 && getRemainingTime() > 0;
    }
    
    /**
     * Añade tiempo bonus (ya no se usa - tiempo solo al completar nivel).
     */
    public void addTimeBonus() {
        // Método obsoleto - tiempo solo se añade al completar nivel
    }
    
    /**
     * Pausa/reanuda el tiempo.
     */
    public void setPaused(boolean pause) {
        if (pause && !paused) {
            pausedTime = System.currentTimeMillis() - startTime;
            paused = true;
        } else if (!pause && paused) {
            startTime = System.currentTimeMillis() - pausedTime;
            paused = false;
        }
    }
    
    public Ship getPlayerShip() {
        return playerShip;
    }
    
    public List<Planet> getPlanets() {
        return planets;
    }
    
    public List<Asteroid> getAsteroids() {
        return asteroids;
    }
    
    public List<Package> getPackages() {
        return packages;
    }
    
    public List<FloatingText> getFloatingTexts() {
        return floatingTexts;
    }
    
    public ScoreManager getScoreManager() {
        return scoreManager;
    }
    
    /**
     * Obtiene el paquete más cercano no recogido.
     */
    public Package getClosestPackage() {
        Package closest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Package pkg : packages) {
            if (!pkg.isCollected() && !pkg.isPickedUp() && !pkg.isExpired()) {
                double distance = Math.sqrt(
                    Math.pow(playerShip.getX() - pkg.getX(), 2) +
                    Math.pow(playerShip.getY() - pkg.getY(), 2)
                );
                
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = pkg;
                }
            }
        }
        
        return closest;
    }
    
    public List<Projectile> getProjectiles() {
        return projectiles;
    }
    
    public List<Point> getAndClearExplosions() {
        List<Point> result = new ArrayList<>(explosionPositions);
        explosionPositions.clear();
        return result;
    }
    
    public List<Integer> getAndClearExplosionSizes() {
        List<Integer> result = new ArrayList<>(explosionSizes);
        explosionSizes.clear();
        return result;
    }
    
    public int getCurrentLevel() {
        return currentLevel;
    }
    
    public List<LevelObjective> getLevelObjectives() {
        return levelObjectives;
    }
    
    public boolean isGameOver() {
        return gameOver;
    }
    
    public boolean isGameWon() {
        return gameWon;
    }
    
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
