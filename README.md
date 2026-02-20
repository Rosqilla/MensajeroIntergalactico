# 🚀 Mensajero Intergaláctico - Juego Arcade Espacial

Juego arcade de naves espaciales con mecánicas tipo Asteroids, sistema de misiones y multijugador P2P en red local. Construido en Java con arquitectura MVC.

## 🎮 Descripción del Juego

**Mensajero Intergaláctico** es un juego de acción arcade donde controlas una nave espacial encargada de entregar paquetes entre planetas mientras evitas y destruyes asteroides. El juego incluye:

- ✨ **5 niveles progresivos** con dificultad creciente
- 🎯 **Sistema de misiones** con objetivos por nivel
- 💥 **Combate espacial** con proyectiles y explosiones animadas
- ⏱️ **Sistema de tiempo** con bonificación por entregas
- 🏆 **Puntuación y combos** por destruir asteroides y completar misiones
- 🌐 **Multijugador P2P** en red local (ver otras naves en tiempo real)

## 🎯 Objetivo del Juego

1. **Recoger paquetes** flotando en el espacio (cuadrados de colores)
2. **Entregarlos al planeta correcto** (coincide con el color del paquete)
3. **Destruir asteroides** para ganar puntos y despejar el camino
4. **Sobrevivir** - Tienes 3 vidas, cada colisión con asteroide te quita una
5. **Gestionar el tiempo** - Cada nivel tiene un límite de tiempo, las entregas añaden +15 segundos

## 🕹️ Controles

### Movimiento de Nave (Estilo Asteroids)
- **A / ←**: Rotar nave a la izquierda
- **D / →**: Rotar nave a la derecha
- **W / ↑**: Acelerar en la dirección que apunta la nave
- **S / ↓**: Freno de emergencia (reduce velocidad rápidamente)
- **SHIFT**: Boost (x2.5 velocidad, consume barra de boost)
- **ESPACIO**: Disparar proyectil (cooldown de 300ms)

### Controles Especiales
- **ENTER**: Reiniciar juego (cuando Game Over)
- **ESC**: Salir del juego

## 📦 Tipos de Paquetes

El juego incluye 3 tipos de paquetes con diferentes características:

### 🟦 NORMAL (55% de aparición)
- Color del planeta destino
- Velocidad normal de nave
- Recompensa estándar

### 🔴 URGENTE (25% de aparición)
- Color rojo brillante (255, 50, 50)
- **Bonus**: +50% puntos al entregar
- **Penalización**: Pierde tiempo si no se entrega rápido

### 🟠 HEAVY (15% de aparición)
- Color naranja (200, 120, 50)
- **Penalización**: Nave a 70% de velocidad
- **Bonus**: Mayor puntuación al entregar

### ⭐ SPECIAL (5% de aparición - futuro)
- Efectos especiales (no implementado aún)

## 🌍 Level Generator - Generación Procedural de Niveles

El sistema **LevelGenerator** crea niveles únicos proceduralmente usando algoritmos inteligentes:

### 📍 Generación de Planetas

```java
// El generador crea 3-6 planetas por nivel dependiendo de la dificultad
int planetCount = 3 + (level * 2) / 5; // Nivel 1: 3 planetas, Nivel 5: 5 planetas
```

**Algoritmo matemático de colocación (patrón radial):**

```java
// Paso 1: Calcular distribución angular uniforme
double angleStep = (2 * Math.PI) / planetCount;  // Divide círculo en N partes iguales
double baseRadius = 400;  // Radio base del círculo de colocación

// Paso 2: Colocar cada planeta
for (int i = 0; i < planetCount; i++) {
    // Ángulo para este planeta (distribución angular uniforme)
    double angle = angleStep * i + randomOffset(-0.3, 0.3);  // Variación ±17°
    
    // Radio variable por planeta (círculos concéntricos)
    double radius = baseRadius + random(0, 300);  // Entre 400-700px del centro
    
    // Conversión polar → cartesiana
    double x = centerX + radius * Math.cos(angle);
    double y = centerY + radius * Math.sin(angle);
    
    // Verificar separación mínima (200-300px entre planetas)
    if (distanceToNearestPlanet(x, y) >= 250) {
        createPlanet(x, y, randomRadius(30, 80), randomColor());
    }
}
```

