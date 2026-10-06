# Infraestructura y despliegue (Google Cloud)

Guía paso a paso para dejar el proyecto corriendo en **producción**. Se hace **una sola vez**; después cada merge a
`main` despliega solo. Hay un único entorno en la nube (producción); el desarrollo se hace en las máquinas locales.

## Qué es cada pieza

| Pieza | Dónde está | Para qué |
| --- | --- | --- |
| `Dockerfile` de cada servicio | `<servicio>/Dockerfile` | Cómo se construye la **imagen** de ese servicio (solo copia su JAR) |
| `docker-compose.yml` | raíz | Cómo se **ejecutan todos juntos**: 8 servicios Java, RabbitMQ y Mailpit, con sus variables |
| `.env.example` | raíz | Variables para **su máquina** (copiar a `.env`) |
| `infra/servidor/env.prod.plantilla` | aquí | Variables de **producción** en la VM |
| `infra/gcp/01_crear_vm.sh` | aquí | Crea la VM, la IP fija y el firewall en Google Cloud |
| `infra/servidor/02_preparar_servidor.sh` | aquí | Instala Docker y PostgreSQL 16 en la VM y la deja lista |
| `infra/servidor/03_crear_bases.sh` | aquí | Crea las 7 bases de producción y sus usuarios con contraseñas aleatorias |
| `.github/workflows/ci-cd.yml` | raíz | Pruebas en cada PR y en `develop`; imagen + despliegue en cada merge a `main` |

## Cómo queda la VM

```
VM Google Cloud (Ubuntu 24.04, e2-standard-2, 8 GB)        IP fija
├── PostgreSQL 16 (fuera de Docker, puerto 5432 solo interno)
│     bd_identidad, bd_flota, bd_viajes, bd_ventas,
│     bd_mantenimiento, bd_reportes, bd_notificaciones
└── docker compose -p prod   (/opt/proyecto2/.env.prod)
      api-gateway :8080   ← único puerto de la API abierto a internet
      identity, fleet, trips, sales, maintenance, reporting, notification
      rabbitmq (5672 / 15672 solo localhost), mailpit
```

Desde internet solo se llega al gateway (8080), al frontend (80/443) y por SSH (22).
PostgreSQL y RabbitMQ no se exponen.

## Costo

e2-standard-2 (2 vCPU, 8 GB) cuesta aprox. 49 USD al mes. La prueba gratuita de Google Cloud da 300 USD de crédito
por 90 días para cuentas nuevas. **Detengan la VM cuando no la usen**:

```bash
gcloud compute instances stop proyecto2-vm --zone=us-central1-a    # detener (solo se cobra disco e IP)
gcloud compute instances start proyecto2-vm --zone=us-central1-a   # volver a encender (los servicios arrancan solos)
```

---

## Paso 1 · Proyecto y gcloud (en su máquina)

1. Crear el proyecto en <https://console.cloud.google.com> y activar la facturación (prueba gratuita).
2. Instalar Google Cloud CLI: <https://cloud.google.com/sdk/docs/install>
3. Autenticarse y elegir el proyecto:

```bash
gcloud auth login
gcloud config set project <ID_DEL_PROYECTO>
```

## Paso 2 · Crear la VM (en su máquina)

```bash
bash infra/gcp/01_crear_vm.sh
```

Al final imprime la **IP fija** de la VM. Anótenla: es el `VM_HOST` del CI y la dirección del gateway.

## Paso 3 · Preparar el servidor (en la VM)

```bash
gcloud compute scp --recurse infra/servidor proyecto2-vm:~ --zone=us-central1-a
gcloud compute ssh proyecto2-vm --zone=us-central1-a

# ya dentro de la VM:
cd ~/servidor
sudo bash 02_preparar_servidor.sh
```

Instala Docker, Docker Compose y PostgreSQL 16; configura PostgreSQL para que los contenedores lleguen a él
(`listen_addresses='*'` + `pg_hba.conf` solo para la red de Docker `172.16.0.0/12`); crea el usuario `deploy`
(el que usa GitHub Actions), la carpeta `/opt/proyecto2` y un respaldo diario de las bases a las 2:00 a. m.

## Paso 4 · Crear las bases de datos (en la VM)

```bash
sudo bash 03_crear_bases.sh
```

Crea las 7 bases `bd_<x>` con un usuario por servicio y contraseña aleatoria, y al final **imprime las 7 líneas
`DB_*_PASSWORD=...`**. Cópienlas para el paso 5. Las tablas no se crean aquí: las crea Flyway al arrancar cada servicio.

> Si lo ejecutan de nuevo, genera contraseñas nuevas: hay que actualizarlas en `.env.prod` y reiniciar los servicios.

## Paso 5 · Archivo `.env.prod` (en la VM)

```bash
sudo cp ~/servidor/env.prod.plantilla /opt/proyecto2/.env.prod
sudo nano /opt/proyecto2/.env.prod          # reemplazar cada CAMBIAR
sudo chown deploy:deploy /opt/proyecto2/.env.prod
sudo chmod 600 /opt/proyecto2/.env.prod
```

| Variable | Valor |
| --- | --- |
| `DB_*_PASSWORD` | Las 7 líneas que imprimió el paso 4 |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `RABBIT_PASSWORD` | `openssl rand -base64 24` |
| `FRONTEND_URL` | La URL del frontend en producción (CORS del gateway) |
| `MAIL_USER` / `MAIL_PASSWORD` | Cuenta de Gmail y su contraseña de aplicación |

