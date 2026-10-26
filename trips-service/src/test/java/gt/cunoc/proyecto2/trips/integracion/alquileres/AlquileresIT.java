package gt.cunoc.proyecto2.trips.integracion.alquileres;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.trips.integracion.soporte.PruebaIntegracion;
import gt.cunoc.proyecto2.trips.integracion.soporte.ServidorFalso;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** T16–T22: alquileres privados, cotización (evento) y asignación con Fleet falso (viaje PRIVADO). */
class AlquileresIT extends PruebaIntegracion {

    private static final String ALQUILERES = "/trips/alquileres";
    private static final OffsetDateTime LLEGADA_ALQUILER = SALIDA_ALQUILER.plusHours(8);

    private UUID xela;
    private UUID guate;

    @BeforeEach
    void datos() {
        xela = insertarSucursal("Quetzaltenango", true);
        guate = insertarSucursal("Guatemala", true);
        insertarPrecioAlquiler("15.00", LocalDate.of(2020, 1, 1));     // vigente para cualquier fecha de solicitud
    }

    /** fechaSolicitud: hoy en Guatemala o después (el servicio rechaza fechas pasadas). */
    private static String nuevoAlquiler(UUID sucursalId, String nit, String correo, OffsetDateTime retorno,
                                        LocalDate fechaSolicitud) {
        return """
                {"sucursalId":"%s","clienteNit":%s,"clienteNombre":"  Maria Lopez ","clienteCorreo":%s,
                 "origen":"Xela","destino":"Antigua","fechaSalida":"%s","fechaRetorno":%s,
                 "cantidadPasajeros":30,"kmEstimados":180.50,"fechaSolicitud":"%s"}
                """.formatted(sucursalId, texto(nit), texto(correo), SALIDA_ALQUILER, texto(retorno), fechaSolicitud);
    }