**Patrón visual resultante:**
- **Círculo uniforme**: Planetas distribuidos equitativamente alrededor del centro
- **Sin clusters**: La separación mínima evita que se superpongan
- **Variación orgánica**: Pequeños offsets aleatorios evitan patrón demasiado perfecto
- **Espacio central libre**: Radio mínimo de 400px mantiene centro despejado para spawn

**Propiedades adicionales:**
1. **Evita el centro**: No coloca planetas donde inicia el jugador (< 400px)
2. **Variedad de tamaños**: Radios entre 30-80 píxeles (aleatorio)
3. **Colores únicos**: Cada planeta tiene color distintivo RGB para el sistema de misiones
4. **Nombres procedurales**: "Kepler-42", "Proxima-7", etc. (generados aleatoriamente)

**Ejemplo de parámetros por nivel:**
```
Nivel 1: 3 planetas, 5 asteroides,  240 segundos
Nivel 2: 4 planetas, 8 asteroides,  210 segundos
Nivel 3: 4 planetas, 12 asteroides, 180 segundos
Nivel 4: 5 planetas, 15 asteroides, 150 segundos
Nivel 5: 5 planetas, 20 asteroides, 120 segundos
```

### 🌑 Generación de Asteroides (Life Generator)

El sistema crea asteroides con diferentes tipos y comportamientos:

**Tipos de asteroides:**
- **SMALL** (Pequeño): 15px radio, 1 HP, 10 puntos, velocidad alta (1.5-2.5 px/frame)
- **MEDIUM** (Mediano): 25px radio, 2 HP, 25 puntos, velocidad media (1.0-2.0 px/frame)
- **LARGE** (Grande): 40px radio, 5 HP, 50 puntos, velocidad baja (0.5-1.5 px/frame)

**Distribución inteligente por nivel:**
```java
if (level == 1) {
    // Nivel 1: Mayoría pequeños (fácil)
    distribution = [70% SMALL, 30% MEDIUM, 0% LARGE]
} else if (level == 2) {
    distribution = [50% SMALL, 40% MEDIUM, 10% LARGE]
} else if (level == 3) {
    distribution = [40% SMALL, 40% MEDIUM, 20% LARGE]
} else if (level >= 4) {
    // Niveles difíciles: Más grandes y peligrosos
    distribution = [20% SMALL, 40% MEDIUM, 40% LARGE]
}
```

**Algoritmo de generación inicial:**
```java
void generateAsteroids(int count, int level) {
    for (int i = 0; i < count; i++) {
        // Selecciona tipo según distribución del nivel
        AsteroidType type = selectTypeByDistribution(level);
        
        // Posición aleatoria en todo el mapa
        double x = random(0, mapWidth);
        double y = random(0, mapHeight);
        
        // Verificar protección de spawn (200px de la nave)
        if (distanceToShip(x, y) < 200) {
            i--;  // Reintentar esta posición
            continue;
        }
        
        // Velocidad aleatoria (negativo = izquierda/arriba)
        double vx = random(-2.0, 2.0);
        double vy = random(-2.0, 2.0);
        double angularVel = random(-0.05, 0.05);  // Rotación del asteroide
        
        asteroids.add(new Asteroid(x, y, type, vx, vy, angularVel));
    }
}
```

**Sistema de Respawn Automático:**
Cuando un asteroide es destruido, el sistema lo reemplaza para mantener la dificultad:

