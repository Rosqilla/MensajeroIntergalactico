package controller;

import model.GameModel;
import model.Ship;
import model.network.NetworkMessage;
import model.network.PeerInfo;
import view.GameView;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Controlador del juego que gestiona la entrada de teclado y el ciclo de actualización.
 * Controles arcade: WASD/Flechas para 8 direcciones, SHIFT/SPACE para boost.
 * Incluye sincronización P2P con NetworkController.
 */
public class GameController implements KeyListener, MouseListener, NetworkController.NetworkListener {
    private final GameModel model;
    private final GameView view;
    private final Timer gameTimer;
    private NetworkController networkController; // Sistema P2P
    
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
     */
    private void gameLoop() {
        // Procesar entrada y controlar la nave
        processInput();
        
        // Actualizar el modelo (posiciones, física, cámara)
        model.update();
        
        // Enviar actualización de red (throttled)
        sendNetworkUpdate();
        
        // Redibujar la vista
        view.repaint();
    }
    
    /**
     * Procesa la entrada del teclado y controla la nave.
     * Movimiento espacial completo: rotación + thrust + strafe + freno.
     */
    private void processInput() {
        Ship ship = model.getPlayerShip();
        if (ship == null || !ship.isAlive()) {
            return;
        }
        
        // Rotación (A/D)
        if (leftPressed) {
            ship.rotate(-1);
        }
        if (rightPressed) {
            ship.rotate(1);
        }
        
        // Empuje solo hacia adelante (W)
        if (upPressed) {
            ship.thrust(1);
        }
        
        // Freno de emergencia (S)
        if (emergencyBrakePressed) {
            ship.emergencyBrake();
        }
        
        // Disparo (SPACE) - permite disparo continuo con cooldown
        if (shootPressed) {
            model.shootProjectile();
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
