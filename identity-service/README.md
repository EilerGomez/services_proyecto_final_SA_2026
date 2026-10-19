# Mensajeria de identity

La infraestructura RabbitMQ permanece en `comun`. Este modulo separa los contratos
de mensajes, los adaptadores de mensajeria y las operaciones de negocio:

```text
evento/
  payload/                  Mensajes publicados
  recibido/                 Mensajes recibidos
messaging/
  publisher/                Construccion de payloads y publicacion mediante outbox
  consumer/                 Adaptacion de mensajes a llamadas de negocio
services/
  personal/                 Cuentas y sincronizacion de sucursales, roles y sesiones
  auth/, usuario/, tarifa/  Operaciones de negocio existentes
```

Los Services reciben datos de negocio, no `EventoRecibido` ni payloads RabbitMQ.
Cuando una operacion genera un evento, llaman a un publisher especifico.
Los publishers construyen el payload y delegan en `PublicadorEventos` de `comun`.
Este guarda el evento en `eventos_salida` dentro de la misma transaccion;
`RelayOutbox` lo envia posteriormente a RabbitMQ.

Los consumers implementan `ManejadorEvento` y son descubiertos por el unico
`ConsumidorEventos` compartido. No se deben agregar listeners independientes a
`identity.q`: el listener existente selecciona el manejador por tipo y coordina
la idempotencia, correlacion, reintentos y errores.

La cartera digital (saldo, recargas y la saga de debito/reembolso con Sales) ya no
esta en identity: es el microservicio `wallet-service` (base `bd_cartera`, exchange
`wallet.eventos`). Identity solo publica `CuentaCreada`, con la que wallet abre la
cartera de los clientes, y responde R4c (`/identity/interno/usuarios/{id}/estado`)
para que wallet verifique la cuenta antes de debitar.

`SincronizacionPersonalService` usa `@Transactional` con propagacion predeterminada
`REQUIRED`, por lo que participa en la transaccion de `ProcesadorIdempotente`.

La topologia sigue definida por `ConfiguracionEventos` de `comun` y los valores
de `application.yml`; no existe una segunda configuracion RabbitMQ en identity.

Desde la raiz del repositorio:

```sh
mvn -pl identity-service -am verify
```

`IdentityMessagingTest` verifica el recorrido del listener compartido hasta el
outbox y el registro Spring de los tres consumers de personal (ChoferDesactivado,
PersonalSucursalAsignado, PersonalSucursalDesactivado). Usa repositorios y conexion JDBC
simulados; no reemplaza una prueba con PostgreSQL y RabbitMQ reales.
`PublishersTest` verifica los contratos de los eventos de salida de identity.

## Roles para cuentas existentes

Una persona que ya se registro (cliente) puede recibir un rol administrativo sin crear otra cuenta.
Conserva su contrasena, perfil y cartera; solo se le agregan el rol (y la sucursal):

| Endpoint | Quien | Para que |
|---|---|---|
| `GET /identity/usuarios/por-correo?correo=` | ADMIN_SISTEMA, ADMIN_SUCURSAL | Buscar la cuenta por correo exacto. Devuelve datos minimos y `puedeSerPersonal` / `motivo`. |
| `POST /identity/interno/cuentas-personal/existente` (R5b) | SERVICIO (Fleet) | Validar la cuenta para ADMIN_SUCURSAL o CAJERO. Idempotente por `solicitudId`, igual que R5. No cambia la cuenta: el rol llega con `PersonalSucursalAsignado`, que Fleet publica en la misma transaccion en que guarda la fila (si Fleet revierte, la cuenta no queda con un rol sin respaldo). |
| `POST /identity/usuarios/{id}/administrador-sistema` | ADMIN_SISTEMA | Hacer administrador del sistema a una cuenta existente. |

Las reglas estan en `ReglasAsignacionRol`: la cuenta debe estar activa (o con el bloqueo vencido),
no puede ser chofer, el personal de sucursal no puede ser administrador del sistema (y al reves) y
nadie trabaja en dos sucursales a la vez.

Cuando una cuenta recibe un rol que no tenia se publica `RolAsignado {usuarioId, correo, nombre, rol,
sucursalId, sucursalNombre}`: lo hace la promocion a administrador del sistema y `SincronizacionPersonalService`
al procesar `PersonalSucursalAsignado` (alta con cuenta existente, traslado o reintegro). En las altas por R5
(cuenta nueva) el rol ya esta puesto y basta la bienvenida. Notification envia el correo "se te asigno como ...
cierra sesion y vuelve a iniciar sesion": el JWT vigente no tiene el rol nuevo hasta que la persona vuelve a entrar.

R4c (`/identity/interno/usuarios/{id}/estado`) devuelve ademas `impedimentoPersonal` (es administrador del
sistema o chofer) para que Fleet no reintegre a quien ya no puede ser personal.

`CuentaCreada` lleva ademas `sucursalNombre` (opcional, lo manda Fleet en R5) para que la bienvenida
diga el puesto y la sucursal.
