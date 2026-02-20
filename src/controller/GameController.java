package controller;

import model.GameModel;
import model.Ship;
import model.FloatingText;
import model.network.NetworkMessage;
import model.network.PeerInfo;
import view.GameView;
import services.*;

import javax.swing.*;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Controlador del juego que gestiona la entrada de teclado y el ciclo de actualización.
 * Controles arcade: WASD/Flechas para 8 direcciones, SHIFT/SPACE para boost.
 * Incluye sincronización P2P con NetworkController.
 * REFACTORIZADO: Usa capa de servicios para separar lógica de negocio.
 */
public class GameController implements KeyListener, MouseListener, NetworkController.NetworkListener {
    private final GameModel model;
    private final GameView view;
    private final Timer gameTimer;
    private NetworkController networkController; // Sistema P2P
    
    // === CAPA DE SERVICIOS (MVC) ===
    private final PhysicsService physics;
    private final CollisionService collisions;
    private final SpawnService spawner;
    private final ScoreService scores;
    private final CameraService camera;
    
    // Estado de las teclas presionadas
    private boolean upPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean boostPressed = false;
    private boolean emergencyBrakePressed = false; // S (antes R)
    private boolean shootPressed = false; // SPACE (disparo)
    
    // FPS objetivo
    private static final int TARGET_FPS = 60;
    private static final int DELAY_MS = 1000 / TARGET_FPS;
    
    // Network update rate (100ms = 10 updates/second)
    private static final long NETWORK_UPDATE_INTERVAL_MS = 100;
    private long lastNetworkUpdate = 0;
    
    public GameController(GameModel model, GameView view) {
        this(model, view, 8888); // Puerto por defecto
    }
    
    public GameController(GameModel model, GameView view, int networkPort) {
        this.model = model;
        this.view = view;
        
        // === INICIALIZAR SERVICIOS (MVC REFACTORIZADO) ===
        this.physics = new PhysicsService();
        this.spawner = new SpawnService();
        this.scores = new ScoreService();
        this.camera = new CameraService();
        this.collisions = new CollisionService(physics, scores, spawner);
        
        System.out.println("[MVC] Capa de servicios inicializada");
        
        // Inicializar NetworkController
        try {
            this.networkController = new NetworkController(networkPort);
            this.networkController.addListener(this);
            System.out.println("Sistema P2P inicializado en puerto " + networkPort);
        } catch (Exception e) {
            System.err.println("Error inicializando sistema P2P: " + e.getMessage());
            this.networkController = null;
        }
        
        // Configurar el listener de teclado
        view.addKeyListener(this);
        view.addMouseListener(this);
        view.setFocusable(true);
        view.requestFocusInWindow();
        
        // Crear el timer del juego (60 FPS)
        gameTimer = new Timer(DELAY_MS, e -> gameLoop());
    }
    
    /**
     * Inicia el ciclo del juego.
     */
    public void start() {
        // Inicializar cámara centrada en la nave
        Ship playerShip = model.getPlayerShip();
        if (playerShip != null) {
            camera.updateCamera(playerShip, view.getWidth(), view.getHeight());
            // Forzar posición inicial (sin interpolación) llamando varias veces para converger rápido
            for (int i = 0; i < 20; i++) {
                camera.updateCamera(playerShip, view.getWidth(), view.getHeight());
            }
            // Sincronizar con GameModel
            Point offset = model.getCameraOffset();
            offset.x = -(int)camera.getCameraX();
            offset.y = -(int)camera.getCameraY();
        }
        
        gameTimer.start();
        
        // Iniciar sistema P2P
        if (networkController != null) {
            try {
                networkController.start();
                System.out.println("Sistema P2P iniciado");
            } catch (Exception e) {
                System.err.println("Error iniciando red P2P: " + e.getMessage());
            }
        }
    }
    
    /**
     * Detiene el ciclo del juego.
     */
    public void stop() {
        gameTimer.stop();
        
        // Detener sistema P2P
        if (networkController != null) {
            networkController.stop();
            System.out.println("Sistema P2P detenido");
        }
    }
    