```java
// En GameModel.update()
if (asteroids.size() < targetAsteroidCount) {
    // Han destruido asteroides, generar uno nuevo
    spawnNewAsteroid();
}

void spawnNewAsteroid() {
    // Spawn fuera de la pantalla visible (evita aparecer frente al jugador)
    double spawnX, spawnY;
    
    int edge = random(0, 4);  // 0=arriba, 1=derecha, 2=abajo, 3=izquierda
    switch(edge) {
        case 0: spawnX = random(0, mapWidth); spawnY = -50; break;
        case 1: spawnX = mapWidth + 50; spawnY = random(0, mapHeight); break;
        case 2: spawnX = random(0, mapWidth); spawnY = mapHeight + 50; break;
        case 3: spawnX = -50; spawnY = random(0, mapHeight); break;
    }
    
    // Velocidad dirigida hacia el mapa (no se alejan)
    double angle = Math.atan2(mapHeight/2 - spawnY, mapWidth/2 - spawnX);
    double speed = random(0.5, 2.0);
    double vx = speed * Math.cos(angle);
    double vy = speed * Math.sin(angle);
    
    asteroids.add(new Asteroid(spawnX, spawnY, selectType(level), vx, vy));
}
```

**Patrón de aparición:**
- **Inicial**: Todos los asteroides spawn simultáneamente al empezar nivel
- **Respawn**: Uno a uno cuando son destruidos (mantiene el count objetivo)
- **Bordes**: Nuevos asteroides aparecen desde fuera de pantalla
- **Dirección**: Se dirigen hacia el centro del mapa (no escapan al vacío)
- **Protección**: NUNCA spawnen dentro del radio de 200px de la nave

### 🎯 Sistema de Objetivos (Level Objectives)

Cada nivel genera 2-4 objetivos de misión automáticamente:

```java
// Ejemplo de objetivo generado:
{
    type: DELIVER_PACKAGES,
    targetPlanet: Planet("Kepler-42", color=BLUE),
    requiredCount: 3,
    completed: false
}
```

**El generador:**
1. Selecciona planetas aleatorios como destinos
2. Determina cuántos paquetes NORMALES hay que entregar (1-3 por planeta)
3. Crea los paquetes en posiciones aleatorias cercanas a la nave
4. El panel de la derecha muestra los objetivos en tiempo real

## 🎨 Sistema de Sprites y Animaciones

El juego usa **sprite sheets** para renderizado eficiente de animaciones:

### 💥 Explosión Sprite Sheet (Tira Horizontal 1×11)

**Archivo**: `resources/sprites/explosion/Explosion Animation.png`

**Cómo se construye el sprite sheet:**
1. **Tamaño total**: 704×64 píxeles (imagen PNG con transparencia)
2. **Layout**: Tira horizontal de 11 frames contiguos (una sola fila)
3. **Tamaño por frame**: 64×64 píxeles (704÷11 = 64)
4. **Estructura del archivo PNG:**
   ```
   [Frame 0][Frame 1][Frame 2][Frame 3][Frame 4][Frame 5][Frame 6][Frame 7][Frame 8][Frame 9][Frame 10]
   ```

**Secuencia de animación:**
- Frame 0-2: Inicio de explosión (pequeña)
- Frame 3-6: Expansión máxima (grande, brillante)  
- Frame 7-10: Disipación (se desvanece)
- Duración de frame: 60ms → animación completa dura ~660ms (11 × 60ms)

**Algoritmo de extracción:**
```java
// Extrae cada frame de la tira horizontal
int frameWidth = 64;
int frameHeight = 64;

for (int i = 0; i < 11; i++) {
    BufferedImage frame = spriteSheet.getSubimage(
        i * frameWidth,  // X offset (se mueve horizontalmente)
        0,               // Y = 0 (fila única)
        frameWidth,      // 64px
        frameHeight      // 64px
    );
}
```

**Escalado dinámico:**
Las explosiones se escalan según el tamaño del asteroide destruido:
```java
double scale = asteroidRadius / 25.0;
// Pequeño (15px): scale = 0.6 → explosión 38×38
// Mediano (25px): scale = 1.0 → explosión 64×64
// Grande  (40px): scale = 1.6 → explosión 102×102
```

### 🔥 Trail Sprite Sheet (Tira Horizontal 1×8)

**Archivo**: `resources/sprites/trail/Group 4 - 4.png`

**Cómo se construye el sprite sheet:**
1. **Layout**: Tira horizontal de 8 frames contiguos (una sola fila)
2. **Dimensiones reales**: 256×48 píxeles
3. **Tamaño por frame**: 32×48 píxeles (256÷8 = 32)
4. **Estructura del archivo PNG:**
   ```
   [Frame 0][Frame 1][Frame 2][Frame 3][Frame 4][Frame 5][Frame 6][Frame 7]
   ```
