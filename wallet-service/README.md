# wallet-service

Cartera digital de los clientes. Puerto **8088**, base **bd_cartera** (usuario `usr_cartera`), prefijo del gateway **`/wallet`**.

Antes la cartera vivía dentro de Identity. Se separó porque el dinero es otro contexto de negocio:
otras reglas de auditoría, otra seguridad (una pasarela de pagos real solo toca este servicio) y otra escala que el inicio de sesión.

## Endpoints (todos con rol CLIENTE)

| Código | Método y ruta | Qué hace |
|---|---|---|
| W1 | `GET /wallet/cartera` | Saldo. Si el cliente no tiene cartera, se le abre en cero |
| W2 | `POST /wallet/cartera/recargas` | Recarga: cobra la pasarela configurada y acredita el saldo |
| W3 | `GET /wallet/cartera/movimientos?desde=&hasta=` | Historial de recargas, débitos y reembolsos |

Swagger: `/wallet/swagger-ui.html` (o el agregado del gateway, definición `wallet-service`).

## Eventos

| Dirección | Exchange | Evento | Qué hace la cartera |
|---|---|---|---|
| Recibe | `sales.eventos` | `DebitoCarteraSolicitado` | Verifica la cuenta en Identity (R4c), debita y responde |
| Recibe | `sales.eventos` | `ReembolsoCarteraSolicitado` | Devuelve el monto de un débito previo |
| Recibe | `identity.eventos` | `CuentaCreada` | Si la cuenta es de cliente, le abre la cartera en cero |
| Publica | `wallet.eventos` | `CarteraDebitada` / `DebitoCarteraRechazado` / `CarteraReembolsada` | Respuestas de la saga para Sales |

Los nombres y payloads de la saga son los mismos que publicaba Identity: Sales solo cambió el exchange al que se suscribe.

Llamada síncrona: **R4c** `GET /identity/interno/usuarios/{id}/estado` (token de servicio) antes de debitar.
Si Identity no responde, el evento se reintenta y al final va a `wallet.q.dlq`.

## Pasarela de pagos (servicios de terceros)

Las recargas pasan por la interfaz `pagos.ProveedorPagos`. Hoy está `ProveedorSimulado` (aprueba todo).
Para conectar una pasarela real (Stripe, PayPal, una pasarela local, ...):

1. Crear una clase que implemente `ProveedorPagos` con `@ConditionalOnProperty(prefix = "app.pagos", name = "proveedor", havingValue = "<codigo>")`.
2. Poner `PAGOS_PROVEEDOR=<codigo>` y sus credenciales en el `.env` (nunca en el repositorio).

No se toca el resto del servicio ni los demás microservicios. Cada recarga guarda la pasarela y la referencia del cobro
(`proveedor_pago`, `referencia_externa`); `UNIQUE(proveedor_pago, referencia_externa)` evita acreditar dos veces el mismo cobro
si la pasarela confirma por webhook más de una vez.

## Variables

| Variable | Defecto | Uso |
|---|---|---|
| `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | `localhost`, `bd_cartera`, `usr_cartera`, `cambiar_cartera` | PostgreSQL |
| `RABBIT_*`, `JWT_SECRET` | igual que los demás servicios | Eventos y JWT |
| `IDENTITY_URL` | `http://localhost:8081` | R4c |
| `PAGOS_PROVEEDOR` | `simulado` | Pasarela de las recargas |
