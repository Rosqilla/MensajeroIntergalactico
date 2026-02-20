package model.generators;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generador procedural de niveles con dificultad progresiva.
 * 
 * Este archivo contiene:
 * - WORLD GENERATOR: Generación de planetas con patrones (líneas 69-210)
 * - LIFE GENERATOR: Generación de asteroides en cinturones (líneas 215-280)
 */
public class LevelGenerator {
    private int currentLevel;
    private final Random random;
    
    // Configuración del mapa
    private static final int MAP_WIDTH = 2400;
    private static final int MAP_HEIGHT = 1800;
    private static final int CENTER_X = MAP_WIDTH / 2;
    private static final int CENTER_Y = MAP_HEIGHT / 2;
    
    // Nombres y colores de planetas
    private static final String[] PLANET_NAMES = {
        "Aridia", "Cryos", "Pyros", "Verdant", "Aquaris",
        "Nebulos", "Solara", "Glacius", "Terros", "Voltis"
    };
    
    private static final Color[] PLANET_COLORS = {
        new Color(200, 50, 50),   // Rojo (Aridia)
        new Color(100, 150, 255), // Azul (Cryos)
        new Color(255, 140, 0),   // Naranja (Pyros)
        new Color(50, 200, 80),   // Verde (Verdant)
        new Color(0, 200, 200),   // Cyan (Aquaris)
        new Color(150, 100, 200), // Púrpura (Nebulos)
        new Color(255, 215, 0),   // Dorado (Solara)
        new Color(180, 220, 255), // Azul claro (Glacius)
        new Color(160, 120, 80),  // Marrón (Terros)
        new Color(200, 200, 50)   // Amarillo (Voltis)
    };
    
    public LevelGenerator() {
        this.currentLevel = 1;
        this.random = new Random();
    }
    
    /**
     * Genera un nivel completo con todos sus elementos.
     */
    public GameData generateLevel(int levelNumber) {
        this.currentLevel = levelNumber;
        
        // Configuración según nivel
        int planetCount = getPlanetCount(levelNumber);
        int asteroidCount = getAsteroidCount(levelNumber);
        int packageCount = getPackageCount(levelNumber);
        
        // Generar elementos
        List<model.Planet> planets = generatePlanets(planetCount);
        List<model.Asteroid> asteroids = generateAsteroids(asteroidCount, levelNumber);
        List<model.Package> packages = generatePackages(packageCount, planets);
        Point playerStart = new Point(CENTER_X, CENTER_Y);
        
        return new GameData(planets, asteroids, packages, playerStart);
    }
    
    // ==================== WORLD GENERATOR ====================
    // Generación procedural de planetas con patrones diferentes
    // Los planetas siempre están en los mismos sitios porque el patrón
    // es determinista, pero los COLORES/NOMBRES cambian porque se asignan
    // secuencialmente desde arrays predefinidos (PLANET_NAMES, PLANET_COLORS)
    // ==========================================================
    
    /**
     * Genera planetas con patrones variados (WorldGenerator).
     * El patrón cambia cada 4 niveles: anillo, espiral, cruz, aleatorio.
     */
    private List<model.Planet> generatePlanets(int count) {
        List<model.Planet> planets = new ArrayList<>();
        
        // Elegir patrón según nivel (WorldGenerator)
        int pattern = currentLevel % 4;
        
        switch (pattern) {
            case 0: // Patrón de anillos concéntricos
                planets = generateRingPattern(count);
                break;
            case 1: // Patrón en espiral
                planets = generateSpiralPattern(count);
                break;
            case 2: // Patrón en cruz/estrella
                planets = generateCrossPattern(count);
                break;
            case 3: // Patrón aleatorio mejorado
                planets = generateScatteredPattern(count);
                break;
        }
        
        return planets;
    }
    
    /**
     * Patrón: Anillos concéntricos (clásico).
     */
    private List<model.Planet> generateRingPattern(int count) {
        List<model.Planet> planets = new ArrayList<>();
        double ringRadius = 300;
        
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count + random.nextDouble() * 0.3;
            double radius = ringRadius + (i % 2) * 200;
            
            double x = CENTER_X + Math.cos(angle) * radius;
            double y = CENTER_Y + Math.sin(angle) * radius;
            
            int styleIndex = i % PLANET_NAMES.length;
            String name = PLANET_NAMES[styleIndex];
            Color color = PLANET_COLORS[styleIndex];
            int planetRadius = 20 + random.nextInt(21);
            
            planets.add(new model.Planet(x, y, planetRadius, color, name));
        }
        