5. **Contenido de cada frame:**
   - Frame 0: Llama pequeña/inicio
   - Frame 1-3: Llama creciendo (intensidad aumenta)
   - Frame 4-5: Llama máxima (más brillante)
   - Frame 6-7: Llama disminuyendo (fade out)

**Extracción de frames:**
```java
// Extrae cada frame de la tira horizontal (igual que explosión)
int frameWidth = trailImage.getWidth() / 8;  // 256 ÷ 8 = 32

for (int i = 0; i < 8; i++) {
    trailFrames[i] = trailImage.getSubimage(
        i * frameWidth,            // X offset (0, 32, 64, 96...)
        0,                         // Y = 0 (fila única)
        frameWidth,                // 32px
        trailImage.getHeight()     // 48px
    );
}
```

**Similitud con el sprite de explosión:**
Ambos sprite sheets (explosión y trail) usan **el mismo patrón**: tiras horizontales con frames contiguos. La única diferencia es el número de frames (11 vs 8) y el tamaño de cada frame.

**Renderizado con ciclo de animación:**
```java
// GameView.drawShipTrail() — implementación real
double speed = Math.sqrt(vel.x * vel.x + vel.y * vel.y);
double speedRatio = Math.min(speed / 8.0, 1.0);
double scaleFactor = 0.3 + (speedRatio * 0.7); // 0.3 (parado) → 1.0 (velocidad máxima)
if (speedRatio < 0.05) return; // No mostrar si casi parado

// Avanzar frame cada 50ms (independiente de velocidad)
if (currentTime - lastFrameTime > 50) {
    trailFrame = (trailFrame + 1) % 8;
    lastFrameTime = currentTime;
}

// Posicionar en la cola de la nave, rotado igual que la nave
g2d.translate(x_cola, y_cola);
g2d.rotate(shipAngle + Math.PI / 2);
g2d.drawImage(fireTrailFrames[trailFrame], -w/2, -h/2, w, h, null);
```

> **Nota**: La velocidad afecta el **tamaño** (scale 0.3→1.0), no la velocidad de animación. El ciclo de frames siempre avanza a 50ms/frame.

**Creación de tu propio sprite sheet:**
- Software recomendado: Adobe Photoshop, GIMP, Aseprite
- Usa canal alpha (PNG transparente) para bordes suaves
- Mantén tamaño uniforme de frames para evitar distorsión
- Exporta en PNG para preservar transparencia

### 🚀 Ship Sprite

**Archivo**: `resources/sprites/ship/ship_red.png`

**Características:**
- Imagen única de nave (no animada)
- **Escalado**: 60% del tamaño original para mejor jugabilidad
- **Rotación**: Se rota según el ángulo de orientación de la nave
- **Overlay para P2P**: Las naves remotas reciben un tinte de color

### 🎯 Projectile Sprite

**Archivo**: `resources/sprites/projectile/laserBlue14.png`

**Características:**
- Sprite pequeño para proyectiles
- Velocidad fija de 12.0 unidades/frame
- Sin límite de rango (viajan por todo el mapa)
- Colisionan con asteroides

### 📊 Gestión de Sprites en Memoria

**Carga eficiente:**
```java
// Los sprites se cargan UNA VEZ al iniciar GameView
private BufferedImage shipSprite;
private BufferedImage explosionSheet;
private BufferedImage[] trailFrames;

public GameView(GameModel model) {
    loadSprites(); // Carga todos los sprites en memoria
    // ...
}
```

**Cache de frames:**
- Las explosiones extraen frames bajo demanda y los cachean
- Try-catch protege contra errores de carga
- Fallback a renderizado básico si los sprites fallan

## 🌐 Sistema P2P Multijugador

El juego incluye un sistema **peer-to-peer** completo para jugar con amigos en red local:

### 🔌 Arquitectura de Red

**Protocolo**: UDP Multicast
- **Dirección multicast**: 230.0.0.1
- **Puerto multicast**: 4446
- **Descubrimiento**: Automático (sin configuración manual)

