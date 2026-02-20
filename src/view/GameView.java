package view;

import model.*;
import model.Package.PackageType;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Vista principal del juego (patrón MVC).
 * Responsable de renderizar todos los elementos visuales del juego:
 * - Fondo estrellado con efecto parallax
 * - Sprites de nave, asteroides, proyectiles y explosiones
 * - HUD con información del jugador
 * - Pantallas de game over y victoria
 * 
 * Esta clase NO contiene lógica de juego, solo renderizado.
 */
public class GameView extends JPanel {
    private IGameModel model;
    private List<Star> stars;
    private Random random;
    
    // Sprites
    private BufferedImage shipSprite;
    private BufferedImage[] asteroidBig;
    private BufferedImage[] asteroidMed;
    private BufferedImage[] asteroidSmall;
    private BufferedImage[] projectileFrames;
    private BufferedImage explosionSheet;
    private BufferedImage backgroundImage;
    private BufferedImage[] fireTrailFrames;
    
    // Animación
    private int trailFrame = 0;
    private long lastFrameTime = 0;
    private static final long FRAME_DELAY = 50;
    
    // Explosiones activas
    private List<ExplosionAnimation> explosions = new ArrayList<>();
    
    /**
     * Representa una estrella del fondo con efecto parallax.
     */
    private static class Star {
        double x, y;
        int size;
        int layer;
        
