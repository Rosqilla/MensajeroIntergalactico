# Sistema de Sprites Implementado

## 📋 Resumen
Se ha implementado un sistema completo de renderizado basado en sprites, reemplazando las formas geométricas originales con imágenes PNG.

## 🎨 Sprites Implementados

### 1. **Nave (Ship)**
- **Archivo**: `resources/sprites/ship/ship_red.png`
- **Características**:
  - Se rota según el ángulo de la nave (`ship.getRotationAngle()`)
  - Tinte amarillo cuando está inmune (opacity 0.7)
  - Centrado en la posición de la nave
  - Indicador amarillo cuando lleva un paquete

### 2. **Meteoritos (Asteroids)**
- **Archivos**: 
  - 4 variantes BIG: `meteorGrey_big1.png` - `meteorGrey_big4.png`
  - 2 variantes MED: `meteorGrey_med1.png` - `meteorGrey_med2.png`
  - 2 variantes SMALL: `meteorGrey_small1.png` - `meteorGrey_small2.png`
- **Características**:
  - Variante seleccionada usando `asteroid.hashCode()` para consistencia
  - Rotación lenta continua basada en tiempo + hashCode
  - CHASER tiene overlay rojo semitransparente
  - Barra de salud para meteoritos dañados
  - Hitbox debe coincidir con dimensiones del sprite

### 3. **Proyectiles (Projectiles)**
- **Archivos**: `laserBlue12.png` - `laserBlue16.png` (5 frames)
- **Características**:
  - Animación de 5 frames a 50ms por frame (FRAME_DELAY)
  - Cicla automáticamente 0→4→0
  - Centrado en la posición del proyectil
  - Fallback a círculo cyan si sprites no cargan

### 4. **Estela de Fuego (Fire Trail)**
- **Archivo**: `resources/sprites/trail/Group 4 - 4.png` (sprite sheet)
- **Características**:
  - 8 frames extraídos del sprite sheet horizontal
  - Animación a 50ms por frame
  - **Intensidad basada en velocidad**:
    - Velocidad baja = más transparente
    - Velocidad alta = más opaco
    - No se muestra si velocidad < 10% de MAX_SPEED
  - Posicionado en la cola de la nave (opuesto a dirección)
  - Rotado según ángulo de la nave

### 5. **Fondo (Background)**
- **Archivo**: `resources/sprites/background/Space01.png`
- **Características**:
  - Sistema de tiling infinito
  - Efecto parallax (se mueve al 50% de la velocidad de la cámara)
  - Calcula tiles necesarios dinámicamente según viewport
  - Se dibuja antes que las estrellas

### 6. **Explosiones (Explosions)**
- **Archivo**: `resources/sprites/explosion/Explosion Animation.png`
- **Estado**: Sprite sheet cargado, **pendiente de implementar**
- **TODO**: 
  - Determinar grid del sprite sheet
  - Extraer frames individuales
  - Implementar animación en destrucción de asteroides/nave
  - Agregar lista de explosiones en GameModel

## 🎮 Sistema de Colores de Paquetes

### Regla Implementada:
```java
if (pkg.getTargetPlanet() != null) {
    pkgColor = pkg.getTargetPlanet().getColor();
} else {
    // Usar colores por defecto según tipo
}
```

- **Paquetes con planeta destino**: Usan el color del planeta
- **URGENT sin planeta**: Rojo (220, 50, 50)
- **HEAVY sin planeta**: Marrón (139, 90, 43)
- **NORMAL sin planeta**: Verde (50, 200, 50)

### IMPORTANTE:
Los generadores de paquetes deben asegurar que:
- ✅ Paquetes URGENT/HEAVY **NO** tengan colores que coincidan con planetas
- ✅ Paquetes con `targetPlanet` hereden el color del planeta

## 🔧 Configuración Técnica

### Animaciones
```java
private static final long FRAME_DELAY = 50; // ms entre frames
private int projectileFrame = 0;  // 0-4
private int trailFrame = 0;       // 0-7
private long lastFrameTime = 0;
```

### Parallax Background
```java
int bgOffsetX = (int)(camX * 0.5);  // 50% velocidad cámara
int bgOffsetY = (int)(camY * 0.5);
```

### Trail Velocity Scaling
```java
double speed = Math.sqrt(vx² + vy²);
float alphaFactor = min(speed / MAX_SPEED, 1.0);
if (alphaFactor < 0.1f) return; // No mostrar si muy lento
```

## 📁 Estructura de Carpetas
```
resources/
└── sprites/
    ├── ship/
    │   └── ship_red.png
    ├── asteroid/
    │   ├── meteorGrey_big1-4.png
    │   ├── meteorGrey_med1-2.png
    │   └── meteorGrey_small1-2.png
    ├── projectile/
    │   └── laserBlue12-16.png
    ├── explosion/
    │   └── Explosion Animation.png
    ├── background/
    │   └── Space01.png
    ├── trail/
    │   └── Group 4 - 4.png
    └── planet/
        └── (vacío - no se usan sprites para planetas)
```

## ✅ Implementado
- [x] Carga de todos los sprites
- [x] Renderizado de nave con rotación
- [x] Renderizado de asteroides con variantes y rotación
- [x] Animación de proyectiles (5 frames)
- [x] Estela de fuego animada con scaling de velocidad
- [x] Fondo con tiling y parallax
- [x] Sistema de colores de paquetes según planetas

## ⏳ Pendiente
- [ ] Sistema de explosiones (extraer frames del sprite sheet)
- [ ] Ajustar hitboxes de colisión para que coincidan exactamente con sprites
- [ ] Sprites para planetas (actualmente usa círculos de colores)
- [ ] Posible optimización de rendimiento con sprite caching

## 🐛 Notas de Debugging
- Si los sprites no cargan, verifica la consola: `System.err.println("Error cargando sprites: ...")`
- Los asteroides usan `hashCode()` para selección de variante - cada asteroide tendrá siempre el mismo sprite
- La rotación de asteroides es continua pero lenta (5 segundos por revolución completa)
- El trail solo se dibuja si la nave se mueve > 10% de MAX_SPEED

## 🎯 Mejoras Futuras
1. **Partículas de explosión**: Implementar sistema de partículas al destruir asteroides
2. **Sprites de planetas**: Reemplazar círculos con sprites de planetas
3. **Efectos de boost**: Sprite diferente o efecto visual cuando boost activo
4. **Sonidos**: Agregar efectos de sonido sincronizados con animaciones
5. **Hitbox precision**: Usar máscaras de colisión pixel-perfect basadas en sprites
