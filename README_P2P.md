# Sistema P2P - Mensajero Intergaláctico

## 🌐 Descripción

Este juego ahora incluye un sistema **peer-to-peer (P2P)** totalmente funcional que permite que múltiples instancias del juego se comuniquen en red local sin necesidad de un servidor central.

## ✨ Características P2P

- ✅ **Descubrimiento automático** de peers en la red local (UDP multicast)
- ✅ **Sincronización en tiempo real** de posición y estado de naves
- ✅ **Diferenciación visual**: Las naves remotas se muestran con colores distintos
- ✅ **Sin configuración manual**: Las instancias se detectan automáticamente
- ✅ **Panel de red**: Muestra el número de peers conectados en pantalla

## 🎮 Diferenciación Visual

### Nave Local (Tu nave)
- **Apariencia**: Color original de la nave
- **Control**: Totalmente controlable con teclado

### Naves Remotas (Otros jugadores)
- **Apariencia**: Overlay de color semitransparente + borde de color
- **Colores únicos**: Cada peer tiene un color diferente (rojo, azul, verde, amarillo, etc.)
- **Control**: No controlables (solo visualización)

## 🚀 Cómo Usar

### Opción 1: Scripts Automáticos (Recomendado)

#### Windows (Batch)
```bash
run_p2p_demo.bat
```

#### Windows (PowerShell)
```powershell
.\run_p2p_demo.ps1
```

#### Linux/macOS
```bash
chmod +x run_p2p_demo.sh
./run_p2p_demo.sh
```

### Opción 2: Compilación y Ejecución Manual

1. **Compilar el proyecto:**
```bash
javac -d bin -sourcepath src src/GameMain.java src/controller/*.java src/model/*.java src/model/network/*.java src/model/generators/*.java src/view/*.java
```

2. **Lanzar primera instancia (Puerto 8888):**
```bash
java -cp bin GameMain 8888
```

3. **Lanzar segunda instancia (Puerto 8889):**
```bash
java -cp bin GameMain 8889
```

4. **Lanzar más instancias (Puertos diferentes):**
```bash
java -cp bin GameMain 8890
java -cp bin GameMain 8891
# etc...
```

## 📡 Configuración de Red

### Configuración Automática
- **Dirección Multicast**: 230.0.0.1
- **Puerto Multicast**: 4446
- **Protocolo**: UDP
- **Alcance**: Red local (LAN)

### Puertos de Instancia
Cada instancia del juego necesita su propio puerto (argumento de línea de comandos):
- Instancia 1: `8888`
- Instancia 2: `8889`
- Instancia 3: `8890`
- ...

> **Nota**: Los puertos individuales son solo para identificación. La comunicación multicast usa el puerto 4446 por defecto.

## 🔍 Verificación del Sistema P2P

### Indicadores de Conexión

1. **Consola**: Verás mensajes como:
   ```
   NetworkController iniciado: Peer_8888_... en puerto 8888
   Sistema P2P iniciado
   Nuevo peer conectado: Peer[...]
   Nueva nave remota añadida: LocalShip_...
   ```

2. **Interfaz**: En la esquina inferior derecha aparece:
   ```
   RED P2P
   2 naves remotas ●
   ```

3. **Visual**: Las naves de otros jugadores aparecen con colores diferentes

### Troubleshooting

#### No se detectan peers
- ✅ Verifica que todas las instancias estén en la misma red local
- ✅ Asegúrate de que el firewall no bloquee el tráfico multicast
- ✅ Comprueba que cada instancia use un puerto diferente
- ✅ En Windows: Ejecuta como administrador si es necesario

#### Las naves no se sincronizan
- ✅ Verifica la consola en busca de errores
- ✅ Asegúrate de que el multicast esté habilitado en tu red
- ✅ Reinicia las instancias del juego

#### Latencia o retraso
- ✅ Normal en redes con alta latencia
- ✅ La interpolación suave compensa hasta 100ms
- ✅ Verifica la congestión de tu red local

## 🛠️ Arquitectura Técnica

### Clases Principales

#### `model/network/`
- **`PeerInfo.java`**: Información de cada peer (ID, IP, puerto, timestamp)
- **`NetworkMessage.java`**: Serialización de mensajes P2P
  - `PEER_ANNOUNCE`: Anuncio de presencia
  - `SHIP_UPDATE`: Actualización de posición/estado
  - `PEER_LEAVE`: Peer abandonando la red

#### `controller/`
- **`NetworkController.java`**: Gestión de comunicación UDP multicast
  - Descubrimiento de peers
  - Envío/recepción de mensajes
  - Heartbeat periódico (cada 5 segundos)
  - Timeout de peers (30 segundos)