    private static String texto(Object valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private static String cotizacion(String precio, int version) {
        return "{\"precioConfirmado\":%s,\"version\":%d}".formatted(precio, version);
    }

    private static String cancelacion(String motivo, int version) {
        return "{\"motivo\":\"%s\",\"version\":%d}".formatted(motivo, version);
    }

    private static String asignacion(UUID busId, UUID choferId, int version) {
        return "{\"busId\":\"%s\",\"choferId\":\"%s\",\"fechaLlegadaEstimada\":\"%s\",\"version\":%d}"
                .formatted(busId, choferId, LLEGADA_ALQUILER, version);
    }

    // ----- T16 -----

    @Test
    void unClienteSolicitaEnLineaConElPrecioEstimado() {
        UUID cliente = UUID.randomUUID();

        ResponseEntity<String> respuesta = post(ALQUILERES, tokenCliente(cliente),
                nuevoAlquiler(xela, null, null, SALIDA_ALQUILER.plusDays(1), hoy()));

        esStatus(respuesta, 201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.canal")).isEqualTo("EN_LINEA");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.clienteUsuarioId")).isEqualTo(cliente.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.clienteNit")).isEqualTo("CF");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.clienteNombre")).isEqualTo("Maria Lopez");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.clienteCorreo")).isEqualTo("cliente@trips.gt");
        assertThat(decimal(respuesta, "$.precioEstimado")).isEqualByComparingTo("2707.50");     // 180.50 × 15.00
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("SOLICITADO");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.fechaSolicitud")).isEqualTo(hoy().toString());
        assertThat(fecha(respuesta, "$.fechaSalida")).isEqualTo(SALIDA_ALQUILER);
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.version")).isZero();
        assertThat(contar("alquileres_privados")).isEqualTo(1);
    }

    @Test
    void unCajeroSolicitaEnMostradorConUnNitQueNoEsDeNadie() {
        ResponseEntity<String> respuesta = post(ALQUILERES, tokenCajero(xela),
                nuevoAlquiler(xela, " 1234567-k ", "Maria@Correo.GT", null, hoy().plusDays(1)));

        esStatus(respuesta, 201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.canal")).isEqualTo("PRESENCIAL");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.clienteNit")).isEqualTo("1234567-K");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.clienteCorreo")).isEqualTo("maria@correo.gt");
        String alquilerId = json(respuesta, "$.alquilerId");
        assertThat(jdbc.queryForObject("SELECT cliente_usuario_id IS NULL FROM alquileres_privados WHERE id = ?",
                Boolean.class, UUID.fromString(alquilerId))).isTrue();
    }

    @Test
    void laVentanillaLigaElAlquilerALaCuentaDelNitYLoVeEnMisAlquileres() {
        UUID cliente = UUID.randomUUID();
        insertarCliente(cliente, "1234567-K", "María López", "maria@correo.gt");

        // El cajero lo pide en Guate aunque trabaje en Xela: el alquiler sale de donde elija el cliente
        ResponseEntity<String> respuesta = post(ALQUILERES, tokenCajero(xela),
                nuevoAlquiler(guate, "1234567-k", null, null, hoy()));

        esStatus(respuesta, 201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.canal")).isEqualTo("PRESENCIAL");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.clienteUsuarioId")).isEqualTo(cliente.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.sucursalId")).isEqualTo(guate.toString());

        // Y por eso le aparece al cliente en "Mis alquileres", aunque lo pidió la ventanilla
        ResponseEntity<String> mios = get(ALQUILERES + "/mios", tokenCliente(cliente));
        esStatus(mios, 200);
        assertThat(PruebaIntegracion.<List<String>>json(mios, "$[*].alquilerId"))
                .containsExactly(json(respuesta, "$.alquilerId"));
    }

    @Test
    void elAdministradorDeSucursalTambienSolicitaEnVentanilla() {
        ResponseEntity<String> respuesta = post(ALQUILERES, tokenAdminSucursal(xela),
                nuevoAlquiler(guate, null, null, null, hoy()));

        esStatus(respuesta, 201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.canal")).isEqualTo("PRESENCIAL");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.sucursalId")).isEqualTo(guate.toString());
    }

    @Test
    void fechasInvalidasEnLaSolicitudSon400() {
        esError(post(ALQUILERES, tokenCliente(UUID.randomUUID()),
                nuevoAlquiler(xela, null, null, null, hoy().minusDays(1))), 400, "FECHA_SOLICITUD_PASADA");
        esError(post(ALQUILERES, tokenCliente(UUID.randomUUID()),
                nuevoAlquiler(xela, null, null, SALIDA_ALQUILER.minusDays(1), hoy())), 400, "RETORNO_ANTES_DE_SALIDA");
        esError(post(ALQUILERES, tokenCliente(UUID.randomUUID()),
                nuevoAlquiler(xela, null, "no-es-correo", null, hoy())), 400, "VALIDACION");
        assertThat(contar("alquileres_privados")).isZero();
    }

    @Test
    void sucursalInactivaOSinPrecioVigenteEs422() {
        UUID coban = insertarSucursal("Coban", false);

        esError(post(ALQUILERES, tokenCliente(UUID.randomUUID()), nuevoAlquiler(coban, null, null, null, hoy())),
                422, "SUCURSAL_INACTIVA");

        jdbc.execute("TRUNCATE TABLE configuracion_alquiler");
        insertarPrecioAlquiler("15.00", hoy().plusDays(30));
        esError(post(ALQUILERES, tokenCliente(UUID.randomUUID()), nuevoAlquiler(xela, null, null, null, hoy())),
                422, "SIN_PRECIO_ALQUILER");
    }

    // ----- T17 y T18 -----

    @Test
    void unClienteMarcaElRecorridoEnElMapa() {
        UUID cliente = UUID.randomUUID();
        String cuerpo = """
                {"sucursalId":"%s","clienteNombre":"Maria Lopez","origen":"Xela","destino":"Antigua",
                 "fechaSalida":"%s","cantidadPasajeros":30,"kmEstimados":180.50,"fechaSolicitud":"%s",
                 "origenLatitud":14.8347,"origenLongitud":-91.5184,
                 "destinoLatitud":14.5586,"destinoLongitud":-90.7295}
                """.formatted(xela, SALIDA_ALQUILER, hoy());

        ResponseEntity<String> respuesta = post(ALQUILERES, tokenCliente(cliente), cuerpo);

        esStatus(respuesta, 201);
        assertThat(decimal(respuesta, "$.origenLatitud")).isEqualByComparingTo("14.8347");
        assertThat(decimal(respuesta, "$.origenLongitud")).isEqualByComparingTo("-91.5184");
        assertThat(decimal(respuesta, "$.destinoLatitud")).isEqualByComparingTo("14.5586");
        assertThat(decimal(respuesta, "$.destinoLongitud")).isEqualByComparingTo("-90.7295");
    }

    @Test
    void coordenadasIncompletasSon400() {
        String cuerpo = """
                {"sucursalId":"%s","clienteNombre":"Maria Lopez","origen":"Xela","destino":"Antigua",
                 "fechaSalida":"%s","cantidadPasajeros":30,"kmEstimados":180.50,"fechaSolicitud":"%s",
                 "origenLatitud":14.8347}
                """.formatted(xela, SALIDA_ALQUILER, hoy());

        esError(post(ALQUILERES, tokenCliente(UUID.randomUUID()), cuerpo), 400, "COORDENADAS_INCOMPLETAS");
        assertThat(contar("alquileres_privados")).isZero();
    }

    @Test
    void elPersonalListaLosAlquileresDeSuSucursal() {
        UUID solicitado = insertarAlquiler(xela, UUID.randomUUID(), "SOLICITADO", 30, SALIDA_ALQUILER);
        UUID cotizado = insertarAlquiler(xela, UUID.randomUUID(), "COTIZADO", 30, SALIDA_ALQUILER.minusDays(5));
        UUID deGuate = insertarAlquiler(guate, UUID.randomUUID(), "SOLICITADO", 30, SALIDA_ALQUILER);

        ResponseEntity<String> deXela = get(ALQUILERES, tokenAdminSucursal(xela));
        ResponseEntity<String> cotizados = get(ALQUILERES + "?estado=COTIZADO", tokenCajero(xela));
        ResponseEntity<String> delDia = get(ALQUILERES + "?desde=2026-12-20&hasta=2026-12-20", tokenCajero(xela));
        ResponseEntity<String> todos = get(ALQUILERES, tokenAdminSistema());
        ResponseEntity<String> soloGuate = get(ALQUILERES + "?sucursalId=" + guate, tokenAdminSistema());

        esStatus(deXela, 200);
        assertThat(PruebaIntegracion.<List<String>>json(deXela, "$[*].alquilerId"))      // por fecha de salida
                .containsExactly(cotizado.toString(), solicitado.toString());
        assertThat(PruebaIntegracion.<List<String>>json(cotizados, "$[*].alquilerId")).containsExactly(cotizado.toString());
        assertThat(PruebaIntegracion.<List<String>>json(delDia, "$[*].alquilerId")).containsExactly(solicitado.toString());
        assertThat(PruebaIntegracion.<List<Object>>json(todos, "$")).hasSize(3);
        assertThat(PruebaIntegracion.<List<String>>json(soloGuate, "$[*].alquilerId")).containsExactly(deGuate.toString());
    }

    @Test
    void listarAlquileresDeOtraSucursalOConRangoInvalidoFalla() {
        esError(get(ALQUILERES + "?sucursalId=" + guate, tokenCajero(xela)), 403, "SUCURSAL_AJENA");
        esError(get(ALQUILERES + "?desde=2026-12-21&hasta=2026-12-20", tokenAdminSistema()),
                400, "RANGO_FECHAS_INVALIDO");
        esError(get(ALQUILERES, tokenCliente(UUID.randomUUID())), 403, "ROL_INSUFICIENTE");
    }

    @Test
    void elClienteVeSoloSusAlquileres() {
        UUID cliente = UUID.randomUUID();
        UUID primero = insertarAlquiler(xela, cliente, "SOLICITADO", 30, SALIDA_ALQUILER);
        UUID segundo = insertarAlquiler(guate, cliente, "CANCELADO", 30, SALIDA_ALQUILER.plusDays(10));
        insertarAlquiler(xela, UUID.randomUUID(), "SOLICITADO");

        ResponseEntity<String> respuesta = get(ALQUILERES + "/mios", tokenCliente(cliente));
        ResponseEntity<String> sinAlquileres = get(ALQUILERES + "/mios", tokenCliente(UUID.randomUUID()));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$[*].alquilerId"))   // la salida más lejana primero
                .containsExactly(segundo.toString(), primero.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$[0].motivoCancelacion"))
                .isEqualTo("Cambio de planes");
        assertThat(PruebaIntegracion.<List<Object>>json(sinAlquileres, "$")).isEmpty();
        esError(get(ALQUILERES + "/mios", tokenCajero(xela)), 403, "ROL_INSUFICIENTE");
    }

    // ----- T19 -----

    @Test
    void elDetalleLoVenElClienteElPersonalDeLaSucursalYElAdminDelSistema() {
        UUID cliente = UUID.randomUUID();
        UUID alquiler = insertarAlquiler(xela, cliente, "SOLICITADO");
        String detalle = ALQUILERES + "/" + alquiler;

        ResponseEntity<String> delCliente = get(detalle, tokenCliente(cliente));

        esStatus(delCliente, 200);
        assertThat(PruebaIntegracion.<String>json(delCliente, "$.alquilerId")).isEqualTo(alquiler.toString());
        assertThat(PruebaIntegracion.<String>json(delCliente, "$.origen")).isEqualTo("Xela");
        esStatus(get(detalle, tokenCajero(xela)), 200);
        esStatus(get(detalle, tokenAdminSucursal(xela)), 200);
        esStatus(get(detalle, tokenAdminSistema()), 200);
        esError(get(detalle, tokenCliente(UUID.randomUUID())), 403, "ALQUILER_AJENO");
        esError(get(detalle, tokenAdminSucursal(guate)), 403, "ALQUILER_AJENO");
        esError(get(ALQUILERES + "/" + UUID.randomUUID(), tokenAdminSistema()), 404, "ALQUILER_NO_EXISTE");
        esError(get(detalle, null), 401, "NO_AUTENTICADO");
    }

    // ----- T20 -----

    @Test
    void cotizarConfirmaElPrecioYPublicaCotizacionAlquilerConfirmada() {
        UUID alquiler = insertarAlquiler(xela, UUID.randomUUID(), "SOLICITADO");
        UUID admin = UUID.randomUUID();

        ResponseEntity<String> respuesta = post(ALQUILERES + "/" + alquiler + "/cotizacion",
                token(admin, Roles.ADMIN_SUCURSAL, xela, null), cotizacion("2500.00", 0));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("COTIZADO");
        assertThat(decimal(respuesta, "$.precioConfirmado")).isEqualByComparingTo("2500.00");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.confirmadoPor")).isEqualTo(admin.toString());
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.version")).isEqualTo(1);
        assertThat(estadoDelAlquiler(alquiler)).isEqualTo("COTIZADO");

        String evento = esperarEventos("CotizacionAlquilerConfirmada").get("CotizacionAlquilerConfirmada");
        assertThat(JsonPath.<String>read(evento, "$.aggregateId")).isEqualTo(alquiler.toString());
        assertThat(versionDelEvento(evento)).isEqualTo(1);
        assertThat(decimal(evento, "$.payload.precioConfirmado")).isEqualByComparingTo("2500.00");
        assertThat(JsonPath.<String>read(evento, "$.payload.clienteNombre")).isEqualTo("Maria Lopez");
        assertThat(JsonPath.<String>read(evento, "$.payload.canal")).isEqualTo("EN_LINEA");
        assertThat(fecha(evento, "$.payload.fechaSalida")).isEqualTo(SALIDA_ALQUILER);
    }

    @Test
    void cotizarUnAlquilerQueNoEstaSolicitadoOConVersionViejaFalla() {
        UUID cotizado = insertarAlquiler(xela, UUID.randomUUID(), "COTIZADO");
        UUID solicitado = insertarAlquiler(xela, UUID.randomUUID(), "SOLICITADO");
        String token = tokenAdminSucursal(xela);

        esError(post(ALQUILERES + "/" + cotizado + "/cotizacion", token, cotizacion("2500.00", 0)),
                422, "ESTADO_INVALIDO");
        esError(post(ALQUILERES + "/" + solicitado + "/cotizacion", token, cotizacion("2500.00", 4)),
                409, "MODIFICACION_CONCURRENTE");
        esError(post(ALQUILERES + "/" + solicitado + "/cotizacion", token, cotizacion("0", 0)),
                400, "VALIDACION");
        esError(post(ALQUILERES + "/" + solicitado + "/cotizacion", tokenAdminSucursal(guate),
                cotizacion("2500.00", 0)), 403, "SUCURSAL_AJENA");
        esError(post(ALQUILERES + "/" + solicitado + "/cotizacion", tokenCajero(xela), cotizacion("2500.00", 0)),
                403, "ROL_INSUFICIENTE");
        esError(post(ALQUILERES + "/" + UUID.randomUUID() + "/cotizacion", token, cotizacion("2500.00", 0)),
                404, "ALQUILER_NO_EXISTE");
        assertThat(estadoDelAlquiler(solicitado)).isEqualTo("SOLICITADO");
    }

    // ----- T21 -----

    @Test
    void elClienteCancelaSuAlquilerSolicitado() {
        UUID cliente = UUID.randomUUID();
        UUID alquiler = insertarAlquiler(xela, cliente, "SOLICITADO");

        ResponseEntity<String> respuesta = post(ALQUILERES + "/" + alquiler + "/cancelar", tokenCliente(cliente),
                cancelacion("  Ya no viajamos ", 0));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("CANCELADO");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.motivoCancelacion")).isEqualTo("Ya no viajamos");
        assertThat(estadoDelAlquiler(alquiler)).isEqualTo("CANCELADO");
    }

    @Test
    void elAdminDeLaSucursalTambienCancela() {
        UUID alquiler = insertarAlquiler(xela, UUID.randomUUID(), "SOLICITADO");

        esStatus(post(ALQUILERES + "/" + alquiler + "/cancelar", tokenAdminSucursal(xela),
                cancelacion("Sin buses", 0)), 200);
        assertThat(estadoDelAlquiler(alquiler)).isEqualTo("CANCELADO");
    }

    @Test
    void cancelarAjenoYaCotizadoOSinMotivoFalla() {
        UUID cliente = UUID.randomUUID();
        UUID solicitado = insertarAlquiler(xela, cliente, "SOLICITADO");
        UUID cotizado = insertarAlquiler(xela, cliente, "COTIZADO");

        esError(post(ALQUILERES + "/" + solicitado + "/cancelar", tokenCliente(UUID.randomUUID()),
                cancelacion("Otro", 0)), 403, "ALQUILER_AJENO");
        esError(post(ALQUILERES + "/" + solicitado + "/cancelar", tokenAdminSucursal(guate),
                cancelacion("Otro", 0)), 403, "ALQUILER_AJENO");
        esError(post(ALQUILERES + "/" + cotizado + "/cancelar", tokenCliente(cliente), cancelacion("Tarde", 0)),
                422, "ESTADO_INVALIDO");
        esError(post(ALQUILERES + "/" + solicitado + "/cancelar", tokenCliente(cliente), cancelacion("Otro", 2)),
                409, "MODIFICACION_CONCURRENTE");
        esError(post(ALQUILERES + "/" + solicitado + "/cancelar", tokenCliente(cliente), cancelacion("  ", 0)),
                400, "VALIDACION");
        assertThat(estadoDelAlquiler(solicitado)).isEqualTo("SOLICITADO");
    }

    // ----- T22 -----

    @Test
    void asignarBusYChoferCreaElViajePrivadoYPublicaViajeProgramado() {
        UUID alquiler = insertarAlquiler(xela, UUID.randomUUID(), "PAGADO");
        UUID bus = insertarBus(xela, "P-111AAA");
        UUID chofer = insertarChofer(xela, "Juan Perez");
        fleetDisponible(xela, bus, 40, chofer);

        ResponseEntity<String> respuesta = post(ALQUILERES + "/" + alquiler + "/asignacion",
                tokenAdminSucursal(xela), asignacion(bus, chofer, 0));

        esStatus(respuesta, 201);
        String viajeId = json(respuesta, "$.viajeId");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.tipo")).isEqualTo("PRIVADO");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.alquilerId")).isEqualTo(alquiler.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.origenNombre")).isEqualTo("Xela");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.destinoNombre")).isEqualTo("Antigua");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("PROGRAMADO");
        assertThat(fecha(respuesta, "$.fechaSalidaProgramada")).isEqualTo(SALIDA_ALQUILER);
        assertThat(fecha(respuesta, "$.fechaLlegadaEstimada")).isEqualTo(LLEGADA_ALQUILER);
        assertThat(estadoDelAlquiler(alquiler)).isEqualTo("ASIGNADO");
        assertThat(jdbc.queryForObject(
                "SELECT ruta_id IS NULL AND precio_boleto IS NULL FROM viajes WHERE id = ?",
                Boolean.class, UUID.fromString(viajeId))).isTrue();
        // R1b con el día de salida del alquiler
        assertThat(FLEET.peticiones()).extracting(ServidorFalso.Peticion::consulta)
                .contains("fecha=" + SALIDA_ALQUILER.toLocalDate());

        Map<String, String> eventos = esperarEventos("ViajeProgramado", "AlquilerAsignado");
        String evento = eventos.get("ViajeProgramado");
        assertThat(JsonPath.<String>read(evento, "$.aggregateId")).isEqualTo(viajeId);
        assertThat(JsonPath.<String>read(evento, "$.payload.tipo")).isEqualTo("PRIVADO");
        assertThat(JsonPath.<String>read(evento, "$.payload.alquilerId")).isEqualTo(alquiler.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.busPlaca")).isEqualTo("P-111AAA");

        // AlquilerAsignado: Notification avisa al cliente que ya tiene bus y piloto
        String asignado = eventos.get("AlquilerAsignado");
        assertThat(JsonPath.<String>read(asignado, "$.aggregateId")).isEqualTo(alquiler.toString());
        assertThat(JsonPath.<String>read(asignado, "$.payload.viajeId")).isEqualTo(viajeId);
        assertThat(JsonPath.<String>read(asignado, "$.payload.clienteCorreo")).isEqualTo("maria@trips.gt");
        assertThat(JsonPath.<String>read(asignado, "$.payload.busPlaca")).isEqualTo("P-111AAA");
        assertThat(JsonPath.<String>read(asignado, "$.payload.choferNombre")).isEqualTo("Juan Perez");

        // el detalle del alquiler ya trae su viaje
        ResponseEntity<String> detalle = get(ALQUILERES + "/" + alquiler, tokenAdminSucursal(xela));
        assertThat(PruebaIntegracion.<String>json(detalle, "$.viajeId")).isEqualTo(viajeId);
        assertThat(PruebaIntegracion.<String>json(detalle, "$.estado")).isEqualTo("ASIGNADO");
    }

    @Test
    void soloSeAsignaUnAlquilerPagadoSinLlamarAFleet() {
        UUID alquiler = insertarAlquiler(xela, UUID.randomUUID(), "COTIZADO");

        esError(post(ALQUILERES + "/" + alquiler + "/asignacion", tokenAdminSucursal(xela),
                asignacion(UUID.randomUUID(), UUID.randomUUID(), 0)), 422, "ALQUILER_NO_PAGADO");
        assertThat(FLEET.peticiones()).isEmpty();
        assertThat(contar("viajes")).isZero();
    }

    @Test
    void unBusConMenosAsientosQuePasajerosEs422() {
        UUID alquiler = insertarAlquiler(xela, UUID.randomUUID(), "PAGADO", 45, SALIDA_ALQUILER);
        UUID bus = UUID.randomUUID();
        UUID chofer = UUID.randomUUID();
        fleetDisponible(xela, bus, 40, chofer);

        esError(post(ALQUILERES + "/" + alquiler + "/asignacion", tokenAdminSucursal(xela),
                asignacion(bus, chofer, 0)), 422, "CAPACIDAD_INSUFICIENTE");
        assertThat(estadoDelAlquiler(alquiler)).isEqualTo("PAGADO");
        assertThat(contar("viajes")).isZero();
    }

    @Test
    void asignarConFleetCaidoOtraSucursalOVersionViejaFalla() {
        UUID alquiler = insertarAlquiler(xela, UUID.randomUUID(), "PAGADO");
        UUID bus = UUID.randomUUID();
        FLEET.responder("GET", rutaDisponibilidad(bus), 500, errorRemoto(500, "ERROR_INTERNO", "Falla"));

        esError(post(ALQUILERES + "/" + alquiler + "/asignacion", tokenAdminSucursal(xela),
                asignacion(bus, UUID.randomUUID(), 0)), 503, "FLEET_NO_DISPONIBLE");
        esError(post(ALQUILERES + "/" + alquiler + "/asignacion", tokenAdminSucursal(guate),
                asignacion(bus, UUID.randomUUID(), 0)), 403, "SUCURSAL_AJENA");
        esError(post(ALQUILERES + "/" + alquiler + "/asignacion", tokenAdminSucursal(xela),
                asignacion(bus, UUID.randomUUID(), 7)), 409, "MODIFICACION_CONCURRENTE");
        assertThat(estadoDelAlquiler(alquiler)).isEqualTo("PAGADO");
    }
}