**Componentes principales:**

#### 1. NetworkController (Gestión de Hilos)

El NetworkController usa **3 hilos** para operación concurrente:

```java
public class NetworkController {
    // Thread 1: MAIN THREAD (GameController)
    // - Llama a sendShipUpdate() desde el game loop
    // - Ejecuta callbacks (onPeerConnected, onShipUpdate) en el hilo principal
    
    // Thread 2: RECEIVER THREAD (recepción continua)
    private Thread receiverThread = new Thread(() -> {
        while (running) {
            // Espera mensajes UDP del socket multicast
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            multicastSocket.receive(packet);  // BLOQUEANTE
            
            // Parsea y procesa el mensaje
            String message = new String(packet.getData(), 0, packet.getLength());
            NetworkMessage msg = NetworkMessage.deserialize(message);
            handleReceivedMessage(msg);  // Actualiza ConcurrentHashMap
        }
    });
    
    // Thread 3: HEARTBEAT THREAD (anuncios periódicos)
    private Thread heartbeatThread = new Thread(() -> {
        while (running) {
            Thread.sleep(5000);  // Espera 5 segundos
            sendPeerAnnounce();  // Envía "Estoy vivo" al grupo multicast
            cleanupInactivePeers();  // Elimina peers sin heartbeat > 30s
        }
    });
}
```

**Sincronización thread-safe:**
```java
// Usa ConcurrentHashMap para evitar condiciones de carrera
private ConcurrentHashMap<String, PeerInfo> connectedPeers;

// Los callbacks se invocan en el MAIN THREAD (no en receiver thread)
private void notifyListeners(PeerInfo peer) {
    SwingUtilities.invokeLater(() -> {
        for (NetworkListener listener : listeners) {
            listener.onPeerConnected(peer);  // Seguro para UI de Swing
        }
    });
}
```

**Gestión de ciclo de vida:**
```java
NetworkController(int localPort) {
    this.multicastSocket = new MulticastSocket(4446);
    this.multicastSocket.joinGroup(InetAddress.getByName("230.0.0.1"));
    
    // Inicia los 3 hilos
    this.receiverThread.start();   // Background daemon
    this.heartbeatThread.start();  // Background daemon
}

void stop() {
    running = false;  // Señal para detener loops
    sendPeerLeave();  // Notifica desconexión limpia
    receiverThread.join(1000);   // Espera terminación (1s timeout)
    heartbeatThread.join(1000);
    multicastSocket.leaveGroup(multicastAddress);
    multicastSocket.close();
}
```

#### 2. NetworkMessage
Formato de serialización: `TYPE|senderId|key1:value1|key2:value2|...`

**Tipos de mensajes:**
- `PEER_ANNOUNCE`: "¡Hola! Estoy aquí" (heartbeat)
- `SHIP_UPDATE`: Posición y estado de nave
- `PEER_LEAVE`: "Me voy" (desconexión limpia)

#### 3. PeerInfo
```java
class PeerInfo {
    String peerId;        // ID único: "Peer_8888_timestamp"
    InetAddress address;  // IP del peer
    int port;             // Puerto local del peer
    long lastSeen;        // Timestamp para timeout
}
```

### 🎨 Diferenciación Visual de Naves

**Nave local (tu nave):**
- Color original del sprite
- Completamente controlable
- Indicadores normales (vidas, boost, etc.)

**Naves remotas (otros jugadores):**
- ✅ **Overlay de color semitransparente** (alpha 120)
- ✅ **Borde de 2px** del mismo color
- ✅ **8 colores únicos** por peer:
  - Rojo (255, 100, 100)
  - Azul (100, 150, 255)
  - Verde (100, 255, 100)
  - Amarillo (255, 255, 100)
  - Magenta (255, 100, 255)
  - Cian (100, 255, 255)
  - Naranja (255, 150, 50)
  - Violeta (200, 100, 255)
- ✅ **NO controlables** (solo visualización)
- ✅ **Interpolación suave** (compensa latencia de red)

### 🚀 Cómo Jugar en Multijugador

