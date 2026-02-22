# 🚀 GUION DE EXPOSICIÓN — MENSAJERO INTERGALÁCTICO

> **Nota para el presentador:** Este guion cubre todos los aspectos técnicos del proyecto. Lee en voz alta las partes en *cursiva* y señala los archivos/líneas indicadas en el IDE o proyector.

---

## 0. INTRODUCCIÓN (1 minuto)

*"Buenos días/tardes. Voy a presentar **Mensajero Intergaláctico**, un videojuego espacial arcade 2D desarrollado en Java. El juego sigue el patrón MVC —Modelo, Vista, Controlador— y tiene soporte multijugador en red usando tecnología P2P.*

*El jugador pilota una nave espacial, recoge paquetes y los entrega a planetas, mientras esquiva asteroides. Varios jugadores pueden jugar simultáneamente en la misma red local gracias al sistema Peer-to-Peer que implementamos."*

**Archivos clave del proyecto:**
```
src/
├── GameMain.java               ← Punto de entrada
├── controller/
│   ├── GameController.java     ← Controlador principal + input
│   ├── GameLogic.java          ← Lógica de negocio
│   └── NetworkController.java  ← Sistema P2P (UDP multicast)
├── model/
│   ├── Ship.java               ← Modelo de la nave
│   ├── Asteroid.java           ← Modelo de asteroide
│   ├── Package.java            ← Modelo de paquete
│   ├── Planet.java             ← Modelo de planeta
│   ├── Projectile.java         ← Modelo de proyectil
│   ├── generators/
│   │   └── LevelGenerator.java ← WorldGenerator + LifeGenerator
│   └── network/
│       ├── NetworkMessage.java ← Protocolo de mensajes P2P
│       └── PeerInfo.java       ← Datos de un peer
├── services/
│   ├── PhysicsService.java     ← Física: movimiento, colisiones
│   └── (otros servicios)
└── view/
    └── GameView.java           ← Renderizado + Sprite Sheets + HUD
resources/sprites/
├── ship/ship_red.png           ← Sprite de la nave
├── trail/Group 4 - 4.png      ← Sprite sheet del trail (256×48, 8 frames)
├── explosion/Explosion Animation.png ← Sprite sheet explosión (704×64, 11 frames)
├── asteroid/meteorGrey_*.png  ← Sprites de asteroides
├── projectile/laserBlue14.png ← Sprite del proyectil
└── background/Space01.png     ← Imagen de fondo espacial
```

---

## 1. SPRITE SHEETS: EL TRAIL DE FUEGO (2 minutos)

*"Empecemos por algo visual: el rastro de fuego que sale de la nave. Este efecto usa una técnica llamada **sprite sheet**: en lugar de varias imágenes sueltas, toda la animación está en **una sola imagen** con los frames uno al lado del otro."*

**Señalar archivo:** `resources/sprites/trail/Group 4 - 4.png`

*"El archivo de trail mide **256×48 píxeles** y contiene **8 frames** de 32×48 píxeles cada uno, organizados horizontalmente en una tira."*

**Señalar código en `GameView.java`, método `loadSprites()`, líneas 147–154:**
```java
BufferedImage trailSheet = ImageIO.read(new File("resources/sprites/trail/Group 4 - 4.png"));
if (trailSheet != null) {
    fireTrailFrames = new BufferedImage[8];
    int frameWidth = trailSheet.getWidth() / 8;  // 256 ÷ 8 = 32px por frame
    for (int i = 0; i < 8; i++) {
        // Extrae cada frame de la tira horizontal
        fireTrailFrames[i] = trailSheet.getSubimage(i * frameWidth, 0, frameWidth, trailSheet.getHeight());
    }
}
```

*"Con `getSubimage(i * 32, 0, 32, 48)` cortamos cada frame: el frame 0 empieza en x=0, el frame 1 en x=32, el frame 2 en x=64... y así hasta el frame 7 en x=224. Java nos devuelve 8 imágenes independientes que luego animamos cíclicamente."*

### Cómo el trail se alarga según la velocidad

*"La característica más interesante del trail es que **cambia de tamaño dependiendo de la velocidad de la nave**. A más velocidad, más grande el fuego."*