El resto (puertos, etiqueta `latest` de las imágenes) ya viene correcto en la plantilla.

### RabbitMQ en la VM

- RabbitMQ corre en un contenedor del proyecto `prod`, con su propio volumen.
- `RABBIT_USER` y `RABBIT_PASSWORD` se aplican **solo la primera vez** que se crea el volumen. Para cambiarlos después:
  `docker compose -p prod --env-file .env.prod down -v` (borra los mensajes pendientes) y volver a desplegar.
- Los servicios reciben esas mismas variables desde el `docker-compose.yml`, así que siempre coinciden.
- Panel web (no está expuesto a internet), por túnel SSH desde su máquina:

```bash
gcloud compute ssh proyecto2-vm --zone=us-central1-a -- -L 15672:localhost:15672
# luego abrir http://localhost:15672
```

## Paso 6 · Llave SSH para GitHub Actions (en su máquina)

```bash
ssh-keygen -t ed25519 -C "github-actions" -f gh-actions -N ""
cat gh-actions.pub        # copiar esta línea (pública)
```

En la VM, pegar la línea pública:

```bash
sudo nano /home/deploy/.ssh/authorized_keys
```

Probar desde su máquina: `ssh -i gh-actions deploy@<IP_DE_LA_VM> docker ps`

## Paso 7 · Secrets del repositorio (GitHub)

*Settings → Secrets and variables → Actions → New repository secret*

| Secret | Valor |
| --- | --- |
| `VM_HOST` | IP fija de la VM (paso 2) |
| `VM_USER` | `deploy` |
| `VM_SSH_KEY` | Contenido completo de `gh-actions` (la llave **privada**, incluidas las líneas BEGIN/END) |

Después borren `gh-actions` de su máquina o guárdenla en un lugar seguro. **Nunca** la suban al repositorio.

`GITHUB_TOKEN` no hay que crearlo: GitHub lo da en cada ejecución. Sirve para publicar las imágenes en
`ghcr.io/eilergomez/...` y para que la VM las descargue durante el despliegue.

## Paso 8 · Primer despliegue

1. Ir a *Actions → CI/CD servicios → Run workflow*, elegir la rama **`main`** y ejecutar.
   Esto despliega los 8 servicios. (Requiere que `main` ya tenga el código: PR de `develop` a `main` y merge.)
2. Revisar en la VM:

```bash
cd /opt/proyecto2
docker compose -p prod --env-file .env.prod --profile app ps
docker compose -p prod --env-file .env.prod --profile app logs -f fleet-service
curl http://localhost:8080/actuator/health
```

3. Desde internet: `http://<IP_DE_LA_VM>:8080`.

## Cómo funciona el CI/CD

| Evento | Qué hace |
| --- | --- |
| Pull Request a `develop` o `main` | `mvn verify` de todo el repositorio (incluye la cobertura mínima de `comun`). No despliega |
| Merge a `develop` | `mvn verify` de todo el repositorio. No despliega |
| Merge a `main` | Por cada servicio cambiado: `mvn verify` → imagen `:latest` → despliegue en producción |
| Ejecución manual en `main` | Despliega los 8 servicios |

- Un cambio en `comun/`, en el `pom.xml` padre o en `docker-compose.yml` redespliega **todos** los servicios.
- En la VM se reemplaza **solo el contenedor del servicio** (`up -d --no-deps`); los demás siguen funcionando.
- Cada imagen se publica también con la etiqueta del commit (`:<sha>`) para poder volver a una versión anterior.

## Comandos útiles en la VM

```bash
cd /opt/proyecto2
P="docker compose -p prod --env-file .env.prod --profile app"
$P ps                       # estado
$P logs -f <servicio>       # logs
$P restart <servicio>       # reiniciar uno
$P down                     # detener todo (conserva datos)
free -h                     # memoria disponible
sudo -u postgres psql -l    # bases de datos
sudo ls /var/lib/postgresql/respaldo_*   # respaldos
```

## Solución de problemas

| Síntoma | Causa probable | Solución |
| --- | --- | --- |
| `password authentication failed for user "usr_x"` | La contraseña de `.env.prod` no coincide con la de PostgreSQL | Copiar de nuevo las líneas del paso 4 o volver a ejecutarlo |
| `Connection refused` a PostgreSQL desde un contenedor | Falta `listen_addresses` o la línea de `pg_hba.conf` | Volver a ejecutar `02_preparar_servidor.sh` |
| `ACCESS_REFUSED` de RabbitMQ | El volumen se creó con otra contraseña | `down -v` y volver a desplegar |
| El CI falla en "Desplegar en la VM" con `Permission denied (publickey)` | La llave pública no está en `authorized_keys` del usuario `deploy` | Repetir el paso 6 |
| `denied` al hacer `pull` en la VM | El paquete de la imagen no está vinculado al repositorio | Verificar el `LABEL org.opencontainers.image.source` del Dockerfile |
| Un servicio se reinicia solo (`Exited (137)`) | Falta memoria | `free -h`; subir a e2-standard-4 |
| El gateway no responde desde internet | Firewall de GCP | `gcloud compute firewall-rules describe proyecto2-publico` |