**Opción 1: Scripts automáticos**
```bash
# Windows
run_p2p_demo.bat

# PowerShell
.\run_p2p_demo.ps1

# Linux/macOS
chmod +x run_p2p_demo.sh
./run_p2p_demo.sh
```

**Opción 2: Lanzar instancias manualmente**
```bash
# Compilar
javac -d bin -sourcepath src src/GameMain.java src/controller/*.java src/model/*.java src/model/network/*.java src/services/*.java src/view/*.java

# Instancia 1 (Jugador 1)
java -cp bin GameMain 8888

# Instancia 2 (Jugador 2) - en otra terminal
java -cp bin GameMain 8889

# Instancia 3, 4, etc.
java -cp bin GameMain 8890
java -cp bin GameMain 8891
```

### 📡 Sincronización en Tiempo Real

**Frecuencia de actualización:**
- **Envío**: 10 actualizaciones/segundo (cada 100ms)
- **Datos enviados**: Posición (x, y), velocidad (vx, vy), ángulo, vidas, estado de paquete

**Interpolación de naves:**
```java
// Suaviza el movimiento de naves remotas
position.x = position.x * 0.7 + newX * 0.3;  // 70% actual + 30% nuevo
position.y = position.y * 0.7 + newY * 0.3;
```

Esto compensa la latencia de red (hasta 100ms) y proporciona movimiento fluido.

### 🔍 Indicadores de Red

**Panel de red (esquina inferior derecha):**
```
RED P2P
2 naves remotas ●
```
- Solo visible cuando hay peers conectados
- Indicador verde pulsante = conexión activa
- Contador actualizado en tiempo real

**Consola de depuración:**
```
Sistema P2P inicializado en puerto 8888
NetworkController iniciado: Peer_8888_1708415234567
Nuevo peer conectado: Peer[Peer_8889_... @ 192.168.1.10:8889]
Nueva nave remota añadida: LocalShip_1708415236789
```

### 📊 Tráfico de Red Estimado

- **Por nave**: ~1.5 KB/s
- **4 jugadores**: ~6 KB/s total
- **Latencia típica**: < 20ms en LAN, < 50ms en WiFi local

### 🔒 Notas de Seguridad

⚠️ **Este sistema es para redes locales confiables solamente**. No incluye cifrado ni autenticación. No lo uses en redes públicas.

## 🏗️ Arquitectura del Proyecto

El proyecto sigue **MVC estricto** con capa de servicios separada. Todos los métodos que necesitan el tiempo actual lo reciben como parámetro (`long currentTime`) — no se llama `System.currentTimeMillis()` dentro del modelo.

