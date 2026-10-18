# API Gateway — Proyecto 2 (transporte extraurbano)

Java 21, Spring Boot 4.1.1, Spring Cloud 2025.1.3, WebFlux y Jackson 3.
Módulo `api-gateway` del monorepo `services` (padre `gt.cunoc.proyecto2:services`).
El paquete base es `api_gateway`. No depende de `comun` (reactivo vs Servlet).
Esta fase no agrega ni ejecuta tests automatizados.

## Compilar y arrancar

Desde la raíz del repo (requerido: el gateway es un módulo del reactor):

```sh
mvn -B -pl api-gateway -am package -DskipTests
java -jar api-gateway/target/api-gateway.jar
```

El gateway escucha en `8080`. La seguridad está desactivada por defecto.
Swagger UI: `http://localhost:8080/swagger-ui.html`.
Health: `http://localhost:8080/actuator/health`.

## Rutas

| Entrada al gateway | Destino local | Path enviado |
| --- | --- | --- |
| `/identity/**` | `http://localhost:8081` | Se conserva |
| `/fleet/**` | `http://localhost:8082` | Se conserva |
| `/trips/**` | `http://localhost:8083` | Se conserva |
| `/sales/**` | `http://localhost:8084` | Se conserva |
| `/maintenance/**` | `http://localhost:8085` | Se conserva |
| `/reports/**` | `http://localhost:8086` | Se conserva |
| `/notifications/**` | `http://localhost:8087` | Se conserva |
| `/wallet/**` | `http://localhost:8088` | Se conserva |
| `/v3/api-docs/identity` | identity-service | `/v3/api-docs` |
| `/v3/api-docs/fleet` | fleet-service | `/v3/api-docs` |
| `/v3/api-docs/trips` | trips-service | `/v3/api-docs` |
| `/v3/api-docs/sales` | sales-service | `/v3/api-docs` |
| `/v3/api-docs/maintenance` | maintenance-service | `/v3/api-docs` |
| `/v3/api-docs/reporting` | reporting-service | `/v3/api-docs` |
| `/v3/api-docs/notification` | notification-service | `/v3/api-docs` |
| `/v3/api-docs/wallet` | wallet-service | `/wallet/v3/api-docs` |

Los documentos OpenAPI se seleccionan en Swagger UI y se descargan por el gateway.

## Variables de entorno

No hace falta definir variables para el arranque local sin autenticación.

| Variable | Default local | Uso |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Puerto HTTP |
| `IDENTITY_URL` | `http://localhost:8081` | URI de identity |
| `FLEET_URL` | `http://localhost:8082` | URI de fleet |
| `TRIPS_URL` | `http://localhost:8083` | URI de trips |
| `SALES_URL` | `http://localhost:8084` | URI de sales |
| `MAINTENANCE_URL` | `http://localhost:8085` | URI de maintenance |
| `REPORTING_URL` | `http://localhost:8086` | URI de reporting |
| `NOTIFICATION_URL` | `http://localhost:8087` | URI de notification |
| `WALLET_URL` | `http://localhost:8088` | URI de wallet (cartera digital) |
| `FRONTEND_URL` | `http://localhost:5173` | Origen CORS (también `GATEWAY_ALLOWED_ORIGINS`) |
| `GATEWAY_ALLOWED_ORIGINS` | valor de `FRONTEND_URL` | Orígenes CORS separados por coma |
| `GATEWAY_SECURITY_ENABLED` | `false` | Activa validación JWT HS256 |
| `JWT_SECRET` | secreto de desarrollo (32+ chars) | Mismo secreto HS256 de los 8 servicios y `comun` |
| `GATEWAY_CONNECT_TIMEOUT` | `3000` | Timeout de conexión en ms |
| `GATEWAY_RESPONSE_TIMEOUT` | `30s` | Timeout de respuesta, formato Duration |
| `LOG_LEVEL` | `INFO` | Nivel general de logs |
| `GATEWAY_LOG_LEVEL` | `INFO` | Usar `DEBUG` para diagnóstico del routing |
| `SPRING_PROFILES_ACTIVE` | vacío | Usar `docker` en contenedores |
| `GATEWAY_PORT` | `8080` | Puerto publicado por Compose (compose real) |

El perfil `docker` cambia los destinos a `identity-service:8081`,
`fleet-service:8082`, `trips-service:8083`, `sales-service:8084`,
`maintenance-service:8085`, `reporting-service:8086`,
`notification-service:8087` y `wallet-service:8088`.

### JWT

Con `GATEWAY_SECURITY_ENABLED=false` todo pasa sin validar (gateway transparente).
Con `true`, los endpoints públicos (`/identity/auth/registro|login|refrescar|recuperar|restablecer`,
health, Swagger, `OPTIONS`) quedan permitidos y las demás rutas exigen el JWT
HS256 de `comun` firmado con el mismo `JWT_SECRET` (`Hs256JwtDecoder`). Se
deshabilitan CSRF, login, HTTP Basic, logout, persistencia del contexto y caché
de solicitudes en sesión. Las rutas `/*/interno/**` (R1–R5) se rechazan con 404
(`BloqueoInternoFilter`): solo existen de servicio a servicio.

### CORS y orden de filtros

`CorsConfig.corsConfigurationSource` es la única fuente de política CORS;
Spring Security la aplica antes de autenticar. No se configura `globalcors`.
El filtro `DedupeResponseHeader` elimina valores repetidos que devuelvan los
microservicios para Allow-Origin y Allow-Credentials.

Las cadenas WebFilter y GlobalFilter tienen órdenes independientes:

1. `CorrelationIdFilter`, WebFilter con `HIGHEST_PRECEDENCE`, conserva o genera
   el UUID y fija el header de respuesta al confirmar. Cubre health, Swagger,
   preflight, errores de seguridad y rutas inexistentes.
2. `corsRejectionJsonFilter`, WebFilter con `HIGHEST_PRECEDENCE + 1`, convierte
   el rechazo CORS vacío de Spring en el mismo error JSON 403.
3. Spring Security aplica CORS y, si corresponde, autentica JWT.
4. Para rutas del gateway se ejecutan `CorrelationIdFilter` (orden mínimo),
   `RequestLoggingFilter` (mínimo + 1) y `UserContextFilter` (mínimo + 2).
   Este último ya dispone del principal autenticado y siempre elimina los
   headers `X-User-*` recibidos del cliente.
5. Con JWT, `X-User-Id` toma `sub`, `X-User-Email` toma `correo`,
   `X-Sucursal-Id` toma `sucursalId`, `X-Chofer-Id` toma `choferId` y
   `X-User-Roles` combina `roles` separados por coma. Sin JWT se
   reenvía la petición sin esos headers.
6. El log se escribe antes de confirmar la respuesta, con método, path,
   status final, latencia y correlation id, sin bodies ni Authorization.

`GlobalErrorWebExceptionHandler` tiene orden `-2` y escribe JSON con
`status`, `codigo`, `mensaje`, `ruta`, `timestamp`, `correlationId` y `errores`
(igual que `comun`), usando Jackson 3. Los handlers de seguridad usan el mismo
escritor. Las excepciones
con status conservan ese status; conexión rechazada/DNS/servicio cerrado se
traducen a 503 y timeouts a 504; errores imprevistos a 500. No se incluye la
excepción en el cuerpo. Las respuestas normales de los microservicios,
incluidos sus propios cuerpos de error, se reenvían sin reescribirlas.

## Verificación manual

En Windows PowerShell usar `curl.exe` en lugar del alias `curl`.
Con el gateway en modo local y seguridad desactivada:

```sh
# Criterio 3: 200 sin token
curl -i http://localhost:8080/actuator/health

# Criterio 4: respuesta del microservicio; 503 JSON si no está disponible
curl -i http://localhost:8080/identity/auth/yo

# Criterio 6: el mismo correlation id en la respuesta y en el microservicio
curl -i -H "X-Correlation-Id: fase1-demo" http://localhost:8080/identity/auth/yo

# Criterio 7: preflight 200, Allow-Origin y Allow-Credentials sin duplicados
curl -i -X OPTIONS http://localhost:8080/identity/auth/yo -H "Origin: http://localhost:5173" -H "Access-Control-Request-Method: GET" -H "Access-Control-Request-Headers: Authorization,X-Correlation-Id"

# 404 JSON, incluso si el cliente acepta HTML
curl -i -H "Accept: text/html" http://localhost:8080/no-existe

# 403 JSON por origen CORS no permitido
curl -i -X OPTIONS http://localhost:8080/identity/auth/yo -H "Origin: http://not-allowed.example" -H "Access-Control-Request-Method: GET"
```

Para comprobar 401, detener el proceso y arrancar con
`GATEWAY_SECURITY_ENABLED=true` (PowerShell: `$env:GATEWAY_SECURITY_ENABLED='true'`),
o usar `--gateway.security.enabled=true` al ejecutar el JAR. Sin token,
`curl -i http://localhost:8080/identity/auth/yo` debe devolver 401 JSON.
Health y los preflight siguen respondiendo 200.

## Docker

El JAR se construye con Maven/Java 21 (`mvn -pl api-gateway -am package -DskipTests`);
la imagen runtime solo lleva JRE 21 sobre Alpine, usuario sin privilegios y puerto 8080
(mismo patrón que los 7 microservicios).

```sh
mvn -B -pl api-gateway -am package -DskipTests
docker compose --profile app up -d --build api-gateway
```

`docker-compose.gateway.yml` es solo un fragmento de referencia con las variables
del gateway; el servicio real vive en `/docker-compose.yml` y comparte red con
los microservicios.

## Resultado de validación de esta fase

Validación manual realizada el 4 de octubre de 2026 (America/Guatemala):

- `mvn clean package -DskipTests`: BUILD SUCCESS; tests omitidos.
- Arranque real con Netty en 8080, Boot 4.1.1 y Cloud 2025.1.3, sin error de
  CompatibilityVerifier y sin `spring-webmvc` en el JAR ejecutable.
- Health 200, 404 JSON, 503 JSON, correlación preservada/generada y preflight
  200 sin duplicados; rechazo CORS 403 JSON.
- Identity real vía gateway: `POST /identity/auth/login` 200 público,
  `GET /identity/cartera` con JWT HS256 200, sin token 401 `TOKEN_INVALIDO`,
  `/*/interno/**` 404 aunque lleve token válido, y `/v3/api-docs/identity` 200.
- Mock con demora de 32 segundos: 504 JSON al alcanzar el timeout de 30 segundos,
  conservando el correlation id.
- Instancia con seguridad activada: 401 JSON sin token y con token malformado;
  health y preflight 200.
- `docker compose config --quiet`: configuración válida.

Referencia de configuración: [Spring Cloud Gateway Server WebFlux](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/configuration.html).
El handler usa el paquete Boot 4 documentado:
[`org.springframework.boot.webflux.error`](https://docs.spring.io/spring-boot/4.0/api/java/org/springframework/boot/webflux/error/ErrorWebExceptionHandler.html).
