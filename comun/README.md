# comun

Librería técnica compartida por los 7 microservicios (no por el api-gateway, que es WebFlux).
**Solo infraestructura, nunca entidades ni reglas de negocio.**

| Paquete | Qué trae |
| --- | --- |
| `eventos` | Sobre de evento, outbox (`PublicadorEventos` + `RelayOutbox`), consumidor (`ConsumidorEventos` + `ManejadorEvento`), topología RabbitMQ desde `application.yml`, id de correlación |
| `idempotencia` | `ProcesadorIdempotente`: cada evento se procesa una sola vez (`eventos_procesados`) |
| `errores` | `ErrorRespuesta`, excepciones de negocio y `ManejadorGlobalErrores` (incluye errores de triggers) |
| `seguridad` | `ServicioJwt` (HS256), filtro que lee el JWT, `UsuarioActual`, `@RequiereRol`, `Roles` |

## Imports (paquetes)

```java
// eventos
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.comun.eventos.sobre.EventoSobre;
import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
// (internos: eventos.outbox.RelayOutbox, eventos.consumidor.ConsumidorEventos,
//  eventos.configuracion.ConfiguracionEventos, eventos.propiedadesevento.PropiedadesEventos)

// idempotencia
import gt.cunoc.proyecto2.comun.idempotencia.ProcesadorIdempotente;

// errores
import gt.cunoc.proyecto2.comun.errores.*;   // NoEncontradoException, ConflictoException, ...

// seguridad
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;          // solo Identity (login)
// (internos: seguridad.filtro.*, seguridad.config.ConfiguracionSeguridadWeb,
//  seguridad.users.InterceptorRoles, seguridad.users.ResolverUsuarioActual)
```

## Requisitos en cada servicio

- `@SpringBootApplication(scanBasePackages = {"gt.cunoc.proyecto2.<servicio>", "gt.cunoc.proyecto2.comun"})`
- Tablas `eventos_salida` (si publica) y `eventos_procesados` (si consume) en su migración V1.
- `spring.rabbitmq.publisher-confirm-type: correlated` y `app.jwt.secret` (≥ 32 caracteres).
- Bloque `comun.eventos` en `application.yml`:

```yaml
comun:
  eventos:
    exchange: sales.eventos      # solo si publica
    cola: sales.q                # solo si consume
    suscripciones:               # exchange origen → eventos (routing keys)
      "[trips.eventos]": [TripScheduled, TripUpdated, TripDeleted]
```

Al arrancar se declaran en RabbitMQ el exchange propio, la cola, su DLQ `<cola>.dlq` y los bindings.

## Publicar un evento (outbox)

Siempre dentro de un método `@Transactional`, junto con el cambio de negocio:

```java
@Transactional
public Venta confirmar(UUID ventaId, LocalDate fechaPago) {
    Venta venta = repositorio.findById(ventaId).orElseThrow(() -> new NoEncontradoException("Venta no existe"));
    venta.confirmar();
    publicador.publicar("Venta", venta.getId(), "TicketSold",
            new BoletoVendido(venta.getId(), venta.getViajeId(), /* ... campos del catálogo */),
            fechaPago);                       // businessDate = fecha elegida por el usuario
    return venta;
}
```

Si la transacción se revierte, el evento tampoco se publica. `RelayOutbox` lo envía a RabbitMQ cada segundo
y solo lo marca como publicado cuando el broker confirma.

## Consumir un evento

Un `@Component` por tipo de evento; el payload es un record propio con solo los campos que se usan:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeProgramado(UUID viajeId, String tipo, int capacidad, BigDecimal precioBoleto, Instant fechaSalida) {}