**Señalar código en `GameView.java`, método `drawShipTrail()`, líneas 568–619:**
```java
// Calcular velocidad para escalar el trail
Point2D.Double vel = ship.getVelocity();
double speed = Math.sqrt(vel.x * vel.x + vel.y * vel.y);  // Módulo del vector velocidad
double maxSpeed = 8.0;

// Escalar tamaño basado en velocidad (0.3 a 1.0)
double speedRatio = Math.min(speed / maxSpeed, 1.0);
double scaleFactor = 0.3 + (speedRatio * 0.7); // De 0.3 a 1.0
if (speedRatio < 0.05) return; // No mostrar trail si casi parado
```

*"Se calcula la **magnitud del vector velocidad** con la fórmula de Pitágoras: raíz de (vx² + vy²). Luego se divide entre la velocidad máxima (8.0) para obtener un ratio de 0 a 1. Con ese ratio calculamos un `scaleFactor` que va de 0.3 (casi parado) a 1.0 (velocidad máxima).*

*Finalmente, ese factor escala el tamaño del sprite al dibujarlo:"*

```java
int w = (int)(trailSprite.getWidth() * scaleFactor);
int h = (int)(trailSprite.getHeight() * scaleFactor);
g2d.drawImage(trailSprite, -w/2, -h/2, w, h, null);
```

*"La animación avanza cada 50 ms cambiando `trailFrame` al siguiente (de 0 a 7 en bucle), y el sprite se rota para seguir exactamente la orientación de la nave."*

---

## 2. SPRITE SHEET DE LA EXPLOSIÓN (2 minutos)

*"Las explosiones usan el mismo concepto de sprite sheet. El archivo `Explosion Animation.png` mide **704×64 píxeles** y contiene **11 frames** de 64×64 píxeles cada uno."*

**Señalar archivo:** `resources/sprites/explosion/Explosion Animation.png`

**Señalar clase `ExplosionAnimation` en `GameView.java`, líneas 72–95:**
```java
private static class ExplosionAnimation {
    double x, y;
    long startTime;
    double scale;
    static final int TOTAL_FRAMES = 11;
    static final long FRAME_DURATION = 60; // ms por frame

    int getCurrentFrame() {
        long elapsed = System.currentTimeMillis() - startTime;
        return (int)(elapsed / FRAME_DURATION);
    }
}
```

*"Cada explosión recuerda el instante en que empezó (`startTime`). Para saber qué frame mostrar, divide el tiempo transcurrido entre la duración de cada frame (60 ms). Así el frame avanza automáticamente con el tiempo real."*

**Señalar código en `drawExplosions()`, líneas 673–688:**
```java
int srcX = frame * 64;  // Offset horizontal en la tira (0, 64, 128...)
BufferedImage frameImg = explosionSheet.getSubimage(srcX, 0, 64, 64);

// Tamaño adaptado al meteorito
int size = (int)(64 * exp.scale);
g2d.drawImage(frameImg, x - size/2, y - size/2, size, size, null);
```

*"La escala de la explosión es proporcional al radio del asteroide que la originó: `scale = radius / 25.0`. Un asteroide pequeño (radio 15) produce una explosión de escala 0.6; uno grande (radio 40) produce una de escala 1.6."*

---

## 3. MOVIMIENTO DE LA NAVE (2 minutos)

### Entrada del jugador

*"Los controles están en `GameController.java`. Las teclas se registran con dos métodos del KeyListener:"*

**Señalar `GameController.java`, métodos `keyPressed` y `keyReleased`, líneas 292–363:**
```java
if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) leftPressed = true;
if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) rightPressed = true;
if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) upPressed = true;
if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) emergencyBrakePressed = true;
if (key == KeyEvent.VK_SHIFT) boostPressed = true;
if (key == KeyEvent.VK_SPACE) shootPressed = true;
```

*"Guardamos el estado de cada tecla (pulsada o no). En cada frame del juego llamamos a `processInput()` que aplica la física correspondiente."*

### Física del movimiento