```
src/
├── GameMain.java                    # Punto de entrada, configura puerto P2P e inyecta initTime
├── controller/
│   ├── GameController.java          # Controlador principal: game loop, input, integración P2P
│   │                                # Usa Swing Timer para nextLevel (no java.util.Timer)
│   ├── GameLogic.java               # ★ NUEVO: Toda la lógica de negocio extraída del modelo
│   │                                #   checkPackagePickup/Delivery, shootProjectile,
│   │                                #   checkProjectileCollision, nextLevel, restartGame,
│   │                                #   spawnAsteroidsOverTime, splitAsteroid
│   └── NetworkController.java       # Comunicación UDP multicast (3 hilos: main/receiver/heartbeat)
├── model/
│   ├── IGameModel.java              # ★ NUEVO: Interfaz de solo lectura para la Vista
│   │                                #   getPlayerShip(), getPlanets(), getRemainingTime(long),
│   │                                #   getRemoteShips(), getExplosionPositions/Sizes(), etc.
│   ├── GameModel.java               # Estado puro del juego (sin lógica, sin timers internos)
│   │                                #   Constructor: GameModel(width, height, initTime)
│   │                                #   getRemainingTime(long) en vez de campo calculado
│   ├── Ship.java                    # Nave espacial; isImmune(long), shoot(long), canShoot(long)
│   ├── Asteroid.java                # Asteroides con tipos SMALL/MEDIUM/LARGE/CHASER
│   ├── Planet.java                  # Planetas destino
│   ├── Package.java                 # Paquetes NORMAL/URGENT/HEAVY
│   ├── Projectile.java              # Proyectiles de la nave
│   ├── FloatingText.java            # Textos flotantes (puntos, mensajes)
│   ├── LevelObjective.java          # Objetivos de misión
│   └── network/
│       ├── PeerInfo.java            # Información de peers
│       └── NetworkMessage.java      # Mensajes P2P serializables
├── services/
│   ├── PhysicsService.java          # Movimiento, colisiones, wraparound, boost
│   │                                #   updateShipPhysics, applyThrust, applyRotation,
│   │                                #   applyBrake, updateBoost, checkShip*Collision
│   ├── LevelGenerator.java          # Generación procedural de niveles
│   │                                #   WORLD GENERATOR (líneas 69-210): planetas con patrones
│   │                                #   LIFE GENERATOR  (líneas 215-280): cinturones de asteroides
│   ├── CollisionService.java        # Detección de colisiones de alto nivel
│   ├── CameraService.java           # Cámara y viewport
│   ├── LevelService.java            # Lógica de nivel y objetivos
│   ├── ScoreService.java            # Puntuación y combos
│   ├── SpawnService.java            # Spawn de paquetes y asteroides
│   └── GameData.java                # Contenedor de datos de nivel generado
└── view/
    ├── GameView.java                # Renderizado principal + sprites; implementa IGameModel
    ├── Viewer.java                  # Lógica de cámara y viewport
    └── ControlPanel.java            # Panel de controles (legacy)

resources/
└── sprites/
    ├── explosion/
    │   └── Explosion Animation.png  # Tira horizontal 1×11, 704×64px (11 frames de 64×64px)
    ├── ship/
    │   └── ship_red.png             # Sprite de nave jugador (escalado al 60%)
    ├── trail/
    │   └── Group 4 - 4.png          # Tira horizontal 1×8, 256×48px (8 frames de 32×48px)
    ├── asteroid/
    │   ├── meteorGrey_big{1-4}.png  # 4 variantes de asteroide grande
    │   ├── meteorGrey_med{1-2}.png  # 2 variantes de asteroide mediano
    │   └── meteorGrey_small{1-2}.png# 2 variantes de asteroide pequeño
    ├── projectile/
    │   └── laserBlue14.png          # Sprite de proyectil
    └── background/
        └── Space01.png              # Fondo espacial (tiling con parallax 0.5×)
```

### Principios MVC aplicados

| Principio | Implementación |
|---|---|
| **Modelo sin tiempo** | Todos los métodos reciben `long currentTime` como parámetro |
| **Modelo sin timers** | `java.util.Timer` eliminado; `nextLevel` usa `Swing Timer` en controlador |
| **Vista sin lógica** | `GameView` implementa `IGameModel` (interfaz de solo lectura) |
| **Lógica separada** | `GameLogic.java` contiene toda la lógica de negocio extraída de `GameModel` |
| **Física separada** | `PhysicsService` maneja todo el movimiento y colisiones |

## 🛠️ Compilación y Ejecución

### Requisitos
- **Java JDK 21** o superior
- Sistema operativo: Windows/Linux/macOS
- Red local para modo multijugador

### Compilar
```bash
cd "c:\Users\busca\OneDrive\Desktop\Bolas v.1 - copia"
javac -d bin -sourcepath src src\GameMain.java src\controller\*.java src\model\*.java src\model\network\*.java src\services\*.java src\view\*.java
```

### Ejecutar (Un jugador)
```bash
java -cp bin GameMain
```

### Ejecutar (Multijugador)
```bash
# Jugador 1
java -cp bin GameMain 8888

# Jugador 2 (otra terminal/PC)
java -cp bin GameMain 8889
```

## 🎯 Sistema de Puntuación

### Puntos por Asteroides
- **Pequeño**: 10 puntos
- **Mediano**: 25 puntos
- **Grande**: 50 puntos

### Puntos por Entregas
- **Paquete Normal**: Base × combo
- **Paquete Urgente**: Base × 1.5 × combo
- **Paquete Heavy**: Base × 1.3 × combo