@Component
public class ViajeProgramadoHandler implements ManejadorEvento<ViajeProgramado> {
    public String tipoEvento() { return "TripScheduled"; }
    public Class<ViajeProgramado> tipoPayload() { return ViajeProgramado.class; }
    public void manejar(EventoRecibido<ViajeProgramado> evento) {
        // upsert en vista_viajes; si evento.aggregateVersion() <= versión guardada, ignorar
    }
}
```

- Corre en una transacción; un evento repetido no vuelve a ejecutarse.
- Si lanza una excepción: 4 intentos (1 s, 2 s, 4 s) y luego a la DLQ.
- JSON inválido o payload incompatible: directo a la DLQ.

## Errores

Lanzar excepciones de `gt.cunoc.proyecto2.comun.errores`; el manejador global responde con el formato común:

| Excepción | HTTP |
| --- | --- |
| `SolicitudInvalidaException` | 400 |
| `NoAutenticadoException` | 401 |
| `ProhibidoException` | 403 |
| `NoEncontradoException` | 404 |
| `ConflictoException` | 409 |
| `ReglaNegocioException` | 422 |
| `ServicioNoDisponibleException` | 503 (fallback de circuit breaker) |

```java
throw new ConflictoException("ASIENTO_OCUPADO", "El asiento 12 ya fue vendido");
```

También se traducen solos: validaciones `@Valid` (400 con lista de campos), JSON mal formado (400),
duplicados de BD (409), `RAISE EXCEPTION` de triggers (422 con el mensaje del trigger) y bloqueo optimista (409).

## Seguridad

```java
@RestController
@RequestMapping("/fleet/buses")
public class BusController {

    @PostMapping
    @RequiereRol({Roles.ADMIN_SUCURSAL, Roles.ADMIN_SISTEMA})
    public BusResponse crear(@RequestBody @Valid BusRequest request,
                             @Parameter(hidden = true) UsuarioActual usuario) {
        usuario.verificarAccesoSucursal(request.sucursalId());
        return servicio.crear(request);
    }
}
```

- Sin token en un endpoint con `@RequiereRol` o con parámetro `UsuarioActual` → 401; rol insuficiente → 403.
- Endpoints sin ninguno de los dos son públicos (login, registro, consultas abiertas).
- `@Parameter(hidden = true)` (springdoc) evita que `UsuarioActual` aparezca en Swagger.
- Identity genera el token con `servicioJwt.generar(new UsuarioActual(...))` al hacer login.

## Estructura del JWT

Formato `cabecera.contenido.firma`, cada parte en Base64URL. **No está cifrado** (cualquiera puede leer el contenido),
solo firmado: nunca poner contraseñas ni datos sensibles.

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
| `roles` | `usuario.roles()` | Roles (`Roles.ADMIN_SISTEMA`, `ADMIN_SUCURSAL`, `CAJERO`, `CHOFER`, `CLIENTE`) |
| `sucursalId` | `usuario.sucursalId()` | Solo admin de sucursal y cajero; si no, no viene (null) |
| `choferId` | `usuario.choferId()` | Solo choferes; si no, no viene (null) |
| `iat` | — | Emitido en (segundos desde 1970) |
| `exp` | — | Vence en; `iat` + `app.jwt.duracion-minutos` (60 por defecto) |

Validación (`ServicioJwt.validar`, la ejecuta `FiltroAutenticacion` en cada petición con `Authorization: Bearer ...`):

1. Debe tener 3 partes separadas por `.`.
2. La cabecera debe decir `alg: HS256` (se rechaza `alg: none`).
3. Se recalcula la firma con `JWT_SECRET` y se compara en tiempo constante; si no coincide, el token fue alterado.
4. `exp` no debe haber pasado (`TOKEN_EXPIRADO`).
5. Se construye el `UsuarioActual`. Cualquier error de formato → 401 `TOKEN_INVALIDO`.

Todos los servicios comparten `JWT_SECRET`, así que validan el token sin consultar a Identity.
Solo Identity lo genera, en el login:

```java
String token = servicioJwt.generar(new UsuarioActual(id, correo, roles, sucursalId, choferId));
```

Orden por el que pasa cada petición:

```
FiltroCorrelacion → FiltroAutenticacion → InterceptorRoles (@RequiereRol) → ResolverUsuarioActual → controlador
```

## Correlación

`X-Correlation-Id` entra por HTTP (o se genera), viaja en `correlationId` de cada evento publicado y se pone en
el MDC de los logs (`%X{correlationId}`), así se sigue una saga completa entre servicios.