**Señalar `PhysicsService.java`, método `applyThrust()`, líneas 50–69:**
```java
public void applyThrust(Ship ship, double thrustValue, boolean boostActive) {
    double power = 0.35 * thrustValue;
    if (boostActive) {
        power = 0.35 * 2.5; // Boost: fuerza x2.5
    }

    double angle = ship.getRotationAngle();
    vel.x += Math.cos(angle) * power;  // Componente X del empuje
    vel.y += Math.sin(angle) * power;  // Componente Y del empuje

    limitSpeed(ship, boostActive);
}
```

*"La nave usa física newtoniana: `W` aplica un empuje en la dirección de la rotación usando seno y coseno. La velocidad se acumula y existe inercia."*

**Señalar `PhysicsService.java`, método `updateShipPhysics()`, líneas 26–45:**
```java
// Aplicar fricción (rozamiento del espacio)
vel.x *= 0.96;
vel.y *= 0.96;

// Actualizar posición
pos.x += vel.x;
pos.y += vel.y;
```

*"Cada frame se aplica una fricción del 4% (multiplicar por 0.96) que simula que la nave frena gradualmente si no se pulsa nada. Luego se suma la velocidad a la posición para mover la nave."*

### Rotación

**Señalar `PhysicsService.java`, método `applyRotation()`, líneas 74–78:**
```java
public void applyRotation(Ship ship, double direction) {
    double newAngle = ship.getRotationAngle() + direction * 0.08;
    ship.setRotationAngle(newAngle);
}
```

*"La rotación aumenta el ángulo en radianes 0.08 por frame (aproximadamente 4.6°/frame). A/← rota en sentido antihorario (direction=-1) y D/→ en sentido horario (direction=+1)."*

---

## 4. WRAPAROUND: ATRAVESAR LOS BORDES DEL MAPA (1 minuto)

*"Una mecánica clásica de los juegos arcade espaciales: cuando la nave sale por un borde, aparece en el lado opuesto."*

**Señalar `PhysicsService.java`, método `updateShipPhysics()`, líneas 41–44:**
```java
// Wraparound: si sale por la derecha aparece por la izquierda, etc.
if (pos.x < 0) pos.x += MAP_WIDTH;
if (pos.x > MAP_WIDTH) pos.x -= MAP_WIDTH;
if (pos.y < 0) pos.y += MAP_HEIGHT;
if (pos.y > MAP_HEIGHT) pos.y -= MAP_HEIGHT;
```

*"El mapa mide 2400×1800 píxeles. Si la posición X de la nave cae por debajo de 0 (salió por la izquierda), le sumamos el ancho completo del mapa para llevarla al lado derecho. Y viceversa. Lo mismo en el eje Y. Los asteroides también tienen este comportamiento en `updateAsteroidPhysics()`."*

---

## 5. DISPAROS DE LA NAVE (2 minutos)

### Creación del proyectil

**Señalar `Ship.java`, método `shoot()`, líneas 244–253:**
```java
public Projectile shoot(long currentTime) {
    if (currentTime - lastShotTime >= SHOT_COOLDOWN_MS) { // Cooldown: 300ms
        lastShotTime = currentTime;
        // Crear proyectil en la punta de la nave
        double tipX = position.x + Math.cos(rotationAngle) * 15;
        double tipY = position.y + Math.sin(rotationAngle) * 15;
        return new Projectile(tipX, tipY, rotationAngle);
    }
    return null;
}
```

*"Al disparar, el proyectil aparece **15 píxeles por delante** de la nave (en la dirección en que apunta) usando seno y coseno del ángulo. El cooldown de 300 ms limita la cadencia a ~3 disparos por segundo."*

### Velocidad y movimiento del proyectil

**Señalar `Projectile.java`, constructor, líneas 21–28:**
```java
public Projectile(double startX, double startY, double angle) {
    this.velX = Math.cos(angle) * SPEED;  // SPEED = 12.0
    this.velY = Math.sin(angle) * SPEED;
    this.color = PROJECTILE_COLOR;        // Cyan brillante
}
```

*"El proyectil viaja a velocidad 12 (la nave va a 8 como máximo), así siempre va más rápido que la nave. Se mueve cada frame en `PhysicsService.updateProjectilePhysics()` y se desactiva si sale del mapa."*