        Star(double x, double y, int size, int layer) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.layer = layer;
        }
    }
    
    /**
     * Representa una animación de explosión temporal.
     * La explosión se renderiza extrayendo frames de un sprite sheet horizontal (tira 1×11).
     * Archivo: Explosion Animation.png (704×64px, 11 frames de 64×64px).
     */
    private static class ExplosionAnimation {
        double x, y;
        long startTime;
        double scale; // Escala según tamaño del asteroide
        static final int TOTAL_FRAMES = 11; // 11 frames en tira horizontal
        static final long FRAME_DURATION = 60; // ms por frame
        
        ExplosionAnimation(double x, double y, double scale) {
            this.x = x;
            this.y = y;
            this.startTime = System.currentTimeMillis();
            this.scale = scale;
        }
        
        boolean isFinished() {
            return getCurrentFrame() >= TOTAL_FRAMES;
        }
        
        // [MVC] Método pasivo - calcula sin cambiar estado
        int getCurrentFrame() {
            long elapsed = System.currentTimeMillis() - startTime;
            return (int)(elapsed / FRAME_DURATION);
        }
    }
    
    /**
     * Constructor de la vista.
     * Inicializa los sprites, el fondo estrellado y configura el panel.
     * 
     * @param model El modelo del juego a visualizar
     */
    public GameView(IGameModel model) {
        this.model = model;
        this.random = new Random();
        setPreferredSize(new Dimension(1200, 800));
        setBackground(new Color(5, 5, 15));
        setDoubleBuffered(true);
        loadSprites();
        initStars();
    }
    
    private void loadSprites() {
        try {
            // Nave
            shipSprite = ImageIO.read(new File("resources/sprites/ship/ship_red.png"));
            
            // Asteroides grandes (4 variantes)
            asteroidBig = new BufferedImage[4];
            for (int i = 0; i < 4; i++) {
                asteroidBig[i] = ImageIO.read(new File("resources/sprites/asteroid/meteorGrey_big" + (i + 1) + ".png"));
            }
            
            // Asteroides medianos (2 variantes)
            asteroidMed = new BufferedImage[2];
            for (int i = 0; i < 2; i++) {
                asteroidMed[i] = ImageIO.read(new File("resources/sprites/asteroid/meteorGrey_med" + (i + 1) + ".png"));
            }
            
            // Asteroides pequeños (2 variantes)
            asteroidSmall = new BufferedImage[2];
            for (int i = 0; i < 2; i++) {
                asteroidSmall[i] = ImageIO.read(new File("resources/sprites/asteroid/meteorGrey_small" + (i + 1) + ".png"));
            }
            
            // Proyectil (solo laserBlue14)
            projectileFrames = new BufferedImage[1];
            projectileFrames[0] = ImageIO.read(new File("resources/sprites/projectile/laserBlue14.png"));
            
            // Explosión (sprite sheet horizontal 1×11: 704×64px, frames de 64×64px)
            explosionSheet = ImageIO.read(new File("resources/sprites/explosion/Explosion Animation.png"));
            
            // Fondo
            backgroundImage = ImageIO.read(new File("resources/sprites/background/Space01.png"));
            
            // Trail de fuego (sprite sheet horizontal 1×8: 256×48px, frames de 32×48px)
            BufferedImage trailSheet = ImageIO.read(new File("resources/sprites/trail/Group 4 - 4.png"));
            if (trailSheet != null) {
                fireTrailFrames = new BufferedImage[8];
                int frameWidth = trailSheet.getWidth() / 8;  // 256 ÷ 8 = 32px por frame
                for (int i = 0; i < 8; i++) {
                    // Extrae cada frame de la tira horizontal
                    fireTrailFrames[i] = trailSheet.getSubimage(i * frameWidth, 0, frameWidth, trailSheet.getHeight());
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error cargando sprites: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initStars() {
        stars = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            stars.add(new Star(
                random.nextDouble() * 3000 - 1500,
                random.nextDouble() * 3000 - 1500,
                random.nextInt(2) + 1,
                random.nextInt(2)
            ));
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        Point cameraOffset = model.getCameraOffset();
        int camX = cameraOffset.x;
        int camY = cameraOffset.y;
        
        // Draw background image with tiling
        if (backgroundImage != null) {
            int bgW = backgroundImage.getWidth();
            int bgH = backgroundImage.getHeight();
            
            // Parallax effect (fondo se mueve más lento)
            int bgOffsetX = (int)(camX * 0.5);
            int bgOffsetY = (int)(camY * 0.5);
            
            // Calcular cuántos tiles necesitamos
            int startTileX = (bgOffsetX / bgW) - 1;
            int startTileY = (bgOffsetY / bgH) - 1;
            int endTileX = ((bgOffsetX + getWidth()) / bgW) + 1;
            int endTileY = ((bgOffsetY + getHeight()) / bgH) + 1;
            
            // Dibujar tiles
            for (int tileY = startTileY; tileY <= endTileY; tileY++) {
                for (int tileX = startTileX; tileX <= endTileX; tileX++) {
                    int x = tileX * bgW - bgOffsetX;
                    int y = tileY * bgH - bgOffsetY;
                    g2d.drawImage(backgroundImage, x, y, null);
                }
            }
        }
        
        // Draw starfield with parallax
        drawStarfield(g2d, camX, camY);
        
        // Draw planets
        drawPlanets(g2d, camX, camY);
        
        // Draw asteroids
        drawAsteroids(g2d, camX, camY);
        
        // Draw packages
        drawPackages(g2d, camX, camY);
        
        // Draw remote ships (other peers)
        drawRemoteShips(g2d, camX, camY);
        
        // Draw player ship (local)
        drawShip(g2d, camX, camY);
        
        // Draw projectiles
        drawProjectiles(g2d, camX, camY);
        
        // Draw explosions
        drawExplosions(g2d, camX, camY);
        
        // Draw floating texts
        drawFloatingTexts(g2d, camX, camY);
        
        // Draw HUD (no camera transformation)
        drawHUD(g2d);
        
        // Draw Game Over / Victory screen
        if (model.isGameOver()) {
            drawGameOverScreen(g2d);
        }
    }
    
    private void drawStarfield(Graphics2D g2d, int camX, int camY) {
        g2d.setColor(Color.WHITE);
        for (Star star : stars) {
            double parallax = star.layer == 0 ? 0.3 : 0.6;
            int x = (int)(star.x + camX * parallax);
            int y = (int)(star.y + camY * parallax);
            
            if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) {
                g2d.fillOval(x, y, star.size, star.size);
            }
        }
    }
    
    private void drawPlanets(Graphics2D g2d, int camX, int camY) {
        for (Planet planet : model.getPlanets()) {
            int x = (int)planet.getX() + camX;
            int y = (int)planet.getY() + camY;
            int radius = planet.getRadius();
            
            // Planet glow
            for (int i = 3; i > 0; i--) {
                Color glowColor = new Color(
                    planet.getColor().getRed(),
                    planet.getColor().getGreen(),
                    planet.getColor().getBlue(),
                    30 / i
                );
                g2d.setColor(glowColor);
                g2d.fillOval(x - radius - i * 5, y - radius - i * 5, 
                           (radius + i * 5) * 2, (radius + i * 5) * 2);
            }
            
            // Planet body
            g2d.setColor(planet.getColor());
            g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            
            // Planet border
            g2d.setColor(planet.getColor().brighter());
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            
            // Planet name
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            String name = planet.getName();
            g2d.drawString(name, x - fm.stringWidth(name) / 2, y + radius + 20);
        }
    }
    
    private void drawAsteroids(Graphics2D g2d, int camX, int camY) {
        long currentTime = System.currentTimeMillis();
        for (Asteroid asteroid : model.getAsteroids()) {
            if (asteroid.isDestroyed()) continue;
            
            int x = (int)asteroid.getX() + camX;
            int y = (int)asteroid.getY() + camY;
            
            // Seleccionar sprite según tipo
            BufferedImage sprite = null;
            switch (asteroid.getType()) {
                case SMALL:
                    if (asteroidSmall != null && asteroidSmall.length > 0) {
                        // Usar hash del asteroid para elegir variante consistentemente
                        int variant = Math.abs(asteroid.hashCode()) % asteroidSmall.length;
                        sprite = asteroidSmall[variant];
                    }
                    break;
                case MEDIUM:
                    if (asteroidMed != null && asteroidMed.length > 0) {
                        int variant = Math.abs(asteroid.hashCode()) % asteroidMed.length;
                        sprite = asteroidMed[variant];
                    }
                    break;
                case LARGE:
                case CHASER:
                    if (asteroidBig != null && asteroidBig.length > 0) {
                        int variant = Math.abs(asteroid.hashCode()) % asteroidBig.length;
                        sprite = asteroidBig[variant];
                    }
                    break;
            }
            
            // Dibujar sprite con rotación
            if (sprite != null) {
                AffineTransform oldTransform = g2d.getTransform();
                g2d.translate(x, y);
                
                // Rotación lenta basada en hash para variedad
                double rotation = (System.currentTimeMillis() / 5000.0 + asteroid.hashCode()) % (2 * Math.PI);
                g2d.rotate(rotation);
                
                // Tinte rojo para CHASERs
                if (asteroid.getType() == Asteroid.AsteroidType.CHASER) {
                    Composite oldComp = g2d.getComposite();
                    AlphaComposite alphaComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f);
                    g2d.setComposite(alphaComp);
                    
                    int w = sprite.getWidth();
                    int h = sprite.getHeight();
                    g2d.drawImage(sprite, -w/2, -h/2, null);
                    
                    // Overlay rojo
                    g2d.setColor(new Color(255, 100, 100, 100));
                    g2d.fillOval(-w/2, -h/2, w, h);
                    
                    g2d.setComposite(oldComp);
                } else {
                    int w = sprite.getWidth();
                    int h = sprite.getHeight();
                    g2d.drawImage(sprite, -w/2, -h/2, null);
                }
                
                g2d.setTransform(oldTransform);
            }
            
            // Health indicator for damaged asteroids (solo si fue golpeado recientemente)
            if (asteroid.getHealth() < 100 && asteroid.wasRecentlyHit(currentTime)) {
                int radius = asteroid.getRadius();
                int healthBarWidth = radius * 2;
                int healthBarHeight = 8;
                int barX = x - radius;
                int barY = y - radius - 15;
                
                // Fondo negro
                g2d.setColor(new Color(0, 0, 0, 200));
                g2d.fillRect(barX, barY, healthBarWidth, healthBarHeight);
                
                // Borde
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRect(barX, barY, healthBarWidth, healthBarHeight);
                
                // Barra de vida (roja a verde según salud)
                double healthPercent = asteroid.getHealth() / 100.0;
                Color healthColor = healthPercent > 0.5 ? Color.GREEN : 
                                   healthPercent > 0.25 ? Color.YELLOW : Color.RED;
                g2d.setColor(healthColor);
                g2d.fillRect(barX + 1, barY + 1, 
                           (int)((healthBarWidth - 2) * healthPercent), healthBarHeight - 2);
                
                // Mostrar HP actual (vida en números) - GRANDE Y VISIBLE
                String hpText = String.format("%.0f", asteroid.getHealth());
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(hpText);
                
                // Fondo negro para el número
                g2d.setColor(new Color(0, 0, 0, 220));
                g2d.fillRoundRect(barX + (healthBarWidth - textWidth) / 2 - 3, barY - 2, textWidth + 6, 16, 5, 5);
                
                // Número blanco brillante
                g2d.setColor(Color.WHITE);
                g2d.drawString(hpText, barX + (healthBarWidth - textWidth) / 2, barY + 11);
            }
        }
    }
    
    private void drawPackages(Graphics2D g2d, int camX, int camY) {
        for (model.Package pkg : model.getPackages()) {
            // No dibujar paquetes que ya han sido recogidos
            if (pkg.isPickedUp()) continue;
            
            int x = (int)pkg.getX() + camX;
            int y = (int)pkg.getY() + camY;
            int size = 20;
            
            Color pkgColor;
            
            // Paquetes URGENT y HEAVY tienen colores especiales (nunca del planeta)
            if (pkg.getType() == PackageType.URGENT) {
                // Rojo brillante para urgente
                pkgColor = new Color(255, 50, 50);
            } else if (pkg.getType() == PackageType.HEAVY) {
                // Marrón/naranja para pesado
                pkgColor = new Color(200, 120, 50);
            } else if (pkg.getTargetPlanet() != null) {
                // Paquetes NORMAL usan el color del planeta destino
                pkgColor = pkg.getTargetPlanet().getColor();
            } else {
                // Fallback
                pkgColor = new Color(50, 200, 50);
            }
            
            // Package glow
            g2d.setColor(new Color(pkgColor.getRed(), pkgColor.getGreen(), pkgColor.getBlue(), 80));
            g2d.fillRect(x - size / 2 - 3, y - size / 2 - 3, size + 6, size + 6);
            
            // Package body
            g2d.setColor(pkgColor);
            g2d.fillRect(x - size / 2, y - size / 2, size, size);
            
            // Package border
            g2d.setColor(pkgColor.brighter());
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(x - size / 2, y - size / 2, size, size);
            
            // Timer indicator for urgent packages
            long currentTime = System.currentTimeMillis();
            if (pkg.getType() == PackageType.URGENT && pkg.getRemainingTime(currentTime) < 10) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.drawString(String.valueOf((int)pkg.getRemainingTime(currentTime)), x - 5, y - size / 2 - 5);
            }
        }
    }
    
    /**
     * Dibuja la nave del jugador local.
     */
    private void drawShip(Graphics2D g2d, int camX, int camY) {
        Ship ship = model.getPlayerShip();
        drawShipEntity(g2d, ship, camX, camY);
    }
    
    /**
     * Dibuja todas las naves remotas (de otros peers).
     */
    private void drawRemoteShips(Graphics2D g2d, int camX, int camY) {
        for (Ship remoteShip : model.getRemoteShips()) {
            drawShipEntity(g2d, remoteShip, camX, camY);
        }
    }
    
    /**
     * Dibuja una nave (local o remota) con diferenciación visual.
     */
    private void drawShipEntity(Graphics2D g2d, Ship ship, int camX, int camY) {
        if (!ship.isAlive()) {
            return;
        }
        
        int x = (int)ship.getX() + camX;
        int y = (int)ship.getY() + camY;
        
        // Draw trail particles
        drawShipTrail(g2d, ship, camX, camY);
        
        // Draw ship sprite
        if (shipSprite != null) {
            AffineTransform oldTransform = g2d.getTransform();
            g2d.translate(x, y);
            // Rotar la nave: el sprite apunta hacia arriba, compensamos +90 grados para que vaya bien
            g2d.rotate(ship.getRotationAngle() + Math.PI / 2);
            
            // Apply immunity tint
            if (ship.isImmune()) {
                AlphaComposite alphaComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f);
                g2d.setComposite(alphaComp);
            }
            
            // Escalar la nave al 60% del tamaño original
            int w = (int)(shipSprite.getWidth() * 0.6);
            int h = (int)(shipSprite.getHeight() * 0.6);
            g2d.drawImage(shipSprite, -w/2, -h/2, w, h, null);
            
            // Apply color tint for remote ships (Overlay semitransparente)
            if (ship.isRemote()) {
                Color tintColor = ship.getTintColor();
                g2d.setColor(new Color(tintColor.getRed(), tintColor.getGreen(), tintColor.getBlue(), 120));
                g2d.fillRect(-w/2, -h/2, w, h);
                
                // Borde adicional para mayor visibilidad
                g2d.setColor(tintColor);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(-w/2-2, -h/2-2, w+4, h+4);
            }
            
            // Reset composite
            if (ship.isImmune()) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }
            
            // Package indicator
            if (ship.getCarriedPackage() != null) {
                g2d.setColor(Color.YELLOW);
                g2d.fillOval(-5, 0, 10, 10);
            }
            
            g2d.setTransform(oldTransform);
        }
        
        // Immunity shield
        if (ship.isImmune()) {
            g2d.setColor(new Color(255, 255, 0, 80));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(x - 25, y - 25, 50, 50);
        }
        
        // Flecha indicadora del planeta destino cuando lleva un paquete
        if (ship.getCarriedPackage() != null) {
            Planet targetPlanet = ship.getCarriedPackage().getTargetPlanet();
            if (targetPlanet != null) {
                // Calcular dirección al planeta
                double dx = targetPlanet.getX() - ship.getX();
                double dy = targetPlanet.getY() - ship.getY();
                double angle = Math.atan2(dy, dx);
                
                // Distancia para la flecha (fuera de la nave)
                int arrowDist = 40;
                int arrowX = x + (int)(Math.cos(angle) * arrowDist);
                int arrowY = y + (int)(Math.sin(angle) * arrowDist);
                
                // Dibujar flecha apuntando al planeta
                g2d.setColor(targetPlanet.getColor());
                g2d.setStroke(new BasicStroke(3));
                
                AffineTransform oldTransform = g2d.getTransform();
                g2d.translate(arrowX, arrowY);
                g2d.rotate(angle);
                
                // Dibujar flecha
                int[] xPoints = {15, 0, 0};
                int[] yPoints = {0, -6, 6};
                g2d.fillPolygon(xPoints, yPoints, 3);
                
                g2d.setTransform(oldTransform);
            }
        }
    }
    
    private void drawShipTrail(Graphics2D g2d, Ship ship, int camX, int camY) {
        if (fireTrailFrames == null || fireTrailFrames.length == 0) return;
        
        // Calcular velocidad para escalar el trail
        Point2D.Double vel = ship.getVelocity();
        double speed = Math.sqrt(vel.x * vel.x + vel.y * vel.y);
        double maxSpeed = 8.0; // MAX_SPEED from Ship
        
        // Escalar tamaño basado en velocidad (0.3 a 1.0)
        double speedRatio = Math.min(speed / maxSpeed, 1.0);
        double scaleFactor = 0.3 + (speedRatio * 0.7); // De 0.3 a 1.0
        if (speedRatio < 0.05) return; // No mostrar trail si casi parado
        
        // Actualizar animación
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > FRAME_DELAY) {
            trailFrame = (trailFrame + 1) % fireTrailFrames.length;
            lastFrameTime = currentTime;
        }
        
        // Posición en la cola de la nave
        // El sprite del trail YA ESTÁ GIRADO correctamente, solo necesitamos posicionarlo
        double shipAngle = ship.getRotationAngle();
        // Calcular la parte trasera de la nave (opuesto a la dirección de movimiento)
        double trailAngle = shipAngle + Math.PI;
        int offsetDist = 25; // Distancia desde el centro de la nave
        int offsetX = (int)(Math.cos(trailAngle) * offsetDist);
        int offsetY = (int)(Math.sin(trailAngle) * offsetDist);
        
        int x = (int)ship.getX() + offsetX + camX;
        int y = (int)ship.getY() + offsetY + camY;
        
        // Dibujar sprite de fuego rotado con la nave
        BufferedImage trailSprite = fireTrailFrames[trailFrame];
        if (trailSprite != null) {
            AffineTransform oldTransform = g2d.getTransform();
            
            // Mover al punto de dibujo
            g2d.translate(x, y);
            // Rotar según la orientación de la nave (+ 90° para coincidir con sprite)
            g2d.rotate(shipAngle + Math.PI / 2);
            
            // Calcular tamaño escalado
            int w = (int)(trailSprite.getWidth() * scaleFactor);
            int h = (int)(trailSprite.getHeight() * scaleFactor);
            
            // Dibujar centrado
            g2d.drawImage(trailSprite, -w/2, -h/2, w, h, null);
            
            g2d.setTransform(oldTransform);
        }
    }
    
    private void drawProjectiles(Graphics2D g2d, int camX, int camY) {
        for (Projectile proj : model.getProjectiles()) {
            if (!proj.isActive()) continue;
            
            int x = (int)proj.getX() + camX;
            int y = (int)proj.getY() + camY;
            
            // Usar sprite laserBlue14 con rotación
            if (projectileFrames != null && projectileFrames.length > 0 && projectileFrames[0] != null) {
                BufferedImage sprite = projectileFrames[0];
                
                AffineTransform oldTransform = g2d.getTransform();
                g2d.translate(x, y);
                
                // Rotar según dirección del proyectil (compensar +90° para vertical)
                double angle = proj.getAngle() + Math.PI / 2;
                g2d.rotate(angle);
                
                // Escalar al 75% del tamaño original para mejor visibilidad
                int w = (int)(sprite.getWidth() * 0.75);
                int h = (int)(sprite.getHeight() * 0.75);
                g2d.drawImage(sprite, -w/2, -h/2, w, h, null);
                
                g2d.setTransform(oldTransform);
            }
        }
    }
    
    private void drawExplosions(Graphics2D g2d, int camX, int camY) {
        // Agregar nuevas explosiones desde el modelo (snapshot de solo lectura)
        List<java.awt.Point> newExplosions = model.getExplosionPositions();
        List<Integer> newSizes = model.getExplosionSizes();
        for (int i = 0; i < newExplosions.size(); i++) {
            java.awt.Point pos = newExplosions.get(i);
            int radius = i < newSizes.size() ? newSizes.get(i) : 25;
            // Escala basada directamente en el radio del asteroide
            double scale = radius / 25.0; // Normalizado: 15px=0.6, 25px=1.0, 40px=1.6
            explosions.add(new ExplosionAnimation(pos.x, pos.y, scale));
        }
        
        // Actualizar y dibujar explosiones
        explosions.removeIf(exp -> exp.isFinished());
        
        if (explosionSheet == null) return;
        
        for (ExplosionAnimation exp : explosions) {
            if (exp.isFinished()) continue;
            
            int frame = exp.getCurrentFrame();
            int x = (int)exp.x + camX;
            int y = (int)exp.y + camY;
            
            // 11 frames, cada uno de 64x64 píxeles en tira horizontal
            if (frame >= 11) continue;
            
            // Calcular offset en la tira horizontal (704×64px)
            // Frame 0: x=0, Frame 1: x=64, Frame 2: x=128, ... Frame 10: x=640
            int srcX = frame * 64;  // Offset horizontal (0, 64, 128, 192...)
            int srcY = 0;                // Siempre 0 (una sola fila)
            
            try {
                // Recortar frame de 64×64 píxeles de la tira horizontal
                BufferedImage frameImg = explosionSheet.getSubimage(srcX, srcY, 64, 64);
                
                // Tamaño adaptado al meteorito
                int size = (int)(64 * exp.scale);
                g2d.drawImage(frameImg, x - size/2, y - size/2, size, size, null);
            } catch (Exception e) {
                // Si hay error leyendo el frame, continuar
            }
        }
    }
    
    private void drawFloatingTexts(Graphics2D g2d, int camX, int camY) {
        long currentTime = System.currentTimeMillis();
        for (FloatingText text : model.getFloatingTexts()) {
            int x = (int)text.getX() + camX;
            int y = (int)text.getY(currentTime) + camY;
            
            // Asegurar que alpha está en rango 0-255
            int alpha = Math.max(0, Math.min(255, text.getAlpha(currentTime)));
            g2d.setColor(new Color(255, 255, 255, alpha));
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(text.getText(), x - fm.stringWidth(text.getText()) / 2, y);
        }
    }
    
    private void drawHUD(Graphics2D g2d) {
        Ship ship = model.getPlayerShip();
        
        // Lives and score panel (top left)
        drawPanel(g2d, 10, 10, 200, 80);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("VIDAS: " + ship.getLives(), 25, 35);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Puntos: " + model.getTotalScore(), 25, 60);
        
        // Boost panel (bottom left)
        drawBoostBar(g2d, ship);
        
        // Timer panel (top center)
        drawTimerPanel(g2d);
        
        // Level objectives panel (right)
        drawObjectivesPanel(g2d);
        
        // Network status panel (bottom right)
        drawNetworkPanel(g2d);
        
        // Combo counter (top center-right)
        drawComboCounter(g2d);
        
        // Game over overlay
        if (model.isGameOver()) {
            drawGameOverScreen(g2d);
        }
    }
    
    private void drawPanel(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRoundRect(x, y, width, height, 15, 15);
        g2d.setColor(new Color(100, 150, 255, 150));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(x, y, width, height, 15, 15);
    }
    
    private void drawBoostBar(Graphics2D g2d, Ship ship) {
        int x = 10;
        int y = getHeight() - 60;
        int width = 200;
        int height = 40;
        
        drawPanel(g2d, x, y, width, height);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("BOOST", x + 15, y + 18);
        
        // Boost bar with gradient
        int barX = x + 15;
        int barY = y + 25;
        int barWidth = width - 30;
        int barHeight = 10;
        
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRoundRect(barX, barY, barWidth, barHeight, 5, 5);
        
        int boostWidth = (int)(barWidth * ship.getBoost() / 100.0);
        Color boostColor1 = ship.getBoost() > 30 ? new Color(0, 200, 255) : new Color(255, 100, 0);
        Color boostColor2 = ship.getBoost() > 30 ? new Color(0, 100, 200) : new Color(200, 50, 0);
        
        GradientPaint gradient = new GradientPaint(barX, barY, boostColor1, barX + boostWidth, barY, boostColor2);
        g2d.setPaint(gradient);
        g2d.fillRoundRect(barX, barY, boostWidth, barHeight, 5, 5);
    }
    
    private void drawTimerPanel(Graphics2D g2d) {
        long currentTime = System.currentTimeMillis();
        int remaining = model.getRemainingTime(currentTime);
        boolean critical = remaining < 30;
        
        int width = 150;
        int x = getWidth() / 2 - width / 2;
        int y = 10;
        int height = 50;
        
        // Pulse effect when critical
        if (critical) {
            float pulse = (float)(Math.sin(System.currentTimeMillis() / 200.0) * 0.2 + 0.8);
            g2d.setColor(new Color(255, 0, 0, (int)(120 * pulse)));
        } else {
            g2d.setColor(new Color(0, 0, 0, 120));
        }
        g2d.fillRoundRect(x, y, width, height, 15, 15);
        
        g2d.setColor(critical ? Color.RED : new Color(100, 150, 255, 150));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(x, y, width, height, 15, 15);
        
        g2d.setColor(critical ? Color.RED : Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        String timeText = String.format("%02d:%02d", remaining / 60, remaining % 60);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(timeText, x + width / 2 - fm.stringWidth(timeText) / 2, y + 33);
    }
    
    private void drawObjectivesPanel(Graphics2D g2d) {
        List<LevelObjective> objectives = model.getLevelObjectives();
        if (objectives.isEmpty()) return;
        
        int width = 250;
        int height = 30 + objectives.size() * 25;
        int x = getWidth() - width - 10;
        int y = 10;
        
        drawPanel(g2d, x, y, width, height);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("NIVEL " + model.getCurrentLevel(), x + 15, y + 22);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        int offsetY = 45;
        for (LevelObjective obj : objectives) {
            // Contar paquetes pendientes NORMALES para este planeta
            int pendingPackages = 0;
            for (model.Package pkg : model.getPackages()) {
                if (pkg.getTargetPlanet() == obj.getTargetPlanet() && 
                    pkg.getType() == PackageType.NORMAL &&
                    !pkg.isCollected() && !pkg.isPickedUp()) {
                    pendingPackages++;
                }
            }
            
            // Color: verde brillante si completado, blanco si faltan paquetes
            if (obj.isCompleted() || pendingPackages == 0) {
                g2d.setColor(new Color(100, 255, 100)); // Verde brillante
            } else {
                g2d.setColor(Color.WHITE);
            }
            
            // Nombre del planeta
            String planetName = obj.getTargetPlanet().getName();
            g2d.drawString(planetName, x + 15, y + offsetY);
            
            // Mostrar número solo si hay paquetes pendientes
            if (pendingPackages > 0) {
                g2d.setColor(new Color(255, 200, 100)); // Amarillo/naranja para el número
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                g2d.drawString(" (" + pendingPackages + ")", x + 15 + g2d.getFontMetrics().stringWidth(planetName), y + offsetY);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            }
            
            offsetY += 25;
        }
    }
    
    private void drawComboCounter(Graphics2D g2d) {
        // Placeholder for combo counter
        int combo = 0;
        if (combo <= 1) return;
        
        float scale = (float)(1.0 + combo * 0.03);
        int width = 120;
        int height = 60;
        int x = getWidth() / 2 - width / 2;
        int y = 80;
        
        drawPanel(g2d, x, y, width, height);
        
        g2d.setColor(new Color(255, 215, 0));
        g2d.setFont(new Font("Arial", Font.BOLD, (int)(24 * scale)));
        String comboText = "x" + combo;
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(comboText, x + width / 2 - fm.stringWidth(comboText) / 2, y + 40);
    }
    
    private void drawGameOverScreen(Graphics2D g2d) {
        // Overlay oscuro
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        boolean won = model.isGameWon();
        int score = model.getTotalScore();
        
        if (won) {
            // Pantalla de Victoria
            g2d.setColor(new Color(255, 215, 0));
            g2d.setFont(new Font("Arial", Font.BOLD, 60));
            String title = "¡VICTORIA!";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, getWidth() / 2 - fm.stringWidth(title) / 2, getHeight() / 2 - 80);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            String congrats = "¡Has completado todos los niveles!";
            fm = g2d.getFontMetrics();
            g2d.drawString(congrats, getWidth() / 2 - fm.stringWidth(congrats) / 2, getHeight() / 2 - 20);
            
        } else {
            // Pantalla de Derrota
            g2d.setColor(new Color(220, 50, 50));
            g2d.setFont(new Font("Arial", Font.BOLD, 60));
            String title = "GAME OVER";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, getWidth() / 2 - fm.stringWidth(title) / 2, getHeight() / 2 - 80);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            String msg = "Tu nave ha sido destruida";
            fm = g2d.getFontMetrics();
            g2d.drawString(msg, getWidth() / 2 - fm.stringWidth(msg) / 2, getHeight() / 2 - 20);
        }
        
        // Puntuación
        g2d.setColor(new Color(255, 255, 100));
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String scoreText = "Puntuación: " + score;
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(scoreText, getWidth() / 2 - fm.stringWidth(scoreText) / 2, getHeight() / 2 + 40);
        
        // Opciones
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String restart = "ENTER - Volver a jugar";
        fm = g2d.getFontMetrics();
        g2d.drawString(restart, getWidth() / 2 - fm.stringWidth(restart) / 2, getHeight() / 2 + 100);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String exit = "ESC - Salir";
        fm = g2d.getFontMetrics();
        g2d.drawString(exit, getWidth() / 2 - fm.stringWidth(exit) / 2, getHeight() / 2 + 135);
    }
    
    /**
     * Dibuja el panel de estado de red P2P (bottom right).
     */
    private void drawNetworkPanel(Graphics2D g2d) {
        int peerCount = model.getRemoteShips().size();
        
        // Solo mostrar si hay peers conectados
        if (peerCount == 0) {
            return;
        }
        
        int width = 180;
        int height = 60;
        int x = getWidth() - width - 10;
        int y = getHeight() - height - 10;
        
        drawPanel(g2d, x, y, width, height);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("RED P2P", x + 15, y + 25);
        
        g2d.setColor(new Color(100, 255, 100));
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String peerText = peerCount == 1 ? "1 nave remota" : peerCount + " naves remotas";
        g2d.drawString(peerText, x + 15, y + 45);
        
        // Indicador de conexión (dot pulsante)
        float pulse = (float)(Math.sin(System.currentTimeMillis() / 500.0) * 0.3 + 0.7);
        g2d.setColor(new Color(100, 255, 100, (int)(255 * pulse)));
        g2d.fillOval(x + width - 25, y + 18, 10, 10);
    }
}
