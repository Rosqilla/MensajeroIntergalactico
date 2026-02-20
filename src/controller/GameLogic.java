package controller;

import model.Asteroid;
import model.FloatingText;
import model.GameModel;
import model.LevelObjective;
import model.Planet;
import model.Projectile;
import model.ScoreManager;
import model.Ship;
import model.Package.PackageType;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lógica de juego extraída del modelo (patrón MVC).
 * El controlador orquesta estas operaciones; el modelo solo almacena estado.
 * 
 * Todas las referencias a System.currentTimeMillis() se sustituyen por
 * el parámetro {@code currentTime} inyectado desde GameController.gameLoop().
 */
public class GameLogic {

    private static final double MIN_SPAWN_DISTANCE = 200;
    private static final double MAX_SPAWN_DISTANCE = 400;
    private static final int PICKUP_RADIUS = 40;
    private static final long ASTEROID_SPAWN_INTERVAL = 1500;

    private final Random random = new Random();

    // ========== Recogida de paquetes ==========

    /**
     * Verifica si la nave puede recoger un paquete cercano.
     */
    public void checkPackagePickup(GameModel model, long currentTime) {
        Ship ship = model.getPlayerShip();
        for (model.Package pkg : model.getPackages()) {
            if (!pkg.isCollected() && !pkg.isPickedUp() && !pkg.isExpired(currentTime)) {
                double distance = Math.hypot(
                    ship.getX() - pkg.getX(),
                    ship.getY() - pkg.getY()
                );
                if (distance < PICKUP_RADIUS) {
                    ship.pickupPackage(pkg);
                    String message = pkg.getType() == PackageType.HEAVY ? "¡PESADO!" : "¡Recogido!";
                    model.addFloatingText(message, pkg.getX(), pkg.getY(), Color.CYAN, currentTime);
                    break;
                }
            }
        }
    }

    // ========== Entrega de paquetes ==========

    /**
     * Verifica si la nave puede entregar un paquete en un planeta.
     * @return {@code true} si se completaron TODOS los objetivos del nivel.
     */
    public boolean checkPackageDelivery(GameModel model, long currentTime) {
        Ship ship = model.getPlayerShip();
        model.Package carried = ship.getCarriedPackage();
        if (carried == null) return false;

        Planet target = carried.getTargetPlanet();

        for (Planet planet : model.getPlanets()) {
            double distance = Math.hypot(
                ship.getX() - planet.getX(),
                ship.getY() - planet.getY()
            );

            if (distance < planet.getRadius() + 20) {
                // Planeta incorrecto
                if (planet != target) {
                    if (currentTime - model.getLastIncorrectPlanetMessageTime() >= 2000) {
                        model.addFloatingText("¡Planeta incorrecto!", planet.getX(), planet.getY() - 40, Color.RED, currentTime);
                        model.addFloatingText("Destino: " + target.getName(), planet.getX(), planet.getY() - 20, Color.YELLOW, currentTime);
                        model.setLastIncorrectPlanetMessageTime(currentTime);
                    }
                    return false;
                }

                // Planeta correcto — proceder con entrega
                carried.setCollected(true);
                ship.deliverPackage();

                int points = switch (carried.getType()) {
                    case NORMAL -> 10;
                    case URGENT -> 30;
                    case SPECIAL -> 50;
                    case HEAVY -> 20;
                };
                model.getScoreManager().addScore(points);
                model.addFloatingText("+" + points, target.getX(), target.getY(), Color.GREEN, currentTime);

                if (carried.grantsExtraLife()) {
                    ship.addLife();
                    model.addFloatingText("+1 VIDA!", target.getX(), target.getY() + 20, new Color(255, 215, 0), currentTime);
                }

                model.getScoreManager().addDelivery(currentTime);

                // Solo NORMAL cuenta para objetivos
                if (carried.getType() == PackageType.NORMAL) {
                    for (LevelObjective objective : model.getLevelObjectives()) {
                        if (objective.getTargetPlanet() == target && !objective.isCompleted()) {
                            objective.incrementDelivered();
                            if (objective.isCompleted()) {
                                model.addFloatingText("¡Objetivo " + target.getName() + " completado!",
                                    target.getX(), target.getY() - 40, Color.CYAN, currentTime);
                            }
                            break;
                        }
                    }
                } else {
                    model.addFloatingText("¡BONUS!", target.getX(), target.getY() - 40, Color.YELLOW, currentTime);
                }

                // Verificar si TODOS los objetivos se completaron
                boolean allCompleted = model.getLevelObjectives().stream()
                    .allMatch(LevelObjective::isCompleted);

                if (allCompleted) {
                    model.addFloatingText("¡NIVEL COMPLETADO!", ship.getX(), ship.getY() - 30, Color.YELLOW, currentTime);
                }
                return allCompleted;
            }
        }
        return false;
    }