### Renderizado del proyectil

**Señalar `GameView.java`, método `drawProjectiles()`, líneas 621–647:**
```java
AffineTransform oldTransform = g2d.getTransform();
g2d.translate(x, y);
g2d.rotate(proj.getAngle() + Math.PI / 2); // Compensar +90° para que apunte bien
int w = (int)(sprite.getWidth() * 0.75);
int h = (int)(sprite.getHeight() * 0.75);
g2d.drawImage(sprite, -w/2, -h/2, w, h, null);
g2d.setTransform(oldTransform);
```

*"El sprite `laserBlue14.png` se rota para alinearse con la dirección del disparo."*

---

## 6. LIFE GENERATOR: GENERACIÓN DE ASTEROIDES (2 minutos)

*"Los asteroides son la 'vida' del nivel, el peligro constante. Se generan en `LevelGenerator.java` con el método `generateAsteroids()`, líneas 235–285."*

**Señalar `LevelGenerator.java`, líneas 235–270:**
```java
private List<model.Asteroid> generateAsteroids(int count, int level) {
    // Velocidad base incrementa con nivel
    double baseSpeed = 1.0 + (level * 0.2);

    // Crear 2-3 cinturones de asteroides
    int beltCount = 2 + (level > 5 ? 1 : 0);
    int asteroidsPerBelt = count / beltCount;

    for (int belt = 0; belt < beltCount; belt++) {
        // Centro del cinturón (posición aleatoria en el mapa)
        double beltCenterX = CENTER_X + (random.nextDouble() - 0.5) * MAP_WIDTH * 0.8;
        double beltCenterY = CENTER_Y + (random.nextDouble() - 0.5) * MAP_HEIGHT * 0.8;
        double beltSpread = 200 + belt * 100;  // Radio de dispersión

        for (int i = 0; i < asteroidsPerBelt; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * beltSpread;
            double x = beltCenterX + Math.cos(angle) * distance;
            double y = beltCenterY + Math.sin(angle) * distance;

            // Velocidad: dirección y magnitud aleatorias
            double velX = (random.nextDouble() - 0.5) * baseSpeed * 2;
            double velY = (random.nextDouble() - 0.5) * baseSpeed * 2;

            model.Asteroid.AsteroidType type = determineAsteroidType(level);
            asteroids.add(new model.Asteroid(x, y, velX, velY, type));
        }
    }
}
```

*"Los asteroides se agrupan en **cinturones**: 2 cinturones en niveles bajos, 3 en niveles 6+. Cada cinturón tiene un centro aleatorio y los asteroides se distribuyen dentro de un radio de dispersión. La velocidad se genera restando 0.5 a un número aleatorio entre 0 y 1, lo que da valores entre -1 y +1 en cada eje, luego multiplicados por `baseSpeed`. Esto produce direcciones completamente aleatorias."*

### Tipos de asteroide según nivel

**Señalar `LevelGenerator.java`, método `determineAsteroidType()`, líneas 291–303:**
```java
private model.Asteroid.AsteroidType determineAsteroidType(int level) {
    double roll = random.nextDouble();
    if (level >= 7 && roll < 0.1) return AsteroidType.CHASER; // 10% perseguidores
    else if (level >= 4 && roll < 0.3) return AsteroidType.LARGE; // 30% grandes
    else if (level >= 3 && roll < 0.5) return AsteroidType.MEDIUM; // 20% medianos
    else return AsteroidType.SMALL; // 50-100% pequeños
}
```

*"La dificultad escala con el nivel: en los primeros niveles solo hay asteroides pequeños. A partir del nivel 3 aparecen medianos, nivel 4 grandes, y nivel 7 los 'CHASER' que persiguen activamente a la nave."*

---

## 7. WORLD GENERATOR: GENERACIÓN DE PLANETAS (2 minutos)

*"Los planetas son los destinos donde el jugador entrega paquetes. Se generan con el WorldGenerator dentro de `LevelGenerator.java`."*

**Señalar `LevelGenerator.java`, comentario líneas 71–76 y método `generatePlanets()`, líneas 82–103:**

*"El WorldGenerator tiene 4 patrones diferentes que rotan según el nivel usando `currentLevel % 4`:"*