### Sistema de Combos
- **Combo**: Se multiplica por entregas consecutivas
- **Tiempo de combo**: 10 segundos entre entregas
- **Máximo combo**: Sin límite teórico

## 🎮 Características del Juego

### Sistema de Vidas
- **Inicial**: 3 vidas
- **Máximo**: 5 vidas
- **Inmunidad**: 2 segundos tras recibir daño
- **Indicador visual**: Escudo amarillo semitransparente

### Sistema de Boost
- **Capacidad**: 100%
- **Consumo**: 10% por segundo al usar
- **Recarga**: 20% por segundo sin usar
- **Multiplicador**: x2.5 velocidad

### Sistema de Tiempo
- **Nivel 1**: 240 segundos (4 minutos)
- **Niveles 2-5**: Tiempo reducido progresivamente
- **Bonus por entrega**: +15 segundos
- **Crítico**: < 30 segundos (indicador rojo pulsante)

### Físicas del Juego
- **Velocidad máxima**: 8.0 unidades/frame
- **Aceleración**: 0.35 unidades/frame²
- **Fricción**: 96% (inercia espacial)
- **Rotación**: 0.08 radianes/frame
- **Freno de emergencia**: Reduce 7% por frame

## 📚 Documentación Adicional

- **[README_P2P.md](README_P2P.md)**: Documentación completa del sistema P2P
- **[RESUMEN_P2P.md](RESUMEN_P2P.md)**: Resumen ejecutivo de la implementación P2P

## 🐛 Troubleshooting

### Problema: No se cargan los sprites
**Solución**: Asegúrate de que la carpeta `resources/sprites/` esté en el mismo directorio que `bin/`

### Problema: No se detectan peers en P2P
**Solución**: 
- Verifica que todas las instancias estén en la misma red local
- Revisa el firewall (puede bloquear multicast)
- Asegúrate de usar puertos diferentes por instancia

### Problema: Asteroides aparecen encima de la nave
**Solución**: Este bug está corregido - hay protección de spawn de 200px

### Problema: Lag en movimiento de naves remotas
**Solución**: Normal en WiFi con latencia > 50ms. La interpolación compensa hasta 100ms.

## 📝 Historial de Cambios

### Refactoring MVC (commit `08bee12`)

**Problema → Solución:**

| # | Problema detectado | Solución aplicada |
|---|---|---|
| 1 | `GameModel` contenía lógica de negocio | Extraída a `GameLogic.java` (controller layer) |
| 2 | `System.currentTimeMillis()` dentro del modelo (10+ llamadas) | Eliminadas, tiempo inyectado como `long currentTime` en todos los métodos |
| 3 | `java.util.Timer` en modelo para `nextLevel` | Reemplazado por `Swing Timer` en `GameController` |
| 4 | Acoplamiento fuerte Vista→Modelo | Añadida interfaz `IGameModel` (solo lectura) que `GameView` consume |
| 5 | Vista accedía a `GameModel` directamente | `GameView(IGameModel model)` — depende solo de la interfaz |

**Archivos nuevos:**
- `src/model/IGameModel.java` — contrato de solo lectura para la Vista
- `src/controller/GameLogic.java` — toda la lógica de negocio (600+ líneas extraídas)

**Archivos modificados:**
- `src/model/GameModel.java` — reducido de 895 → 425 líneas (estado puro)
- `src/controller/GameController.java` — usa `logic.*`, Swing Timer, `clearExplosions()` tras cada repaint
- `src/view/GameView.java` — acepta `IGameModel`, usa `getExplosionPositions/Sizes()` (snapshot)
- `src/GameMain.java` — `new GameModel(800, 600, initTime)` inyectando tiempo inicial

---

## 🎉 Créditos y Licencia

Desarrollo: Proyecto académico de arquitectura MVC en Java
Sprites: Recursos de sprite sheets genéricos
Sistema P2P: Implementación original con UDP multicast

---

**¡Disfruta jugando y destruyendo asteroides en el espacio!** 🚀💥

Para preguntas sobre el sistema P2P, consulta [README_P2P.md](README_P2P.md)
