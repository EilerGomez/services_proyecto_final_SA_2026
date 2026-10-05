# Módulo `comun`

Librería Java compartida por los 7 microservicios del repositorio `services_proyecto_final_SA_2026`
(identity, fleet, trips, sales, maintenance, reporting y notification).

Este README explica **qué es**, **por qué existe**, **cómo se integra en un servicio** y **cómo se usa cada pieza**,
con ejemplos completos. Está pensado para que cualquier integrante (o una IA a la que se le pida ayuda) tenga todo
el contexto necesario.

> Stack: Java 21 · Spring Boot 4.1.1 · Spring Cloud 2025.1.3 · Jackson 3 · Spring AMQP 4 (RabbitMQ) · PostgreSQL 16

---

## Índice

1. [Qué es `comun` y por qué existe](#1-qué-es-comun-y-por-qué-existe)
2. [Cómo llega `comun` a un microservicio (Maven)](#2-cómo-llega-comun-a-un-microservicio-maven)
3. [Compilar e importar las clases (terminal y NetBeans)](#3-compilar-e-importar-las-clases-terminal-y-netbeans)
4. [Configurar un microservicio para usar `comun`](#4-configurar-un-microservicio-para-usar-comun)
5. [Mapa de paquetes e imports](#5-mapa-de-paquetes-e-imports)
6. [Eventos: conceptos](#6-eventos-conceptos)
7. [Eventos: publicar](#7-eventos-publicar)
8. [Eventos: consumir](#8-eventos-consumir)
9. [Eventos: ejemplo completo de saga](#9-eventos-ejemplo-completo-de-saga)
10. [Errores](#10-errores)
11. [Seguridad: usuario actual y roles](#11-seguridad-usuario-actual-y-roles)
12. [Estructura del JWT](#12-estructura-del-jwt)
13. [Correlación y logs](#13-correlación-y-logs)
14. [Pruebas](#14-pruebas)
15. [Reglas: qué hacer y qué no](#15-reglas-qué-hacer-y-qué-no)
16. [Modificar `comun`](#16-modificar-comun)
17. [Solución de problemas](#17-solución-de-problemas)

---

## 1. Qué es `comun` y por qué existe

Los 7 microservicios necesitan hacer las mismas tareas **técnicas**:

- publicar eventos en RabbitMQ sin perderlos,
- recibir eventos sin procesarlos dos veces,
- responder errores con el mismo formato,
- saber qué usuario hace la petición y qué roles tiene.

Si cada integrante programara esto en cada servicio, habría 7 versiones distintas del mismo código y los servicios
**no se entenderían entre sí** (un evento publicado por Sales debe poder leerlo Identity con exactamente el mismo
formato). `comun` escribe esas piezas **una sola vez** y todos los servicios las usan igual.

| Sin `comun` (en cada servicio) | Con `comun` (lo que escribe el servicio) |
| --- | --- |
| Tabla outbox, job que publica, publisher confirms, reintentos | `publicador.publicar("Venta", id, "BoletoVendido", payload, fecha)` |
| Listener, leer JSON, despachar por tipo, transacción, control de duplicados, DLQ | Una clase que implementa `ManejadorEvento<T>` |
| Declarar exchanges, colas, DLQ y bindings en Java | 5 líneas en `application.yml` |
| Un `@RestControllerAdvice` propio y su formato de error | `throw new ConflictoException("ASIENTO_OCUPADO", "...")` |
| Leer el header, validar el JWT, revisar roles | `@RequiereRol(...)` + parámetro `UsuarioActual` |

### Qué NO es

- **No es un microservicio**: no tiene `main`, puerto, base de datos ni `Dockerfile`. Es un JAR que se mete
  **dentro** de cada servicio al compilar.
- **No contiene negocio**: nada de entidades (`Venta`, `Bus`), repositorios ni reglas ("cuándo se puede vender").
  Si se compartieran, un cambio en Sales obligaría a recompilar Fleet y los servicios dejarían de ser independientes,
  que es justo lo que la arquitectura de microservicios evita.
- **No lo usa el api-gateway**: el gateway es reactivo (WebFlux) y `comun` usa Spring MVC (Servlet).
  El gateway valida el JWT por su cuenta con el mismo `JWT_SECRET`.

---

## 2. Cómo llega `comun` a un microservicio (Maven)

El repositorio es un **proyecto Maven multimódulo**:

```
services/
├── pom.xml                 ← POM padre: lista los módulos y fija versiones
├── comun/                  ← módulo librería (packaging jar, SIN spring-boot-maven-plugin)
├── identity-service/       ← módulo aplicación (CON spring-boot-maven-plugin)
├── fleet-service/
└── ...
```

1. El POM padre declara `comun` en `<dependencyManagement>` con la versión `${project.version}`.
2. Cada servicio lo agrega como dependencia **sin versión**:

   ```xml
   <dependency>
       <groupId>gt.cunoc.proyecto2</groupId>
       <artifactId>comun</artifactId>
   </dependency>
   ```

3. Al compilar desde la raíz, Maven ordena los módulos por dependencias: **primero `comun`**, después los servicios.
4. `spring-boot-maven-plugin` empaqueta el servicio como JAR ejecutable y mete `comun` adentro:

   ```
   fleet-service.jar
   └── BOOT-INF/lib/
       ├── comun-1.0.0-SNAPSHOT.jar     ← aquí viaja comun
       ├── spring-web-*.jar
       └── ...
   ```

5. El `Dockerfile` del servicio copia `fleet-service.jar`; `comun` ya va dentro de la imagen.

**Por qué `comun` no lleva `spring-boot-maven-plugin`**: si lo tuviera, su JAR se reempaquetaría como aplicación
(con las clases en `BOOT-INF/classes`) y los servicios no podrían importar sus clases.

**Consecuencia**: si se cambia `comun`, hay que recompilar y redesplegar **todos** los servicios. Por eso el CI
redespliega todo cuando cambia `comun/**`.

---

## 3. Compilar e importar las clases (terminal y NetBeans)

### Primera vez (o después de hacer `git pull`)

Desde la **raíz** del repositorio (donde está el `pom.xml` padre):

```bash
mvn -B clean install -DskipTests
```

`install` compila todos los módulos y copia `comun-1.0.0-SNAPSHOT.jar` al repositorio local de Maven
(`~/.m2/repository/gt/cunoc/proyecto2/comun/`). A partir de ahí cualquier servicio encuentra sus clases.

### Después de modificar `comun`

```bash
mvn -B -pl comun install                 # recompila e instala solo comun (con sus pruebas)
```

Si no se reinstala, el servicio sigue usando la versión vieja de `comun` que estaba en `~/.m2`.

### Compilar un solo servicio (y lo que necesita)

```bash
mvn -B -pl sales-service -am package     # -am = also make: compila también comun
```

### NetBeans

1. *File → Open Project* → seleccionar la carpeta **raíz** `services` (no un servicio suelto).
2. NetBeans muestra el proyecto padre con un nodo **Modules**; abrir desde ahí el servicio que se va a trabajar.
3. La primera vez: clic derecho sobre el proyecto padre → **Build with Dependencies**
   (equivale a `mvn install`).
4. Si NetBeans subraya en rojo un `import gt.cunoc.proyecto2.comun...`: clic derecho en el servicio →
   **Reload POM**, o volver a hacer *Build with Dependencies* en el padre.

### Cómo se importa una clase

Igual que una clase de Spring: con su paquete completo.

```java
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
```

El IDE lo autocompleta (Ctrl + Shift + I en NetBeans) **solo si** `comun` está declarado en el `pom.xml` del
servicio y fue compilado/instalado al menos una vez.

---

## 4. Configurar un microservicio para usar `comun`

Los 7 servicios ya vienen configurados en el esqueleto. Esta sección explica cada pieza por si hay que revisarla.

### 4.1 Clase principal: escanear el paquete de `comun`

```java
@SpringBootApplication(scanBasePackages = {
        "gt.cunoc.proyecto2.sales",     // paquete del servicio
        "gt.cunoc.proyecto2.comun"      // paquete de comun
})
public class SalesServiceApplication { ... }
```

**Por qué**: `@SpringBootApplication` solo busca `@Component`, `@Service`, `@Configuration`... dentro del paquete del
servicio. Sin la segunda línea, Spring no crea los beans de `comun` (`PublicadorEventos`, filtros, manejador de
errores) y aparecen errores como `No qualifying bean of type PublicadorEventos`.

### 4.2 Tablas en la migración V1 del servicio

```sql
-- si el servicio PUBLICA eventos
CREATE TABLE eventos_salida (
  id UUID PRIMARY KEY, tipo_agregado VARCHAR(50) NOT NULL, agregado_id UUID NOT NULL,
  tipo_evento VARCHAR(80) NOT NULL, version_evento INT NOT NULL DEFAULT 1, datos JSONB NOT NULL,
  fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(), fecha_publicacion TIMESTAMPTZ,
  intentos INT NOT NULL DEFAULT 0, ultimo_error TEXT);

-- si el servicio CONSUME eventos
CREATE TABLE eventos_procesados (
  evento_id UUID PRIMARY KEY, tipo_evento VARCHAR(80) NOT NULL,
  fecha_procesado TIMESTAMPTZ NOT NULL DEFAULT now());
```

Ya existen en las `V1__esquema_inicial.sql` de cada servicio. Son tablas **de cada servicio**: el código es
compartido, los datos no. El `PublicadorEventos` que corre dentro de Sales escribe en `bd_ventas.eventos_salida`.

### 4.3 `application.yml`

```yaml
spring:
  rabbitmq:
    host: ${RABBIT_HOST:localhost}
    port: ${RABBIT_PORT:5672}
    username: ${RABBIT_USER:proyecto2}
    password: ${RABBIT_PASSWORD:proyecto2}
    publisher-confirm-type: correlated    # OBLIGATORIO si el servicio publica (ver §6)
    listener:
      simple:
        default-requeue-rejected: false   # al agotar reintentos el mensaje va a la DLQ
        retry:
          enabled: true
          max-attempts: 4                 # 1 intento + 3 reintentos
          initial-interval: 1000          # 1 s, 2 s, 4 s
          multiplier: 2

comun:
  eventos:
    exchange: sales.eventos               # exchange propio → activa PublicadorEventos y RelayOutbox
    cola: sales.q                         # cola propia → activa ConsumidorEventos
    suscripciones:                        # exchange de otro servicio → eventos que escucha
      "[identity.eventos]": [ClienteRegistrado, PerfilActualizado, CarteraDebitada, DebitoCarteraRechazado, CarteraReembolsada]
      "[trips.eventos]": [ViajeProgramado, ViajeActualizado, ViajeEliminado, ViajeIniciado, CotizacionAlquilerConfirmada]
    outbox:
      intervalo-ms: 1000                  # opcional (por defecto 1000)
      lote: 100                           # opcional (por defecto 100)

app:
  jwt:
    secret: ${JWT_SECRET:solo-desarrollo-cambiar-este-secreto-de-32-caracteres}   # ≥ 32 caracteres, IGUAL en todos
    duracion-minutos: 60
```

- Las claves con punto van entre corchetes y comillas (`"[trips.eventos]"`); si no, Spring interpreta los puntos
  como niveles del YAML.
- Reporting y Notification **no tienen `exchange`** porque no publican; solo `cola` y `suscripciones`.
- Al arrancar, `ConfiguracionEventos` crea en RabbitMQ el exchange propio, la cola, su DLQ (`sales.q.dlq`) y un
  binding por cada evento suscrito. Declarar algo que ya existe no tiene efecto.

---

## 5. Mapa de paquetes e imports

Base: `gt.cunoc.proyecto2.comun`

| Paquete | Clase | La usa el servicio | Para qué |
| --- | --- | --- | --- |
| `eventos.publicador` | `PublicadorEventos` | **Sí** | Publicar eventos (outbox) |
| `eventos.manejadorevento` | `ManejadorEvento<T>` | **Sí** | Interfaz para consumir un tipo de evento |
| `eventos.recibido` | `EventoRecibido<T>` | **Sí** | Evento recibido con payload tipado |
| `eventos.sobre` | `EventoSobre` | Rara vez | Sobre JSON que viaja por RabbitMQ |
| `eventos.contextocorrelacion` | `ContextoCorrelacion` | Rara vez | Id de correlación del flujo actual |
| `eventos.outbox` | `RelayOutbox` | No (interna) | Job que publica el outbox |
| `eventos.consumidor` | `ConsumidorEventos` | No (interna) | Listener único de la cola |
| `eventos.configuracion` | `ConfiguracionEventos` | No (interna) | Topología RabbitMQ |
| `eventos.propiedadesevento` | `PropiedadesEventos` | No (interna) | Lee `comun.eventos.*` |
| `idempotencia` | `ProcesadorIdempotente` | No (interna) | Evita procesar dos veces |
| `errores` | `NoEncontradoException`, `ConflictoException`, ... | **Sí** | Errores de negocio |
| `errores` | `ErrorRespuesta`, `ManejadorGlobalErrores`, `EscritorErrores` | No (interna) | Formato y manejo global |
| `seguridad.users` | `UsuarioActual`, `RequiereRol`, `Roles` | **Sí** | Usuario autenticado y permisos |
| `seguridad.service` | `ServicioJwt` | Solo Identity | Generar el token en el login |
| `seguridad.filtro` | `FiltroAutenticacion`, `FiltroCorrelacion` | No (interna) | Filtros HTTP |
| `seguridad.users` | `InterceptorRoles`, `ResolverUsuarioActual` | No (interna) | Aplicar `@RequiereRol` e inyectar `UsuarioActual` |
| `seguridad.config` | `ConfiguracionSeguridadWeb` | No (interna) | Registra interceptor y resolver |

```java
// Lo que normalmente importa un servicio
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.ReglaNegocioException;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
```

---

## 6. Eventos: conceptos

```
SERVICIO PRODUCTOR (Sales)                                    SERVICIO CONSUMIDOR (Identity)
┌──────────────────────────────────────┐                    ┌──────────────────────────────────────┐
│ @Transactional                       │                    │ ConsumidorEventos (@RabbitListener)  │
│  1. guarda la venta (JPA)            │                    │  5. lee el sobre JSON                │
│  2. publicador.publicar(...)         │                    │  6. ProcesadorIdempotente:           │
│     └─ INSERT en eventos_salida      │                    │     INSERT eventos_procesados        │
│  COMMIT (venta + evento juntos)      │                    │     (si ya existía → ignora)         │
│                                      │                    │  7. ManejadorEvento.manejar(...)     │
│ RelayOutbox (cada 1 s)               │     RabbitMQ       │  COMMIT                              │
│  3. lee pendientes                   │                    │                                      │
│  4. publica y espera confirmación ───┼─► sales.eventos ──►│ identity.q                           │
│     marca fecha_publicacion          │                    │ error → 4 intentos → identity.q.dlq  │
└──────────────────────────────────────┘                    └──────────────────────────────────────┘
```

| Concepto | Qué es en este proyecto |
| --- | --- |
| **Exchange** | Uno por servicio productor, tipo *topic*: `identity.eventos`, `fleet.eventos`, `trips.eventos`, `sales.eventos`, `maintenance.eventos` |
| **Routing key** | El nombre del evento: `BoletoVendido`, `ViajeProgramado`, ... |
| **Cola** | Una por servicio consumidor: `identity.q`, `sales.q`, `reporting.q`, ... |
| **Binding** | "La cola X recibe del exchange Y los eventos Z" (se define en `comun.eventos.suscripciones`) |
| **DLQ** | `<cola>.dlq`: donde terminan los mensajes que fallaron 4 veces o que no se pueden leer |
| **Sobre** | El JSON común de todos los eventos (`EventoSobre`) |

### Nombres de los eventos

Convención de nombres (en español, PascalCase, sin tildes ni ñ): `<Sustantivo><Participio>` en pasado
(`BoletoVendido`, `ViajeProgramado`). El nombre del evento es la routing key, el valor de `tipoEvento()` y el nombre
recomendado del record del payload; el manejador se llama `Manejador<Evento>` (p. ej. `ManejadorBoletoVendido`).

La lista completa (33 eventos, con su productor y consumidores) está en `CLAUDE.md` §8.

### El sobre (`EventoSobre`)

```json
{
  "eventId": "5f1c7e0a-3b8e-4c55-9a1e-2d7b8f0c1a22",
  "eventType": "BoletoVendido",
  "version": 1,
  "aggregateId": "b7d4e2a1-0c9f-4e3b-8a6d-1f2e3c4d5e6f",
  "aggregateVersion": 3,
  "occurredAt": "2026-10-20T15:30:12Z",
  "businessDate": "2026-10-20",
  "correlationId": "9a8b7c6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d",
  "payload": { "ventaId": "b7d4e2a1-...", "asientos": [12, 13], "montoTotal": 150.00 }
}
```

| Campo | Para qué |
| --- | --- |
| `eventId` | Id único; el consumidor lo usa para no procesar duplicados |
| `eventType` | Nombre del evento = routing key |
| `version` | Versión del **esquema** del evento; sube solo si se quita o renombra un campo |
| `aggregateId` | Id principal del payload (ventaId, viajeId, busId...) |
| `aggregateVersion` | Versión del registro; el consumidor descarta eventos más viejos que lo que ya tiene (puede ser null) |
| `occurredAt` | Momento técnico en que se generó |
| `businessDate` | **Fecha de negocio elegida por el usuario** (el enunciado exige fechas manuales); los reportes filtran por esta |
| `correlationId` | Id del flujo completo (saga); se ve en los logs de todos los servicios |
| `payload` | Datos del evento según el catálogo de eventos (`CLAUDE.md` §8) |

### Por qué outbox y no publicar directo con `RabbitTemplate`

Publicar directo dentro del método de negocio tiene dos fallas posibles:

- se guarda la venta pero RabbitMQ está caído → **el evento se pierde** y Identity nunca cobra;
- se publica el evento pero la transacción hace rollback → **Identity cobra una venta que no existe**.

Con outbox, el evento se guarda en la **misma transacción** que la venta: o se guardan los dos o ninguno. Después
`RelayOutbox` lo publica, y si RabbitMQ está caído lo reintenta cada segundo hasta lograrlo. Solo lo marca como
publicado cuando RabbitMQ confirma que lo recibió (*publisher confirms*); por eso es obligatorio
`publisher-confirm-type: correlated`. Si un evento falla, el lote se detiene para no publicar los siguientes antes
que él (se conserva el orden).

### Por qué idempotencia

RabbitMQ garantiza entrega **al menos una vez**: si el consumidor se cae justo después de procesar y antes de
confirmar, el mensaje se vuelve a entregar. Sin control, Identity cobraría dos veces. `ProcesadorIdempotente`
registra el `eventId` en `eventos_procesados` en la misma transacción que el manejador: si el `eventId` ya está,
el evento se ignora. Si el manejador falla, el registro también se revierte y el evento se puede reintentar.

---

## 7. Eventos: publicar

### Reglas

1. **Siempre dentro de un método `@Transactional`**. `publicar` exige una transacción activa
   (`Propagation.MANDATORY`); fuera de una lanza `IllegalTransactionStateException`. Así se garantiza que el
   evento y el cambio de negocio se guardan juntos.
2. **Nunca usar `RabbitTemplate` directamente** para eventos de negocio.
3. El **payload es un record propio** con los campos del catálogo de eventos (`CLAUDE.md` §8).
4. `fechaNegocio` = la fecha que el usuario eligió en la pantalla (no `LocalDate.now()` si el usuario eligió otra).
5. Para entidades que se actualizan (*Created / *Updated), enviar el **estado completo** y la versión.

### Ejemplo completo (Sales publica `BoletoVendido`)

```java
package gt.cunoc.proyecto2.sales.venta.evento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Payload de BoletoVendido. Campos según el catálogo de eventos. */
public record BoletoVendido(
        UUID ventaId, UUID viajeId, UUID rutaId, UUID busId, UUID sucursalId, String canal,
        UUID clienteUsuarioId, String clienteNit, String clienteNombre, String clienteCorreo,
        List<Integer> asientos, int cantidadBoletos, BigDecimal montoTotal, String metodoPago,
        LocalDate fechaPago, String origenNombre, String destinoNombre, Instant fechaSalida) {
}
```

```java
package gt.cunoc.proyecto2.sales.venta;

import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.sales.venta.evento.BoletoVendido;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmacionVentaService {

    private final VentaRepository ventas;
    private final PublicadorEventos publicador;

    public ConfirmacionVentaService(VentaRepository ventas, PublicadorEventos publicador) {
        this.ventas = ventas;
        this.publicador = publicador;
    }

    @Transactional
    public void confirmar(UUID ventaId) {
        Venta venta = ventas.findById(ventaId)
                .orElseThrow(() -> new NoEncontradoException("VENTA_NO_EXISTE", "La venta no existe"));

        venta.confirmar();                       // regla de negocio (cambia estado a CONFIRMADA)
        ventas.saveAndFlush(venta);              // flush para que @Version ya esté incrementada

        publicador.publicar(
                "Venta",                          // tipo de agregado
                venta.getId(),                    // aggregateId
                venta.getVersion(),               // aggregateVersion (campo @Version de la entidad)
                "BoletoVendido",                     // tipo de evento = routing key
                new BoletoVendido(venta.getId(), venta.getViajeId(), /* ...resto de campos... */),
                venta.getFechaPago());            // businessDate = fecha elegida por el usuario
    }                                             // COMMIT: venta + evento en eventos_salida
}
```

Si algo falla antes del `COMMIT`, ni la venta ni el evento se guardan. Si RabbitMQ está caído, el evento queda en
`eventos_salida` y sale cuando vuelva.

Para eventos sin versión (por ejemplo `RutaEliminada`) se usa la sobrecarga corta:

```java
publicador.publicar("Ruta", rutaId, "RutaEliminada", new RutaEliminada(rutaId), fechaNegocio);
```

### Ver qué pasó con un evento publicado

```sql
-- pendientes o con error (en la BD del servicio productor)
SELECT tipo_evento, intentos, ultimo_error, fecha_creacion
FROM eventos_salida WHERE fecha_publicacion IS NULL ORDER BY fecha_creacion;
```

---

## 8. Eventos: consumir

### Reglas

1. **Un `@Component` por tipo de evento** que implementa `ManejadorEvento<T>`.
2. El payload es un **record propio del servicio consumidor**, con **solo los campos que usa**, y siempre con
   `@JsonIgnoreProperties(ignoreUnknown = true)`. Así el productor puede agregar campos sin romper a nadie.
   Nunca se comparten estas clases entre servicios ni se ponen en `comun`.
3. El evento debe estar suscrito en `comun.eventos.suscripciones` del `application.yml`; si no, no llega.
4. El manejador **corre dentro de una transacción**. Si lanza una excepción: rollback, 4 intentos (1 s, 2 s, 4 s)
   y luego a la DLQ. No hay que atrapar excepciones para "evitar" el reintento.
5. Para eventos *Created / *Updated: hacer **upsert** y **descartar** el evento si `aggregateVersion` es menor o
   igual a la versión guardada (los eventos pueden llegar desordenados por los reintentos).
6. Dentro del manejador se puede publicar otro evento con `PublicadorEventos` (ya hay transacción); ver §9.

### Qué hace `ConsumidorEventos` por detrás

1. Lee el mensaje de la cola y convierte el JSON a `EventoSobre`.
   JSON inválido → directo a la DLQ, sin reintentos (reintentar no lo arreglaría).
2. Busca el `ManejadorEvento` cuyo `tipoEvento()` coincide con `eventType`.
   Si no hay ninguno → lo registra en el log y lo descarta.
3. Abre transacción con `ProcesadorIdempotente`: registra `eventId`; si ya existía → ignora.
4. Convierte `payload` al record de `tipoPayload()`. Si es incompatible → DLQ.
5. Llama a `manejar(...)`.

### Ejemplo completo (Sales consume `ViajeProgramado` para su copia local de viajes)

```java
package gt.cunoc.proyecto2.sales.viaje.evento;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Solo los campos de ViajeProgramado que Sales necesita. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeProgramado(
        UUID viajeId, String tipo, UUID rutaId, UUID sucursalId,
        String origenNombre, String destinoNombre, UUID busId, String busPlaca,
        int capacidad, BigDecimal precioBoleto, Instant fechaSalida) {
}
```

```java
package gt.cunoc.proyecto2.sales.viaje.evento;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.viaje.AsientoService;
import gt.cunoc.proyecto2.sales.viaje.VistaViaje;
import gt.cunoc.proyecto2.sales.viaje.VistaViajeRepository;
import org.springframework.stereotype.Component;

@Component
public class ManejadorViajeProgramado implements ManejadorEvento<ViajeProgramado> {

    private final VistaViajeRepository viajes;
    private final AsientoService asientos;

    public ManejadorViajeProgramado(VistaViajeRepository viajes, AsientoService asientos) {
        this.viajes = viajes;
        this.asientos = asientos;
    }

    @Override
    public String tipoEvento() {
        return "ViajeProgramado";
    }

    @Override
    public Class<ViajeProgramado> tipoPayload() {
        return ViajeProgramado.class;
    }

    @Override
    public void manejar(EventoRecibido<ViajeProgramado> evento) {
        ViajeProgramado v = evento.payload();
        if (!"REGULAR".equals(v.tipo())) {
            return;                                    // los viajes privados no se venden por boleto
        }

        long version = evento.aggregateVersion() != null ? evento.aggregateVersion() : 0;
        VistaViaje existente = viajes.findById(v.viajeId()).orElse(null);
        if (existente != null && existente.getVersion() >= version) {
            return;                                    // evento viejo o repetido: se ignora
        }

        viajes.save(VistaViaje.desde(v, version));     // upsert en vista_viajes
        if (existente == null) {
            asientos.crearAsientos(v.viajeId(), v.capacidad());   // asientos 1..capacidad
        }
    }
}
```

No hace falta `@RabbitListener`, ni `@Transactional`, ni verificar duplicados: `comun` lo hace.

### Agregar una suscripción nueva

1. Confirmar en el catálogo de eventos (`CLAUDE.md` §8) que el productor publica ese evento.
2. Agregar el nombre en `comun.eventos.suscripciones` del `application.yml` del consumidor.
3. Crear su record de payload y su `ManejadorEvento`.
4. Reiniciar el servicio: el binding nuevo se crea solo.

### Ver qué pasó con un evento consumido

- Panel de RabbitMQ (`http://localhost:15672`, usuario `proyecto2`): *Queues* → `sales.q` (mensajes en espera)
  y `sales.q.dlq` (fallidos; *Get messages* muestra el contenido y el motivo en los headers `x-death`).
- `SELECT * FROM eventos_procesados ORDER BY fecha_procesado DESC;` en la BD del consumidor.
- Logs del servicio: `Manejadores de eventos registrados: [...]` al arrancar, y advertencias por eventos sin manejador.

---

## 9. Eventos: ejemplo completo de saga

Compra en línea de un boleto (Sales ↔ Identity). Muestra cómo un manejador **consume un evento y publica otro**
en la misma transacción.

```
Sales                                   RabbitMQ                                Identity
  │ reserva asientos                                                               │
  │ venta PENDIENTE_PAGO                                                           │
  │ publicar(DebitoCarteraSolicitado) ──► sales.eventos ──► identity.q ──────────────► │ ManejadorDebitoCarteraSolicitado:
  │                                                                                │   debita la cartera
  │ ◄── sales.q ◄── identity.eventos ◄── publicar(CarteraDebitada / DebitoCarteraRechazado) ─ │
  │ ManejadorCarteraDebitada:                                                          │
  │   asientos VENDIDO, venta CONFIRMADA                                           │
  │   publicar(BoletoVendido) ──► reporting.q, notification.q                         │
```

Manejador en Identity:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record DebitoCarteraSolicitado(String referenciaTipo, UUID referenciaId, UUID usuarioId,
                              BigDecimal monto, LocalDate fechaMovimiento) {
}

public record CarteraDebitada(UUID movimientoId, String referenciaTipo, UUID referenciaId, UUID usuarioId,
                              BigDecimal monto, BigDecimal saldoResultante, LocalDate fechaMovimiento) {
}

public record DebitoCarteraRechazado(String referenciaTipo, UUID referenciaId, UUID usuarioId,
                              BigDecimal monto, String motivo) {
}

@Component
public class ManejadorDebitoCarteraSolicitado implements ManejadorEvento<DebitoCarteraSolicitado> {

    private final CarteraService carteras;
    private final PublicadorEventos publicador;

    public ManejadorDebitoCarteraSolicitado(CarteraService carteras, PublicadorEventos publicador) {
        this.carteras = carteras;
        this.publicador = publicador;
    }

    @Override
    public String tipoEvento() { return "DebitoCarteraSolicitado"; }

    @Override
    public Class<DebitoCarteraSolicitado> tipoPayload() { return DebitoCarteraSolicitado.class; }

    @Override
    public void manejar(EventoRecibido<DebitoCarteraSolicitado> evento) {
        DebitoCarteraSolicitado s = evento.payload();
        Optional<MovimientoCartera> mov = carteras.intentarDebitar(s.usuarioId(), s.monto(),
                s.referenciaTipo(), s.referenciaId(), s.fechaMovimiento());

        if (mov.isPresent()) {
            publicador.publicar("Cartera", s.usuarioId(), "CarteraDebitada",
                    new CarteraDebitada(mov.get().getId(), s.referenciaTipo(), s.referenciaId(), s.usuarioId(),
                            s.monto(), mov.get().getSaldoResultante(), s.fechaMovimiento()),
                    evento.businessDate());
        } else {
            publicador.publicar("Cartera", s.usuarioId(), "DebitoCarteraRechazado",
                    new DebitoCarteraRechazado(s.referenciaTipo(), s.referenciaId(), s.usuarioId(), s.monto(),
                            "SALDO_INSUFICIENTE"),
                    evento.businessDate());
        }
    }
}
```

- El débito, el registro en `eventos_procesados` y el evento de respuesta se guardan en **una sola transacción**:
  si algo falla, no queda un débito sin respuesta ni una respuesta sin débito.
- El `correlationId` del evento recibido se copia solo al evento publicado, así toda la saga comparte el mismo id
  en los logs.
- `DebitoCarteraRechazado` **no es un error**: es una respuesta válida de negocio. No se lanza excepción (eso causaría
  reintentos y DLQ); se publica el evento de rechazo.

---

## 10. Errores

### Formato único de respuesta

Todos los servicios responden los errores así (y el frontend los maneja de una sola forma):

```json
{
  "status": 409,
  "codigo": "ASIENTO_OCUPADO",
  "mensaje": "El asiento 12 ya fue vendido",
  "ruta": "/sales/tickets",
  "timestamp": "2026-10-20T15:30:12Z",
  "correlationId": "9a8b7c6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d",
  "errores": []
}
```

### Excepciones disponibles (`gt.cunoc.proyecto2.comun.errores`)

| Excepción | HTTP | Cuándo usarla | Ejemplo |
| --- | --- | --- | --- |
| `SolicitudInvalidaException` | 400 | Datos con forma correcta pero inválidos para la operación | Fecha de retorno anterior a la de salida |
| `NoAutenticadoException` | 401 | Falta sesión | (normalmente la lanza `comun`) |
| `ProhibidoException` | 403 | Tiene sesión pero no permiso | Admin de otra sucursal |
| `NoEncontradoException` | 404 | El recurso no existe | Bus inexistente |
| `ConflictoException` | 409 | Choca con el estado actual | Asiento ya vendido, placa repetida |
| `ReglaNegocioException` | 422 | Viola una regla del enunciado | Desactivar un bus con viajes programados |
| `ServicioNoDisponibleException` | 503 | Otro servicio no responde | Fallback del circuit breaker (R1–R5) |

Cada una acepta `(codigo, mensaje)` o solo `(mensaje)`. El **código** es para el frontend (en MAYÚSCULAS, estable);
el **mensaje** es para el usuario (en español, claro).

```java
throw new ConflictoException("ASIENTO_OCUPADO", "El asiento 12 ya fue vendido");
throw new ReglaNegocioException("BUS_CON_VIAJES", "No se puede desactivar: el bus tiene viajes programados");
throw new NoEncontradoException("BUS_NO_EXISTE", "El bus no existe");
```

### Lo que `ManejadorGlobalErrores` traduce solo

| Situación | HTTP | Código |
| --- | --- | --- |
| `@Valid` falla en un `@RequestBody` | 400 | `VALIDACION` (con la lista de campos en `errores`) |
| JSON mal formado o tipos incorrectos | 400 | `CUERPO_INVALIDO` |
| Parámetro de URL inválido o faltante | 400 | `PARAMETRO_INVALIDO` |
| Ruta inexistente | 404 | `RUTA_INEXISTENTE` |
| Método HTTP no soportado | 405 | `METODO_NO_PERMITIDO` |
| Violación de `UNIQUE` en la BD (23505) | 409 | `REGISTRO_DUPLICADO` |
| Borrar algo referenciado por FK (23503) | 409 | `REGISTRO_EN_USO` |
| `RAISE EXCEPTION` de un trigger (P0001) | 422 | `REGLA_NEGOCIO` con **el mensaje del trigger** |
| Otro `CHECK` / restricción (23xxx) | 422 | `DATOS_INVALIDOS` |
| Bloqueo optimista (`@Version`) | 409 | `MODIFICACION_CONCURRENTE` |
| Cualquier otra excepción | 500 | `ERROR_INTERNO` (se registra en el log) |

Ejemplo de validación:

```java
public record BusRequest(
        @NotNull UUID sucursalId,
        @NotBlank @Size(max = 10) String placa,
        @Min(1) @Max(120) int capacidadPasajeros) {
}

@PostMapping
public BusResponse crear(@RequestBody @Valid BusRequest request) { ... }
```

```json
{ "status": 400, "codigo": "VALIDACION", "mensaje": "Hay campos con errores",
  "errores": [ { "campo": "placa", "mensaje": "no debe estar vacío" } ] }
```

Los triggers de las migraciones ya protegen reglas del enunciado (último admin de sistema, salida/llegada
inmutables, tipo de viaje inmutable...). Si se viola una, el usuario recibe 422 con el mensaje del trigger
(por ejemplo "No se puede desactivar al último administrador del sistema activo") sin escribir código extra.

**No** atrapar excepciones en el controlador para construir un `ResponseEntity` de error a mano: se pierde el
formato común. Lanzar la excepción y dejar que `comun` responda.

---

## 11. Seguridad: usuario actual y roles

### Recorrido de una petición

```
Petición HTTP (Authorization: Bearer eyJ...)
   │
   ▼
FiltroCorrelacion      → asigna X-Correlation-Id
   ▼
FiltroAutenticacion    → ¿trae "Bearer ..."?
   │                      sí → valida el JWT → guarda UsuarioActual en la petición (inválido → 401 y termina)
   │                      no → sigue como anónimo
   ▼
InterceptorRoles       → ¿el método/clase tiene @RequiereRol?  sin usuario → 401 · sin el rol → 403
   ▼
ResolverUsuarioActual  → entrega UsuarioActual como parámetro del controlador (si no hay usuario → 401)
   ▼
Controlador            → reglas finas: usuario.verificarAccesoSucursal(...)
```

### Niveles de protección

| Necesidad | Cómo |
| --- | --- |
| Endpoint público (login, registro, ver viajes disponibles) | Ni `@RequiereRol` ni parámetro `UsuarioActual` |
| Cualquier usuario con sesión | Parámetro `UsuarioActual` o `@RequiereRol` sin roles |
| Roles específicos | `@RequiereRol({Roles.X, Roles.Y})` (basta con tener uno) |
| Solo su sucursal | Además: `usuario.verificarAccesoSucursal(sucursalId)` (ADMIN_SISTEMA pasa siempre) |

### Ejemplo de controlador

```java
package gt.cunoc.proyecto2.fleet.bus;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fleet/buses")
@RequiereRol({Roles.ADMIN_SUCURSAL, Roles.ADMIN_SISTEMA})       // aplica a todos los métodos de la clase
public class BusController {

    private final BusService servicio;

    public BusController(BusService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusResponse crear(@RequestBody @Valid BusRequest request,
                             @Parameter(hidden = true) UsuarioActual usuario) {
        usuario.verificarAccesoSucursal(request.sucursalId());   // 403 si es de otra sucursal
        return servicio.crear(request);
    }

    @GetMapping
    public List<BusResponse> listar(@RequestParam UUID sucursalId,
                                    @Parameter(hidden = true) UsuarioActual usuario) {
        usuario.verificarAccesoSucursal(sucursalId);
        return servicio.listar(sucursalId);
    }
}
```

- `@Parameter(hidden = true)` (de springdoc) evita que `UsuarioActual` aparezca en Swagger como un parámetro.
- Las reglas de "a quién le pertenece el dato" se validan en el servicio con los datos de `UsuarioActual`; por
  ejemplo, un chofer solo registra la salida de sus viajes:

```java
if (!usuario.tieneRol(Roles.ADMIN_SUCURSAL) && !viaje.getChoferId().equals(usuario.choferId())) {
    throw new ProhibidoException("VIAJE_AJENO", "Solo puede registrar la salida de sus propios viajes");
}
```

### `UsuarioActual`

| Método | Devuelve |
| --- | --- |
| `usuarioId()` | Id del usuario (claim `sub`) |
| `correo()` | Correo |
| `roles()` | `Set<String>` de roles |
| `sucursalId()` | Sucursal (solo ADMIN_SUCURSAL y CAJERO; si no, null) |
| `choferId()` | Id de chofer en Fleet (solo CHOFER; si no, null) |
| `tieneRol(rol)` / `tieneAlgunRol(...)` | true / false |
| `esAdminSistema()` | true / false |
| `verificarAccesoSucursal(id)` | Lanza `ProhibidoException` si no es su sucursal (ADMIN_SISTEMA siempre pasa) |

### Generar el token (solo Identity, en el login)

```java
@PostMapping("/identity/auth/login")
public LoginResponse login(@RequestBody @Valid LoginRequest request) {
    Usuario usuario = autenticacion.verificarCredenciales(request.correo(), request.contrasena());
    String token = servicioJwt.generar(new UsuarioActual(
            usuario.getId(), usuario.getCorreo(), usuario.getRoles(),
            usuario.getSucursalId(), usuario.getChoferId()));
    return new LoginResponse(token /* , refresh token, datos del usuario... */);
}
```

El frontend guarda el token y lo envía en cada petición: `Authorization: Bearer <token>`.

---

## 12. Estructura del JWT

Formato `cabecera.contenido.firma`, cada parte en Base64URL. **No está cifrado** (cualquiera puede leer el contenido,
por ejemplo en jwt.io), solo firmado: nunca poner contraseñas ni datos sensibles.

```json
// cabecera
{ "alg": "HS256", "typ": "JWT" }

// contenido (claims)
{
  "sub": "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90",
  "correo": "admin.xela@correo.com",
  "roles": ["ADMIN_SUCURSAL", "CLIENTE"],
  "sucursalId": "8b1c4f0e-2d3a-4b5c-9e8f-7a6b5c4d3e21",
  "iat": 1792504800,
  "exp": 1792508400
}

// firma = HMAC-SHA256(JWT_SECRET, "cabecera.contenido")
```

| Claim | Se convierte en | Descripción |
| --- | --- | --- |
| `sub` | `usuario.usuarioId()` | **Id del usuario.** Se llama `sub` (subject) porque es el nombre estándar de JWT (RFC 7519) |
| `correo` | `usuario.correo()` | Correo del usuario |
| `roles` | `usuario.roles()` | `ADMIN_SISTEMA`, `ADMIN_SUCURSAL`, `CAJERO`, `CHOFER`, `CLIENTE` |
| `sucursalId` | `usuario.sucursalId()` | Solo admin de sucursal y cajero |
| `choferId` | `usuario.choferId()` | Solo choferes |
| `iat` | — | Emitido en (segundos desde 1970) |
| `exp` | — | Vence en; `iat` + `app.jwt.duracion-minutos` |

**Para qué sirve la firma**: si alguien cambia `"CLIENTE"` por `"ADMIN_SISTEMA"` en el contenido, la firma ya no
coincide, y no puede calcular otra sin conocer `JWT_SECRET`.

### Validación (`ServicioJwt.validar`, la ejecuta `FiltroAutenticacion`)

1. Debe tener 3 partes separadas por `.`.
2. La cabecera debe decir `alg: HS256` (se rechaza `alg: none`, un ataque conocido).
3. Se recalcula la firma con `JWT_SECRET` y se compara en tiempo constante; si no coincide, el token fue alterado.
4. `exp` no debe haber pasado → si pasó, 401 `TOKEN_EXPIRADO`.
5. Se construye el `UsuarioActual`. Cualquier error de formato → 401 `TOKEN_INVALIDO` (nunca 500).

**Por qué cada servicio valida sin preguntarle a Identity**: todos comparten `JWT_SECRET`, así que cada uno comprueba
la firma por su cuenta. Si Identity se cae, quienes ya iniciaron sesión siguen trabajando hasta que venza su token.

**Importante**: `JWT_SECRET` debe ser **el mismo** en los 7 servicios y en el gateway. Si uno tiene otro valor,
ese servicio responderá `TOKEN_INVALIDO` a todo.

---

## 13. Correlación y logs

- Cada petición HTTP trae o recibe un `X-Correlation-Id` (se devuelve en la respuesta).
- Ese id viaja en el `correlationId` de cada evento publicado y se restaura al consumirlo.
- Está en el MDC con la clave `correlationId`. Para verlo en los logs, en `application.yml`:

```yaml
logging:
  pattern:
    level: "%5p [%X{correlationId:-}]"
```

Así se puede seguir una saga completa buscando el mismo id en los logs de Sales e Identity.
El frontend puede enviar su propio `X-Correlation-Id` para relacionar un clic con todo lo que provocó.

---

## 14. Pruebas

### Pruebas de `comun`

```bash
mvn -B -pl comun test
```

| Clase | Qué prueba |
| --- | --- |
| `ServicioJwtTest` | Generar y validar; token alterado; otro secreto; expirado; texto que no es token; secreto corto |
| `UsuarioActualTest` | Acceso por sucursal (admin de sucursal vs admin de sistema) |
| `EventoSobreJsonTest` | El sobre se serializa y se lee sin perder datos; el payload se convierte al record del consumidor |

### Probar un endpoint protegido en un servicio

En una prueba con contexto de Spring, se genera un token real con `ServicioJwt` (usa el `app.jwt.secret` del
`application.yml` de pruebas):

```java
@Autowired ServicioJwt jwt;

String token = jwt.generar(new UsuarioActual(UUID.randomUUID(), "admin@test.com",
        Set.of(Roles.ADMIN_SUCURSAL), sucursalId, null));

// enviar el header en la petición de prueba (MockMvc / cliente HTTP):
// Authorization: Bearer <token>
```

### Probar un manejador de eventos

Los manejadores son clases normales: se pueden probar llamando a `manejar(...)` con un `EventoRecibido` armado a mano.

```java
var evento = new EventoRecibido<>(UUID.randomUUID(), "ViajeProgramado", 1, viajeId, 1L,
        Instant.now(), LocalDate.now(), UUID.randomUUID(),
        new ViajeProgramado(viajeId, "REGULAR", /* ... */));
handler.manejar(evento);
```

### Postman

1. `POST /identity/auth/login` → copiar el `token` de la respuesta.
2. En la colección: *Authorization* → *Bearer Token* → `{{token}}`.

---

## 15. Reglas: qué hacer y qué no

**Sí**

- Publicar eventos solo con `PublicadorEventos`, dentro de `@Transactional`.
- Un `ManejadorEvento` por tipo de evento, con su propio record y `@JsonIgnoreProperties(ignoreUnknown = true)`.
- Enviar la fecha elegida por el usuario como `fechaNegocio`.
- Lanzar las excepciones de `comun.errores` con un código estable y un mensaje claro.
- Proteger endpoints con `@RequiereRol` y validar la sucursal con `UsuarioActual`.
- Revisar el catálogo de eventos (`CLAUDE.md` §8) antes de publicar o consumir.

**No**

- Usar `RabbitTemplate` o `@RabbitListener` directamente para eventos de negocio.
- Poner entidades, repositorios, DTOs o reglas de negocio en `comun`.
- Compartir los records de payload entre servicios.
- Atrapar excepciones dentro de un `ManejadorEvento` para "evitar" el reintento (se perdería el mensaje).
- Lanzar una excepción para una respuesta de negocio válida (por ejemplo saldo insuficiente → publicar `DebitoCarteraRechazado`).
- Construir respuestas de error a mano en los controladores.
- Poner datos sensibles en el JWT o en los eventos.

---

## 16. Modificar `comun`

`comun` afecta a los 7 servicios, así que:

1. Hacer el cambio en una rama `feature/...` y **que el otro integrante revise el PR**.
2. Compilar y probar todo el repositorio, no solo `comun`:
   ```bash
   mvn -B clean install
   ```
3. Cambios compatibles (agregar un método, una excepción, un campo opcional) se pueden hacer en cualquier momento.
   Cambios incompatibles (renombrar una clase, cambiar una firma, cambiar el formato del sobre) obligan a ajustar
   todos los servicios en el mismo PR.
4. Tras el merge, los demás deben hacer `git pull` y `mvn install` para que su `~/.m2` tenga la versión nueva.
5. En producción, el CI redespliega todos los servicios cuando cambia `comun/**`.

---

## 17. Solución de problemas

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `package gt.cunoc.proyecto2.comun... does not exist` | `comun` no está compilado/instalado o falta la dependencia | `mvn install` desde la raíz; revisar `<dependency>` de `comun` en el `pom.xml` del servicio; en NetBeans *Reload POM* |
| El servicio no ve un cambio reciente en `comun` | Usa la versión vieja de `~/.m2` | `mvn -pl comun install` |
| `No qualifying bean of type PublicadorEventos` | Falta `comun.eventos.exchange` en el yml, o falta el paquete de `comun` en `scanBasePackages` | Agregar el exchange (solo si el servicio publica) y revisar la clase principal |
| `IllegalTransactionStateException` al publicar | Se llamó `publicar` fuera de una transacción | Anotar el método de servicio con `@Transactional` |
| Eventos que no salen (`fecha_publicacion` NULL) | RabbitMQ caído, credenciales malas o falta `publisher-confirm-type: correlated` | Revisar `ultimo_error` en `eventos_salida`, el yml y `docker compose ps` |
| El consumidor no recibe un evento | No está en `comun.eventos.suscripciones`, nombre mal escrito o falta `comun.eventos.cola` | Revisar el yml y el panel de RabbitMQ (*Exchanges → Bindings*) |
| Log "No hay manejador para el evento X" | Está suscrito pero no existe su `ManejadorEvento` | Crear el manejador o quitar la suscripción |
| `Hay dos manejadores para el evento X` al arrancar | Dos clases devuelven el mismo `tipoEvento()` | Dejar solo una |
| Mensajes en `<cola>.dlq` | El manejador falló 4 veces o el JSON/payload no es válido | Ver el mensaje en el panel (*Get messages*), corregir el código y volver a enviarlo a la cola |
| `401 TOKEN_INVALIDO` en un servicio pero no en otro | `JWT_SECRET` distinto entre servicios | Usar el mismo valor en todos (`.env`) |
| `401 TOKEN_EXPIRADO` | Pasaron `app.jwt.duracion-minutos` | Volver a iniciar sesión (o usar refresh token) |
| `app.jwt.secret debe tener al menos 32 caracteres` al arrancar | Secreto corto | Usar un secreto de 32+ caracteres |
| Payload con campos en null al consumir | El nombre del campo del record no coincide con el del catálogo | Usar exactamente los nombres del catálogo (camelCase en español) |