```java
int pattern = currentLevel % 4;
switch (pattern) {
    case 0: return generateRingPattern(count);    // Anillos concéntricos
    case 1: return generateSpiralPattern(count);  // Espiral galáctica
    case 2: return generateCrossPattern(count);   // Cruz de 4 brazos
    case 3: return generateScatteredPattern(count); // Distribución orgánica
}
```

### ¿Por qué la composición es siempre la misma pero los planetas cambian de lugar?

*"Esta es una pregunta clave de diseño. Dentro de cada patrón, la posición de cada planeta sí es **determinista** por el índice (ej: anillo en ángulo `2πi/count`), pero hay un factor `random.nextDouble() * 0.3` de variación y el radio varía con `random.nextInt(21)`. Además, los **colores y nombres** provienen de arrays predefinidos asignados por índice:"*

**Señalar `LevelGenerator.java`, líneas 28–44 y dentro de `generateRingPattern()`, líneas 120–125:**
```java
// Arrays fijos de nombres y colores
private static final String[] PLANET_NAMES = {
    "Aridia", "Cryos", "Pyros", "Verdant", "Aquaris", ...
};
private static final Color[] PLANET_COLORS = {
    new Color(200, 50, 50),   // Rojo (Aridia)
    new Color(100, 150, 255), // Azul (Cryos)
    ...
};

// Asignación: cada planeta i usa el índice i % 10
int styleIndex = i % PLANET_NAMES.length;
String name = PLANET_NAMES[styleIndex];
Color color = PLANET_COLORS[styleIndex];
int planetRadius = 20 + random.nextInt(21); // Radio aleatorio 20-40
```

*"La composición cambia porque el **patrón** (anillo/espiral/cruz/disperso) cambia cada 4 niveles. Dentro del mismo patrón, los ángulos base son fijos para cada planeta, pero el radio tiene variación aleatoria. Los nombres y colores siempre van en el mismo orden (Aridia=rojo, Cryos=azul...) pero su posición en el mapa varía con el patrón."*

---

## 8. GENERACIÓN DE PAQUETES (1.5 minutos)

*"Los paquetes son el objetivo principal del juego: recogerlos y llevarlos a su planeta destino."*

**Señalar `LevelGenerator.java`, método `generatePackages()`, líneas 308–343:**
```java
private List<model.Package> generatePackages(int count, List<model.Planet> planets) {
    for (int i = 0; i < count; i++) {
        // Planeta destino: completamente aleatorio
        model.Planet targetPlanet = planets.get(random.nextInt(planets.size()));

        // Posición: 50% cerca del centro (jugador), 50% cerca de un planeta
        double x, y;
        if (random.nextDouble() < 0.5) {
            // Cerca del centro (zona inicial del jugador)
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 100 + random.nextDouble() * 200;
            x = CENTER_X + Math.cos(angle) * distance;
            y = CENTER_Y + Math.sin(angle) * distance;
        } else {
            // Cerca de un planeta aleatorio
            model.Planet nearPlanet = planets.get(random.nextInt(planets.size()));
            double distance = 80 + random.nextDouble() * 100;
            x = nearPlanet.getX() + Math.cos(angle) * distance;
            y = nearPlanet.getY() + Math.sin(angle) * distance;
        }

        model.Package.PackageType type = determinePackageType();
        packages.add(new model.Package(x, y, targetPlanet, type, currentTime));
    }
}
```

### Tipos de paquete y sus probabilidades

**Señalar `determinePackageType()`, líneas 349–360:**
```java
if (roll < 0.05) return PackageType.SPECIAL;  // 5%  → +1 vida
else if (roll < 0.30) return PackageType.URGENT;  // 25% → caduca en 30s
else if (roll < 0.45) return PackageType.HEAVY;   // 15% → reduce velocidad
else return PackageType.NORMAL;                    // 55% → normal
```

*"Los paquetes se representan como **cuadrados de colores** en pantalla: rojo para urgentes, naranja/marrón para pesados, y del color del planeta destino para los normales, haciendo intuitivo saber dónde llevarlos."*

---