    /**
     * Ciclo principal del juego ejecutado cada frame.
     * REFACTORIZADO: Orquesta servicios en lugar de llamar model.update()
     */
    private void gameLoop() {
        // Tiempo único para todo el frame (inyectado a todos los servicios y modelos)
        long currentTime = System.currentTimeMillis();
        
        // Procesar entrada del jugador
        processInput(currentTime);
        
        // === ORQUESTACIÓN DE SERVICIOS (MVC) ===
        Ship playerShip = model.getPlayerShip();
        
        // 1. Actualizar física de la nave
        if (playerShip != null && playerShip.isAlive()) {
            physics.updateShipPhysics(playerShip);
            physics.updateBoost(playerShip); // [BUG FIX] Actualizar boost (consumo/recarga)
            
            // 2. Actualizar cámara (smooth scrolling)
            camera.updateCamera(playerShip, view.getWidth(), view.getHeight());
            
            // Sincronizar cámara con GameModel (para GameView)
            // CameraService devuelve posición del viewport en el mundo (top-left)
            // GameView usa offset negativo para dibujar
            Point offset = model.getCameraOffset();
            offset.x = -(int)camera.getCameraX();
            offset.y = -(int)camera.getCameraY();
        }
        
        // 3. Actualizar física de asteroides
        for (model.Asteroid asteroid : model.getAsteroids()) {
            physics.updateAsteroidPhysics(asteroid);
        }
        
        // 4. Actualizar física de proyectiles
        for (model.Projectile projectile : model.getProjectiles()) {
            physics.updateProjectilePhysics(projectile);
        }
        
        // 5. Detectar colisiones (nave-asteroides)
        if (playerShip != null && playerShip.isAlive()) {
            CollisionService.ShipAsteroidResult shipCollision = 
                collisions.checkShipAsteroidCollisions(playerShip, model.getAsteroids(), currentTime);
            
            if (shipCollision.collision) {
                scores.resetCombo();
                if (shipCollision.shipDestroyed) {
                    model.triggerGameOver(currentTime);
                }
            }
        }
        
        // 6. Actualizar combos e inmunidad con tiempo inyectado
        scores.update(currentTime);
        
        // Actualizar estado de inmunidad del jugador
        if (playerShip != null) {
            playerShip.isImmune(currentTime); // expira si ya pasaron 2 segundos
        }
        
        // 7. Orquestar lógica de juego (MVC - Controller orquesta, modelo NO se auto-actualiza)
        if (!model.isGameOver()) {
            // Verificar condiciones de Game Over
            if (playerShip != null && (playerShip.getLives() <= 0 || model.getRemainingTime() <= 0)) {
                model.triggerGameOver(currentTime);
            } else {
                // Limpiar proyectiles inactivos
                model.getProjectiles().removeIf(p -> !p.isActive());
                
                // Actualizar textos flotantes (limpiar expirados)
                model.getFloatingTexts().removeIf(text -> text.isExpired(currentTime));
                
                // Verificar recogida y entrega de paquetes
                if (playerShip != null) {
                    if (!playerShip.hasPackage()) {
                        model.checkPackagePickup(currentTime);
                    } else {
                        model.checkPackageDelivery(currentTime);
                    }
                }
                
                // Eliminar paquetes urgentes expirados
                model.getPackages().removeIf(pkg -> !pkg.isCollected() && pkg.isExpired(currentTime));
                
                // Regenerar paquetes faltantes
                model.checkAndRegeneratePackages(currentTime);
                
                // Generar asteroides periódicamente
                model.spawnAsteroidsOverTime(currentTime);
                
                // Verificar colisiones (proyectiles-asteroides)
                // TODO: Migrar a CollisionService
                model.checkProjectileCollision(currentTime);
            }
        }
        
        // 8. Enviar actualización de red (throttled)
        sendNetworkUpdate();
        
        // 9. Redibujar la vista
        view.repaint();
    }
    
    /**
     * Procesa la entrada del teclado y controla la nave.
     * REFACTORIZADO: Usa PhysicsService en lugar de métodos de Ship.
     */
    private void processInput(long currentTime) {
        Ship ship = model.getPlayerShip();
        if (ship == null || !ship.isAlive()) {
            return;
        }
        
        // Rotación (A/D) - USA PHYSICSSERVICE
        if (leftPressed) {
            physics.applyRotation(ship, -1);
        }
        if (rightPressed) {
            physics.applyRotation(ship, 1);
        }
        
        // Empuje solo hacia adelante (W) - USA PHYSICSSERVICE
        if (upPressed) {
            physics.applyThrust(ship, 1.0, boostPressed);
        }
        
        // Freno de emergencia (S) - USA PHYSICSSERVICE
        if (emergencyBrakePressed) {
            physics.applyBrake(ship);
        }
        
        // Disparo (SPACE) - permite disparo continuo con cooldown
        if (shootPressed) {
            model.shootProjectile(currentTime);
        }
        
        // Activar/desactivar boost
        ship.setBoostActive(boostPressed);
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        // Si el juego terminó, manejar solo ENTER y ESC
        if (model.isGameOver()) {
            if (key == KeyEvent.VK_ENTER) {
                model.restartGame();
            } else if (key == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
            return;
        }
        
        // Rotación (A/D y flechas)
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
            leftPressed = true;
        }
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
            rightPressed = true;
        }
        