    // ========== Regeneración de paquetes ==========

    /**
     * Regenera paquetes faltantes para objetivos no completados.
     */
    public void checkAndRegeneratePackages(GameModel model, long currentTime) {
        Ship ship = model.getPlayerShip();
        for (LevelObjective objective : model.getLevelObjectives()) {
            if (objective.isCompleted()) continue;

            Planet targetPlanet = objective.getTargetPlanet();

            long availablePackages = model.getPackages().stream()
                .filter(pkg -> !pkg.isCollected()
                    && pkg.getTargetPlanet() == targetPlanet
                    && pkg.getType() == PackageType.NORMAL)
                .count();

            int toGenerate = (int)(objective.getRemaining() - availablePackages);

            for (int i = 0; i < toGenerate; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double dist = MIN_SPAWN_DISTANCE + random.nextDouble() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
                double x = ship.getX() + Math.cos(angle) * dist;
                double y = ship.getY() + Math.sin(angle) * dist;

                model.getPackages().add(new model.Package(x, y, targetPlanet, PackageType.NORMAL, currentTime));
                model.addFloatingText("¡Nuevo paquete!", x, y, Color.CYAN, currentTime);
            }
        }
    }

    // ========== Generación de asteroides ==========

    /**
     * Genera asteroides periódicamente desde los bordes del mapa o entre planetas.
     */
    public void spawnAsteroidsOverTime(GameModel model, long currentTime) {
        if (currentTime - model.getLastAsteroidSpawn() < ASTEROID_SPAWN_INTERVAL) {
            return;
        }
        model.setLastAsteroidSpawn(currentTime);

        Ship ship = model.getPlayerShip();
        int level = model.getCurrentLevel();
        int count = 1 + level / 2;

        for (int i = 0; i < count; i++) {
            double x, y, velX, velY;
            double baseSpeed = 1.5 + (level * 0.3);

            if (random.nextDouble() < 0.7 || model.getPlanets().isEmpty()) {
                // Desde los bordes
                int edge = random.nextInt(4);
                switch (edge) {
                    case 0 -> { x = random.nextDouble() * 2400; y = -50;
                                velX = (random.nextDouble() - 0.5) * baseSpeed; velY = baseSpeed; }
                    case 1 -> { x = 2450; y = random.nextDouble() * 1800;
                                velX = -baseSpeed; velY = (random.nextDouble() - 0.5) * baseSpeed; }
                    case 2 -> { x = random.nextDouble() * 2400; y = 1850;
                                velX = (random.nextDouble() - 0.5) * baseSpeed; velY = -baseSpeed; }
                    default -> { x = -50; y = random.nextDouble() * 1800;
                                 velX = baseSpeed; velY = (random.nextDouble() - 0.5) * baseSpeed; }
                }
            } else {
                // Entre planetas
                Planet p1 = model.getPlanets().get(random.nextInt(model.getPlanets().size()));
                Planet p2 = model.getPlanets().get(random.nextInt(model.getPlanets().size()));
                double angleOff = random.nextDouble() * 2 * Math.PI;
                double dist = p1.getRadius() + 100 + random.nextDouble() * 100;
                x = p1.getX() + Math.cos(angleOff) * dist;
                y = p1.getY() + Math.sin(angleOff) * dist;

                double dx = p2.getX() - x;
                double dy = p2.getY() - y;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d > 0) { velX = (dx / d) * baseSpeed; velY = (dy / d) * baseSpeed; }
                else        { velX = (random.nextDouble() - 0.5) * baseSpeed * 2;
                              velY = (random.nextDouble() - 0.5) * baseSpeed * 2; }
            }

            Asteroid.AsteroidType type = determineAsteroidType(level);

            double distToShip = Math.hypot(x - ship.getX(), y - ship.getY());
            if (distToShip > 200) {
                model.getAsteroids().add(new Asteroid(x, y, velX, velY, type));
            }
        }
    }

    // ========== Disparo ==========

    /**
     * Dispara un proyectil desde la nave si el cooldown lo permite.
     */
    public void shootProjectile(GameModel model, long currentTime) {
        Ship ship = model.getPlayerShip();
        if (ship != null && ship.canShoot(currentTime)) {
            Projectile proj = ship.shoot(currentTime);
            if (proj != null) {
                model.getProjectiles().add(proj);
            }
        }
    }

    // ========== Colisiones proyectil-asteroide ==========

    /**
     * Detecta colisiones entre proyectiles y asteroides.
     */
    public void checkProjectileCollision(GameModel model, long currentTime) {
        List<Asteroid> asteroids = model.getAsteroids();
        List<Projectile> projectiles = model.getProjectiles();
        ScoreManager scores = model.getScoreManager();

        for (Projectile projectile : new ArrayList<>(projectiles)) {
            if (!projectile.isActive()) continue;

            for (Asteroid asteroid : new ArrayList<>(asteroids)) {
                if (asteroid.isDestroyed()) continue;

                double distance = Math.hypot(
                    projectile.getX() - asteroid.getX(),
                    projectile.getY() - asteroid.getY()
                );

                if (distance < projectile.getRadius() + asteroid.getRadius()) {
                    projectile.setActive(false);
                    double hp = asteroid.getHealth();
                    asteroid.setHealth(hp - 1);
                    asteroid.setLastHitTime(currentTime);

                    if (asteroid.getHealth() <= 0) {
                        asteroid.setDestroyed(true);
                        model.addExplosion(new Point((int) asteroid.getX(), (int) asteroid.getY()),
                                           asteroid.getRadius());

                        int points = switch (asteroid.getType()) {
                            case SMALL  -> 10;
                            case MEDIUM -> 25;
                            case LARGE  -> 50;
                            case CHASER -> 30;
                        };
                        scores.addScore(points);
                        model.addFloatingText("+" + points, asteroid.getX(), asteroid.getY(), Color.YELLOW, currentTime);

                        if (asteroid.canSplit()) {
                            splitAsteroid(model, asteroid);
                        }
                    }
                    break;
                }
            }
        }
        asteroids.removeIf(Asteroid::isDestroyed);
    }

    // ========== Game Over ==========

    /**
     * Activa el estado de Game Over.
     */
    public void triggerGameOver(GameModel model, long currentTime) {
        model.setGameOver(true);
        Ship ship = model.getPlayerShip();
        model.addFloatingText("GAME OVER", ship.getX(), ship.getY(), Color.RED, currentTime);
    }

    // ========== Avance de nivel ==========

    /**
     * Avanza al siguiente nivel, calcula puntuación del nivel completado.
     */
    public void nextLevel(GameModel model, long currentTime) {
        int levelScore = calculateLevelScore(model, currentTime);
        model.getScoreManager().addScore(levelScore);
        Ship ship = model.getPlayerShip();
        model.addFloatingText("¡Nivel Completado! +" + levelScore,
            ship.getX(), ship.getY(), new Color(255, 215, 0), currentTime);

        if (model.getCurrentLevel() >= 5) { // MAX_LEVELS
            model.setGameWon(true);
            model.setGameOver(true);
            return;
        }

        model.loadLevel(model.getCurrentLevel() + 1, currentTime);
    }

    // ========== Reinicio ==========

    /**
     * Reinicia el juego desde el nivel 1.
     */
    public void restartGame(GameModel model, long currentTime) {
        model.setGameOver(false);
        model.setGameWon(false);
        model.setCurrentLevel(1);
        model.setLevelTime(240);
        model.setStartTime(currentTime);
        model.setPausedTime(0);
        model.setPausedFlag(false);
        model.setDamageReceivedThisLevel(0);
        model.resetScoreManager();
        model.getFloatingTexts().clear();
        model.getProjectiles().clear();
        model.setPlayerShip(null); // Forzar creación de nueva nave
        model.loadLevel(1, currentTime);
    }

    // ========== Helpers privados ==========

    private int calculateLevelScore(GameModel model, long currentTime) {
        int score = 100 * model.getCurrentLevel();
        int timeRemaining = model.getRemainingTime(currentTime);
        if (timeRemaining > 0) {
            score += timeRemaining * 10;
        }
        score -= model.getDamageReceivedThisLevel() * 5;
        return Math.max(0, score);
    }

    private Asteroid.AsteroidType determineAsteroidType(int level) {
        double rand = random.nextDouble();
        if (level <= 2) {
            return rand < 0.6 ? Asteroid.AsteroidType.SMALL : Asteroid.AsteroidType.MEDIUM;
        }
        if (level >= 7 && rand < 0.1) return Asteroid.AsteroidType.CHASER;
        if (rand < 0.4) return Asteroid.AsteroidType.SMALL;
        if (rand < 0.6) return Asteroid.AsteroidType.MEDIUM;
        return Asteroid.AsteroidType.LARGE;
    }

    private void splitAsteroid(GameModel model, Asteroid parent) {
        double x = parent.getX();
        double y = parent.getY();
        model.getAsteroids().add(new Asteroid(x, y, 2, 1, Asteroid.AsteroidType.SMALL));
        model.getAsteroids().add(new Asteroid(x, y, -2, -1, Asteroid.AsteroidType.SMALL));
    }
}