#### Modificaciones en Clases Existentes
- **`Ship.java`**: Propiedades P2P (shipId, ownerId, isRemote, tintColor)
- **`GameModel.java`**: Gestión de naves remotas (Map<String, Ship>)
- **`GameView.java`**: Renderizado diferenciado (overlay de color + borde)
- **`GameController.java`**: Integración con NetworkController, sincronización cada 100ms

### Flujo de Sincronización

1. **Inicio**:
   - NetworkController inicia en puerto especificado
   - Envía `PEER_ANNOUNCE` al grupo multicast
   - Escucha mensajes entrantes

2. **Descubrimiento**:
   - Cada peer recibe anuncios de otros peers
   - Se añaden a la lista de peers conocidos
   - Panel de red se actualiza

3. **Actualización (10 veces/segundo)**:
   - GameController envía `SHIP_UPDATE` con posición/estado de nave local
   - Otros peers reciben el mensaje
   - Se actualiza/crea nave remota en su GameModel
   - GameView renderiza con color diferenciado

4. **Desconexión**:
   - Al cerrar, envía `PEER_LEAVE`
   - Timeout automático si no hay heartbeat (30s)
   - Se eliminan naves del peer desconectado

### Interpolación de Red

Las naves remotas usan interpolación suave (70% posición actual + 30% nueva):
```java
position.x = position.x * 0.7 + newX * 0.3;
position.y = position.y * 0.7 + newY * 0.3;
```

Esto compensa la latencia de red y proporciona movimiento fluido.

## 🎨 Personalización

### Colores de Naves Remotas

Los colores se asignan automáticamente basándose en el hash del peer ID:
- Rojo: `(255, 100, 100)`
- Azul: `(100, 150, 255)`
- Verde: `(100, 255, 100)`
- Amarillo: `(255, 255, 100)`
- Magenta: `(255, 100, 255)`
- Cian: `(100, 255, 255)`
- Naranja: `(255, 150, 50)`
- Violeta: `(200, 100, 255)`

### Configuración de Red (NetworkController)

Puedes modificar estas constantes en `NetworkController.java`:
```java
private static final String MULTICAST_ADDRESS = "230.0.0.1";
private static final int MULTICAST_PORT = 4446;
private static final int PEER_TIMEOUT_MS = 30000; // 30 segundos
private static final int HEARTBEAT_INTERVAL_MS = 5000; // 5 segundos
```

### Tasa de Actualización (GameController)

```java
private static final long NETWORK_UPDATE_INTERVAL_MS = 100; // 100ms = 10 updates/s
```

Reducir este valor aumenta la frecuencia de actualización (más tráfico de red, más fluidez).

## 📊 Estadísticas de Red

### Tráfico Estimado
- **Por nave**: ~10 mensajes/segundo
- **Tamaño mensaje**: ~150 bytes
- **Bandwidth por nave**: ~1.5 KB/s
- **4 jugadores**: ~6 KB/s total

### Latencia
- **Red local (LAN)**: < 5ms
- **WiFi local**: 5-20ms
- **Interpolación compensa**: hasta 100ms

## 🔒 Seguridad

> ⚠️ **Advertencia**: Este es un sistema P2P básico sin cifrado ni autenticación. Solo para uso en redes locales confiables.

## 📝 Ejemplo de Sesión

```bash
# Terminal 1
$ java -cp bin GameMain 8888
Sistema P2P inicializado en puerto 8888
NetworkController iniciado: Peer_8888_1708415234567 en puerto 8888
Sistema P2P iniciado

# Terminal 2
$ java -cp bin GameMain 8889
Sistema P2P inicializado en puerto 8889
NetworkController iniciado: Peer_8889_1708415236789 en puerto 8889
Sistema P2P iniciado
Nuevo peer conectado: Peer[Peer_8888_... @ 192.168.1.10:8888]
Nueva nave remota añadida: LocalShip_1708415234567

# Terminal 1 (detecta peer 2)
Nuevo peer conectado: Peer[Peer_8889_... @ 192.168.1.10:8889]
Nueva nave remota añadida: LocalShip_1708415236789
```

## 🎯 Próximas Mejoras

- [ ] Sincronización de asteroides y paquetes
- [ ] Chat entre jugadores
- [ ] Configuración de colores personalizada
- [ ] Estadísticas de red en HUD
- [ ] Soporte para WAN (relaying)
- [ ] Compresión de mensajes
- [ ] Cifrado básico

## 📄 Licencia

Mismo que el proyecto principal.

---

**¡Disfruta jugando en red local con tus amigos!** 🚀🎮
