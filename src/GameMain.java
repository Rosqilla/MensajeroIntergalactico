import controller.GameController;
import model.GameModel;
import view.GameView;

import javax.swing.*;
import java.awt.*;

/**
 * Clase principal del juego Mensajero Intergaláctico.
 * Inicializa el modelo, vista y controlador, y arranca el juego.
 * Soporte P2P: Ejecuta con argumento de puerto (ej: java GameMain 8888)
 */
public class GameMain {
    public static void main(String[] args) {
        // Parsear puerto de red (por defecto 8888)
        int networkPort = 8888;
        if (args.length > 0) {
            try {
                networkPort = Integer.parseInt(args[0]);
                System.out.println("Puerto de red configurado: " + networkPort);
            } catch (NumberFormatException e) {
                System.err.println("Puerto inválido, usando por defecto: 8888");
            }
        }
        
        final int port = networkPort;
        
        SwingUtilities.invokeLater(() -> {
            // 1. Instanciar el modelo del juego (tiempo inyectado — MVC)
            long initTime = System.currentTimeMillis();
            GameModel model = new GameModel(800, 600, initTime);
            
            // 2. Instanciar la vista con el modelo
            GameView view = new GameView(model);
            
            // 3. Instanciar el controlador con modelo, vista y puerto P2P
            GameController controller = new GameController(model, view, port);
            
            // 4. Crear y configurar la ventana principal
            JFrame frame = new JFrame("Mensajero Intergaláctico [Puerto " + port + "]");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            
            // Añadir la vista al frame
            frame.add(view, BorderLayout.CENTER);
            
            // Mostrar la ventana
            frame.setVisible(true);
            
            // Asegurar que la vista tenga el foco para capturar teclas
            view.requestFocusInWindow();
            
            // 5. Iniciar el timer del controlador
            controller.start();
            
            // Mensaje en consola
            System.out.println("===========================================");
            System.out.println("  MENSAJERO INTERGALÁCTICO - ARCADE MODE");
            System.out.println("===========================================");
            System.out.println("Controles Completos:");
            System.out.println("  A/D o ←/→      : Rotar nave");
            System.out.println("  W o ↑          : Acelerar adelante");
            System.out.println("  S o ↓          : Freno de emergencia");
            System.out.println("  Q / E          : Strafe lateral (⬅/➡)");
            System.out.println("  SHIFT          : Boost (x2.5 potencia)");
            System.out.println("  ESPACIO        : Disparar (1/segundo)");
            System.out.println("===========================================");
            System.out.println("Sistema Arcade:");
            System.out.println("  - Vidas: 3 (máx 5) - ♥♥♥");
            System.out.println("  - Tiempo: 4:00 minutos por nivel");
            System.out.println("  - Boost: Recarga automática");
            System.out.println("  - Disparos: Recarga automática");
            System.out.println("  - +15s por paquete entregado");
            System.out.println("  - Inmunidad de 2s tras golpe");
            System.out.println("  - Combos por entregas consecutivas");
            System.out.println("===========================================");
            System.out.println("Asteroides:");
            System.out.println("  - Pequeños: 1 vida, 10 puntos");
            System.out.println("  - Medianos: 2 vidas, 25 puntos");
            System.out.println("  - Grandes: 5 vidas, 50 puntos");
            System.out.println("===========================================");
            System.out.println("Objetivo:");
            System.out.println("  - Recoge los paquetes (cuadrados)");
            System.out.println("  - Entrégalos a los planetas correctos");
            System.out.println("  - ¡Destruye asteroides para puntos!");
            System.out.println("  - Mantén combos para multiplicadores");
            System.out.println("  - ¡Sobrevive y entrega todo!");
            System.out.println("===========================================");
            System.out.println("Sistema P2P Multijugador:");
            System.out.println("  - Puerto: " + port);
            System.out.println("  - Red: 230.0.0.1:4446 (multicast)");
            System.out.println("  - Naves remotas: Color diferenciado");
            System.out.println("  - Para 2+ jugadores: Ejecuta varias instancias");
            System.out.println("    Ej: java GameMain 8888");
            System.out.println("        java GameMain 8889");
            System.out.println("===========================================");
        });
    }
}