        // Aceleración (W y flecha arriba)
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
            upPressed = true;
        }
        
        // Freno de emergencia (S y flecha abajo)
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
            emergencyBrakePressed = true;
        }
        
        // Boost (SHIFT) y Disparo (SPACE)
        if (key == KeyEvent.VK_SHIFT) {
            boostPressed = true;
        }
        if (key == KeyEvent.VK_SPACE) {
            shootPressed = true;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        
        // Rotación
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
            leftPressed = false;
        }
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
            rightPressed = false;
        }
        
        // Aceleración
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
            upPressed = false;
        }
        
        // Freno de emergencia
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
            emergencyBrakePressed = false;
        }
        
        // Boost
        if (key == KeyEvent.VK_SHIFT) {
            boostPressed = false;
        }
        
        // Disparo
        if (key == KeyEvent.VK_SPACE) {
            shootPressed = false;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // No se necesita implementación
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        // Si está en Game Over, detectar clics en botones
        if (model.isGameOver()) {
            int mouseX = e.getX();
            int mouseY = e.getY();
            
            int buttonWidth = 200;
            int buttonHeight = 50;
            int buttonX = (view.getWidth() - buttonWidth) / 2;
            int playButtonY = 370;
            int exitButtonY = 440;
            
            // Botón Volver a Jugar
            if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                mouseY >= playButtonY && mouseY <= playButtonY + buttonHeight) {
                model.restartGame();
                view.requestFocusInWindow();
            }
            
            // Botón Salir
            if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                mouseY >= exitButtonY && mouseY <= exitButtonY + buttonHeight) {
                System.exit(0);
            }
        }
    }
    
    @Override
    public void mousePressed(MouseEvent e) {}
    
    @Override
    public void mouseReleased(MouseEvent e) {}
    
    @Override
    public void mouseEntered(MouseEvent e) {}
    
    @Override
    public void mouseExited(MouseEvent e) {}
    
    // ========== Network P2P Methods ==========
    
    /**
     * Envía actualización de la nave local a la red (throttled).
     */
    private void sendNetworkUpdate() {
        if (networkController == null || !networkController.isRunning()) {
            return;
        }
        
        long now = System.currentTimeMillis();
        if (now - lastNetworkUpdate < NETWORK_UPDATE_INTERVAL_MS) {
            return; // Throttle
        }
        
        lastNetworkUpdate = now;
        
        Ship localShip = model.getPlayerShip();
        if (localShip == null || !localShip.isAlive()) {
            return;
        }
        
        // Crear datos de la nave
        NetworkMessage.ShipData shipData = new NetworkMessage.ShipData(
            localShip.getShipId(),
            localShip.getX(),
            localShip.getY(),
            localShip.getVelX(),
            localShip.getVelY(),
            localShip.getRotationAngle(),
            networkController.getLocalPeerId(),
            localShip.getLives(),
            localShip.hasPackage()
        );
        
        // Enviar a la red
        networkController.sendShipUpdate(shipData);
    }
    
    /**
     * Callback cuando un nuevo peer se conecta.
     */
    @Override
    public void onPeerConnected(PeerInfo peer) {
        System.out.println("Peer conectado: " + peer);
    }
    
    /**
     * Callback cuando un peer se desconecta.
     */
    @Override
    public void onPeerDisconnected(PeerInfo peer) {
        System.out.println("Peer desconectado: " + peer);
        
        // Eliminar naves de este peer
        model.removeRemoteShipsByOwner(peer.getPeerId());
    }
    
    /**
     * Callback cuando se recibe una actualización de nave remota.
     */
    @Override
    public void onShipUpdate(NetworkMessage.ShipData shipData) {
        // Actualizar o añadir nave remota en el modelo
        model.addOrUpdateRemoteShip(
            shipData.shipId,
            shipData.ownerPeerId,
            shipData.posX,
            shipData.posY,
            shipData.velX,
            shipData.velY,
            shipData.angle,
            shipData.lives,
            shipData.hasPackage
        );
    }
    
    /**
     * Obtiene el peer ID local.
     */
    public String getLocalPeerId() {
        return networkController != null ? networkController.getLocalPeerId() : "Local";
    }
    
    /**
     * Obtiene el número de peers conectados.
     */
    public int getPeerCount() {
        return networkController != null ? networkController.getPeerCount() : 0;
    }
}