## 9. FONDO ESPACIAL Y EFECTO PARALLAX (1.5 minutos)

*"El fondo del juego tiene tres capas para crear profundidad visual."*

**Señalar `GameView.java`, método `paintComponent()`, líneas 186–213:**

### Capa 1: Imagen de fondo con tiling

```java
// La imagen se repite en mosaico para cubrir el mundo entero
int bgOffsetX = (int)(camX * 0.5); // Se mueve a la MITAD de velocidad (parallax)
int bgOffsetY = (int)(camY * 0.5);

// Calcular cuántos tiles necesitamos para cubrir la pantalla
for (int tileY = startTileY; tileY <= endTileY; tileY++) {
    for (int tileX = startTileX; tileX <= endTileX; tileX++) {
        g2d.drawImage(backgroundImage, tileX * bgW - bgOffsetX, tileY * bgH - bgOffsetY, null);
    }
}
```

*"El fondo se mueve al **50% de la velocidad** de la cámara, creando el efecto de que está más lejos."*

### Capa 2: Campo de estrellas procedural con parallax

**Señalar `GameView.java`, métodos `initStars()` y `drawStarfield()`, líneas 163–258:**
```java
// Inicialización: 300 estrellas en posiciones aleatorias con 2 capas
for (int i = 0; i < 300; i++) {
    stars.add(new Star(x, y, size, random.nextInt(2))); // layer 0 o 1
}

// Dibujo: cada capa se mueve a velocidad diferente
double parallax = star.layer == 0 ? 0.3 : 0.6; // Capa 0: lenta, Capa 1: rápida
int x = (int)(star.x + camX * parallax);
int y = (int)(star.y + camY * parallax);
```

*"Las estrellas de capa 0 se mueven al 30% de la velocidad de la cámara (parecen muy lejanas), las de capa 1 al 60% (más cercanas). Esto crea la ilusión de profundidad 3D."*

---

## 10. SISTEMA P2P (PEER-TO-PEER) (3 minutos)

*"El sistema P2P permite que varios jugadores vean sus naves en la misma partida. Está implementado con **UDP multicast**, lo que significa que no hace falta un servidor central: todos los jugadores se comunican entre sí directamente."*

### Arquitectura P2P

```
Jugador A (puerto 8888)          Jugador B (puerto 8889)
     |                                    |
     └───── UDP Multicast 230.0.0.1:4446 ─┘
              (todos escuchan y envían)
```

### Configuración de red

**Señalar `NetworkController.java`, constantes, líneas 19–21:**
```java
private static final String MULTICAST_ADDRESS = "230.0.0.1"; // Dirección de grupo
private static final int MULTICAST_PORT = 4446;               // Puerto compartido
private static final int PEER_TIMEOUT_MS = 30000;            // 30s sin señal = desconectado
```

### Inicio y unión al grupo multicast

**Señalar `NetworkController.java`, método `start()`, líneas 60–83:**
```java
public void start() throws IOException {
    socket = new MulticastSocket(MULTICAST_PORT); // Escuchar en puerto 4446
    group = InetAddress.getByName(MULTICAST_ADDRESS);

    // Unirse al grupo multicast (recibir todos los mensajes del grupo)
    SocketAddress groupAddress = new InetSocketAddress(group, MULTICAST_PORT);
    NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
    socket.joinGroup(groupAddress, netIf);

    startReceiverThread(); // Thread receptor de mensajes
    startHeartbeatThread(); // Thread de latidos periódicos
    announcePresence();    // Anunciar que estamos aquí
}
```

*"Al iniciar, el juego crea un socket UDP y se une al grupo multicast 230.0.0.1. Cualquier mensaje enviado a ese grupo lo reciben **todos** los que estén en la misma red local."*

### Tipos de mensajes

**Señalar `NetworkMessage.java`, enum `MessageType`, líneas 13–17:**
```java
public enum MessageType {
    PEER_ANNOUNCE,  // "Hola, estoy aquí" (cada 5 segundos)
    SHIP_UPDATE,    // Posición y estado de mi nave (cada 100ms)
    PEER_LEAVE      // "Me desconecto"
}
```

### Protocolo de mensajes (serialización)

