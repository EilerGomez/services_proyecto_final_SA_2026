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