        return planets;
    }
    
    /**
     * Patrón: Espiral galáctica (WorldGenerator).
     */
    private List<model.Planet> generateSpiralPattern(int count) {
        List<model.Planet> planets = new ArrayList<>();
        double armLength = 500;
        
        for (int i = 0; i < count; i++) {
            double progress = (double) i / count;
            double angle = progress * Math.PI * 3; // 1.5 vueltas
            double radius = 150 + progress * armLength;
            
            double x = CENTER_X + Math.cos(angle) * radius;
            double y = CENTER_Y + Math.sin(angle) * radius;
            
            int styleIndex = i % PLANET_NAMES.length;
            String name = PLANET_NAMES[styleIndex];
            Color color = PLANET_COLORS[styleIndex];
            int planetRadius = 20 + random.nextInt(21);
            
            planets.add(new model.Planet(x, y, planetRadius, color, name));
        }
        
        return planets;
    }
    
    /**
     * Patrón: Cruz/estrella (WorldGenerator).
     */
    private List<model.Planet> generateCrossPattern(int count) {
        List<model.Planet> planets = new ArrayList<>();
        int arms = 4; // Cruz de 4 brazos
        int planetsPerArm = count / arms;
        
        for (int arm = 0; arm < arms; arm++) {
            double armAngle = (Math.PI * 2 * arm) / arms;
            
            for (int i = 0; i < planetsPerArm; i++) {
                double distance = 200 + (i * 150);
                double x = CENTER_X + Math.cos(armAngle) * distance;
                double y = CENTER_Y + Math.sin(armAngle) * distance;
                
                int planetIndex = arm * planetsPerArm + i;
                int styleIndex = planetIndex % PLANET_NAMES.length;
                String name = PLANET_NAMES[styleIndex];
                Color color = PLANET_COLORS[styleIndex];
                int planetRadius = 20 + random.nextInt(21);
                
                planets.add(new model.Planet(x, y, planetRadius, color, name));
            }
        }
        
        // Planetas restantes
        int remaining = count - (planetsPerArm * arms);
        for (int i = 0; i < remaining; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 250 + random.nextDouble() * 300;
            double x = CENTER_X + Math.cos(angle) * distance;
            double y = CENTER_Y + Math.sin(angle) * distance;
            
            int styleIndex = (planetsPerArm * arms + i) % PLANET_NAMES.length;
            String name = PLANET_NAMES[styleIndex];
            Color color = PLANET_COLORS[styleIndex];
            int planetRadius = 20 + random.nextInt(21);
            
            planets.add(new model.Planet(x, y, planetRadius, color, name));
        }
        
        return planets;
    }
    
    /**
     * Patrón: Distribución orgánica (WorldGenerator).
     */
    private List<model.Planet> generateScatteredPattern(int count) {
        List<model.Planet> planets = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 200 + random.nextDouble() * 400;
            double x = CENTER_X + Math.cos(angle) * distance;
            double y = CENTER_Y + Math.sin(angle) * distance;
            
            int styleIndex = i % PLANET_NAMES.length;
            String name = PLANET_NAMES[styleIndex];
            Color color = PLANET_COLORS[styleIndex];
            int planetRadius = 20 + random.nextInt(21);
            
            planets.add(new model.Planet(x, y, planetRadius, color, name));
        }
        
        return planets;
    }
    
    // ==================== LIFE GENERATOR ====================
    // Generación procedural de asteroides (vida del nivel)
    // Los asteroides se organizan en cinturones y respawnean
    // cuando son destruidos (ver GameModel.checkProjectileCollisions)
    // ==========================================================
    
    /**
     * Genera asteroides agrupados en cinturones (LifeGenerator).
     * Crea 2-3 cinturones con asteroides distribuidos aleatoriamente.
     */
    private List<model.Asteroid> generateAsteroids(int count, int level) {
        List<model.Asteroid> asteroids = new ArrayList<>();
        
        // Velocidad base incrementa con nivel
        double baseSpeed = 1.0 + (level * 0.2);
        
        // Crear 2-3 cinturones de asteroides
        int beltCount = 2 + (level > 5 ? 1 : 0);
        int asteroidsPerBelt = count / beltCount;
        
        for (int belt = 0; belt < beltCount; belt++) {
            // Centro del cinturón
            double beltCenterX = CENTER_X + (random.nextDouble() - 0.5) * MAP_WIDTH * 0.8;
            double beltCenterY = CENTER_Y + (random.nextDouble() - 0.5) * MAP_HEIGHT * 0.8;
            double beltSpread = 200 + belt * 100;
            
            for (int i = 0; i < asteroidsPerBelt; i++) {
                // Posición dentro del cinturón
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * beltSpread;
                double x = beltCenterX + Math.cos(angle) * distance;
                double y = beltCenterY + Math.sin(angle) * distance;
                
                // Mantener dentro del mapa
                x = Math.max(100, Math.min(MAP_WIDTH - 100, x));
                y = Math.max(100, Math.min(MAP_HEIGHT - 100, y));
                
                // Velocidad
                double velX = (random.nextDouble() - 0.5) * baseSpeed * 2;
                double velY = (random.nextDouble() - 0.5) * baseSpeed * 2;
                
                // Tipo según nivel y probabilidad
                model.Asteroid.AsteroidType type = determineAsteroidType(level);
                
                asteroids.add(new model.Asteroid(x, y, velX, velY, type));
            }
        }
        
        // Añadir asteroides restantes
        int remaining = count - (asteroidsPerBelt * beltCount);
        for (int i = 0; i < remaining; i++) {
            double x = 100 + random.nextDouble() * (MAP_WIDTH - 200);
            double y = 100 + random.nextDouble() * (MAP_HEIGHT - 200);
            double velX = (random.nextDouble() - 0.5) * baseSpeed * 2;
            double velY = (random.nextDouble() - 0.5) * baseSpeed * 2;
            
            model.Asteroid.AsteroidType type = determineAsteroidType(level);
            asteroids.add(new model.Asteroid(x, y, velX, velY, type));
        }
        
        return asteroids;
    }
    
    /**
     * Determina el tipo de asteroide según el nivel.
     */
    private model.Asteroid.AsteroidType determineAsteroidType(int level) {
        double roll = random.nextDouble();
        
        if (level >= 7 && roll < 0.1) {
            return model.Asteroid.AsteroidType.CHASER; // 10% perseguidores nivel 7+
        } else if (level >= 4 && roll < 0.3) {
            return model.Asteroid.AsteroidType.LARGE; // 30% grandes nivel 4+
        } else if (level >= 3 && roll < 0.5) {
            return model.Asteroid.AsteroidType.MEDIUM; // 20% medianos nivel 3+
        } else {
            return model.Asteroid.AsteroidType.SMALL; // 50-100% pequeños
        }
    }
    
    /**
     * Genera paquetes vinculados a planetas.
     */
    private List<model.Package> generatePackages(int count, List<model.Planet> planets) {
        List<model.Package> packages = new ArrayList<>();
        
        if (planets.isEmpty()) {
            return packages;
        }
        
        // Distribuir paquetes entre planetas
        for (int i = 0; i < count; i++) {
            // Planeta destino aleatorio
            model.Planet targetPlanet = planets.get(random.nextInt(planets.size()));
            
            // Posición: cerca del centro o de otro planeta
            double x, y;
            if (random.nextDouble() < 0.5) {
                // Cerca del centro (cerca del jugador)
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = 100 + random.nextDouble() * 200;
                x = CENTER_X + Math.cos(angle) * distance;
                y = CENTER_Y + Math.sin(angle) * distance;
            } else {
                // Cerca de un planeta aleatorio
                model.Planet nearPlanet = planets.get(random.nextInt(planets.size()));
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = 80 + random.nextDouble() * 100;
                x = nearPlanet.getX() + Math.cos(angle) * distance;
                y = nearPlanet.getY() + Math.sin(angle) * distance;
            }
            
            // Tipo de paquete
            model.Package.PackageType type = determinePackageType();
            
            packages.add(new model.Package(x, y, targetPlanet, type));
        }
        
        return packages;
    }
    
    /**
     * Determina el tipo de paquete con probabilidades.
     */
    private model.Package.PackageType determinePackageType() {
        double roll = random.nextDouble();
        
        if (roll < 0.05) {
            return model.Package.PackageType.SPECIAL; // 5%
        } else if (roll < 0.30) {
            return model.Package.PackageType.URGENT; // 25%
        } else if (roll < 0.45) {
            return model.Package.PackageType.HEAVY; // 15%
        } else {
            return model.Package.PackageType.NORMAL; // 55%
        }
    }
    
    // Configuración de cantidades por nivel
    private int getPlanetCount(int level) {
        if (level == 1) return 3;
        if (level == 2) return 4;
        if (level == 3) return 5;
        return Math.min(5 + (level - 3), 8); // Máximo 8 planetas
    }
    
    private int getAsteroidCount(int level) {
        if (level == 1) return 8;
        if (level == 2) return 12;
        if (level == 3) return 18;
        return 18 + (level - 3) * 5; // +5 por nivel
    }
    
    private int getPackageCount(int level) {
        if (level == 1) return 3;
        if (level == 2) return 5;
        if (level == 3) return 8;
        return 8 + (level - 3) * 2; // +2 por nivel
    }
    
    private int getFuelStationCount(int level) {
        if (level == 1) return 0; // Tutorial sin límite de fuel
        return 1 + (level / 2); // 1 estación cada 2 niveles
    }
}