**Señalar `NetworkMessage.java`, métodos `serialize()` y `deserialize()`, líneas 84–125:**
```java
// Formato: TYPE|senderId|key1:value1|key2:value2|...
// Ejemplo: SHIP_UPDATE|Peer_8888_123456|posX:1200|posY:900|velX:3.5|velY:-1.2|angle:1.57

public String serialize() {
    StringBuilder sb = new StringBuilder();
    sb.append(type.name()).append("|").append(senderId);
    for (Map.Entry<String, String> entry : data.entrySet()) {
        sb.append("|").append(entry.getKey()).append(":").append(entry.getValue());
    }
    return sb.toString();
}
```

*"Los mensajes son texto plano separado por `|`. Un mensaje típico de actualización de nave incluye: tipo de mensaje, ID del remitente, posición X/Y, velocidad X/Y, ángulo, vidas y si lleva paquete."*

### Actualización de nave: envío (cada 100ms)

**Señalar `GameController.java`, método `sendNetworkUpdate()`, líneas 415–447:**
```java
private void sendNetworkUpdate() {
    // Throttle: solo enviamos cada 100ms (10 veces/segundo)
    long now = System.currentTimeMillis();
    if (now - lastNetworkUpdate < NETWORK_UPDATE_INTERVAL_MS) return;

    NetworkMessage.ShipData shipData = new NetworkMessage.ShipData(
        localShip.getShipId(),
        localShip.getX(), localShip.getY(),
        localShip.getVelX(), localShip.getVelY(),
        localShip.getRotationAngle(),
        networkController.getLocalPeerId(),
        localShip.getLives(), localShip.hasPackage()
    );
    networkController.sendShipUpdate(shipData);
}
```

### Recepción y actualización con interpolación

**Señalar `Ship.java`, método `updateFromNetwork()`, líneas 306–320:**
```java
public void updateFromNetwork(double x, double y, double vx, double vy, double angle, ...) {
    if (!isRemote) return; // Solo para naves de otros jugadores

    // Interpolación suave para compensar latencia de red
    position.x = position.x * 0.7 + x * 0.3;
    position.y = position.y * 0.7 + y * 0.3;
    velocity.x = vx;
    velocity.y = vy;
    rotationAngle = angle;
}
```

*"Para evitar tirones bruscos por la latencia de red, aplicamos **interpolación**: la nueva posición se calcula como 70% de la posición actual + 30% de la posición recibida. Esto suaviza los saltos cuando llegan actualizaciones."*

### Identificación de peers

**Señalar `PeerInfo.java`, clase completa:**
*"Cada jugador tiene un `peerId` único generado al iniciar: `'Peer_' + puerto + '_' + timestamp`. Si no se recibe ningún mensaje de un peer en 30 segundos, se considera que se desconectó y se elimina su nave del juego."*

### Diferenciación visual de naves remotas

**Señalar `Ship.java`, método `generateTintColor()`, líneas 75–92:**
```java
private static Color generateTintColor(String ownerId) {
    int hash = ownerId.hashCode(); // Hash determinista del ID
    Color[] tints = {
        new Color(255, 100, 100),  // Rojo
        new Color(100, 150, 255),  // Azul
        new Color(100, 255, 100),  // Verde
        ...
    };
    return tints[Math.abs(hash) % tints.length];
}
```

*"Cada nave remota recibe un color único y consistente basado en el hash de su ID. Así el mismo jugador siempre aparece del mismo color en todas las instancias."*

---

## 11. INTERFAZ DE USUARIO (HUD) (1.5 minutos)

*"La interfaz superpone información importante sin tapar el juego. Está dibujada directamente sobre el panel en `GameView.java`, método `drawHUD()`."*

**Señalar `GameView.java`, método `drawHUD()`, líneas 709–738:**

### Elementos del HUD:

1. **Panel superior izquierdo** — Vidas y puntuación
   ```java
   g2d.drawString("VIDAS: " + ship.getLives(), 25, 35);
   g2d.drawString("Puntos: " + model.getTotalScore(), 25, 60);
   ```

2. **Panel central superior** — Temporizador del nivel (4 minutos)
   - Pulsa en rojo con efecto de latido cuando quedan menos de 30 segundos

3. **Panel inferior izquierdo** — Barra de boost con gradiente de color
   - Azul cuando hay boost suficiente, naranja cuando está bajo

4. **Panel superior derecho** — Objetivos del nivel: lista de planetas con paquetes pendientes

5. **Flecha indicadora** — Aparece cuando llevas un paquete, apunta al planeta destino
   ```java
   double angle = Math.atan2(dy, dx); // Ángulo hacia el planeta
   g2d.translate(arrowX, arrowY);
   g2d.rotate(angle);
   g2d.fillPolygon(xPoints, yPoints, 3); // Triángulo apuntando al destino
   ```

6. **Panel inferior derecho** — Estado de red P2P (solo si hay peers conectados)

7. **Pantalla Game Over/Victoria** — Overlay oscuro semi-transparente con puntuación final

---

## 12. PUNTO DE ENTRADA Y ARQUITECTURA MVC (1 minuto)

**Señalar `GameMain.java`, método `main()`:**

*"El juego arranca con el patrón MVC clásico:"*
```java
GameModel model = new GameModel(800, 600, initTime);  // 1. Modelo (datos)
GameView view = new GameView(model);                   // 2. Vista (renderizado)
GameController controller = new GameController(model, view, port); // 3. Controlador
```

*"El Modelo contiene todos los datos del juego. La Vista solo lee el modelo y dibuja. El Controlador lee la entrada del jugador, ejecuta la lógica y actualiza el modelo. Esta separación hace el código más limpio y fácil de mantener."*

---

## 13. RESUMEN Y CONCLUSIÓN (1 minuto)

*"Para resumir los puntos técnicos más importantes:"*

| Aspecto | Técnica clave |
|---------|--------------|
| Sprite sheet trail | `getSubimage()` divide 256×48px en 8 frames de 32px |
| Trail escala con velocidad | `scaleFactor = 0.3 + (speed/maxSpeed) * 0.7` |
| Sprite sheet explosión | 704×64px → 11 frames de 64×64px, avance por tiempo real |
| Movimiento nave | Física newtoniana: empuje acumula velocidad, fricción 4%/frame |
| Wraparound bordes | `if (pos.x < 0) pos.x += MAP_WIDTH` en 4 bordes |
| Disparos | Proyectil en punta de nave a velocidad 12, cooldown 300ms |
| Life Generator (asteroides) | Cinturones aleatorios, velocidad escala con nivel |
| World Generator (planetas) | 4 patrones geométricos: anillo/espiral/cruz/disperso |
| Paquetes | 4 tipos con probabilidades: 55% normal, 25% urgente, 15% pesado, 5% especial |
| Fondo parallax | 3 capas a 30%, 50% y 60% de la velocidad de cámara |
| P2P red | UDP Multicast 230.0.0.1:4446, heartbeat cada 5s, timeout 30s |
| Interpolación red | `pos = pos * 0.7 + received * 0.3` (suavizado de latencia) |

*"El proyecto demuestra cómo combinar patrones de diseño (MVC), física básica, animación por sprites y comunicación en red en un juego funcional en Java. ¿Preguntas?"*

---

## PREGUNTAS FRECUENTES QUE PUEDEN HACER

**¿Por qué UDP y no TCP para el P2P?**
*"UDP es más rápido y no requiere conexión establecida. En un juego, si se pierde un paquete de posición no importa porque llega el siguiente en 100ms. TCP añadiría latencia innecesaria."*

**¿Por qué no se sincronizan los asteroides entre jugadores?**
*"Los asteroides son locales a cada instancia del juego para reducir el tráfico de red. Solo se sincronizan las posiciones de las naves de los jugadores."*

**¿Qué pasa si dos jugadores recogen el mismo paquete?**
*"En la implementación actual, cada paquete es local a cada instancia. El multijugador está enfocado en mostrar dónde están los otros jugadores, no en un estado completamente sincronizado."*

**¿Cuántos jugadores soporta?**
*"El sistema soporta tantos como haya en la red local. El multicast envía a todos simultáneamente. Se puede especificar el puerto como argumento: `java GameMain 8888`, `java GameMain 8889`, etc."*
