package gt.cunoc.proyecto2.sales.integracion.ventas;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.sales.integracion.soporte.PruebaIntegracion;
import gt.cunoc.proyecto2.sales.integracion.soporte.ServidorFalso;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** S2–S6: venta en mostrador y en línea con Trips falso (R2), consulta, cancelación y listado. */
class VentasIT extends PruebaIntegracion {

    private static final String VENTAS = "/sales/ventas";
    private static final String MOSTRADOR = VENTAS + "/mostrador";
    private static final String EN_LINEA = VENTAS + "/en-linea";
    private static final BigDecimal PRECIO = new BigDecimal("75.00");
    private static final LocalDate FECHA_CANCELACION = FECHA_PAGO.plusDays(2);

    private UUID xela;
    private UUID guate;
    private UUID viaje;

    @BeforeEach
    void datos() {
        xela = UUID.randomUUID();
        guate = UUID.randomUUID();
        viaje = insertarViaje(xela);
    }

    // ----- Cuerpos y helpers propios -----

    private static String texto(Object valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private static String mostrador(UUID viajeId, String asientos, String nit, String nombre, String correo) {
        return """
                {"viajeId":"%s","asientos":%s,"clienteNit":%s,"clienteNombre":%s,"clienteCorreo":%s,"fechaPago":"%s"}
                """.formatted(viajeId, asientos, texto(nit), texto(nombre), texto(correo), FECHA_PAGO);
    }

    private static String mostrador(UUID viajeId, String asientos) {
        return mostrador(viajeId, asientos, null, null, null);
    }

    private static String enLinea(UUID viajeId, String asientos) {
        return "{\"viajeId\":\"%s\",\"asientos\":%s,\"fechaPago\":\"%s\"}".formatted(viajeId, asientos, FECHA_PAGO);
    }

    private static String cancelacion(String motivo, LocalDate fecha, Integer version) {
        return "{\"motivo\":%s,\"fechaCancelacion\":%s,\"version\":%s}"
                .formatted(texto(motivo), texto(fecha), version);
    }

    private static OffsetDateTime fecha(ResponseEntity<String> respuesta, String ruta) {
        return OffsetDateTime.parse(PruebaIntegracion.<String>json(respuesta, ruta));
    }

    private static int versionDelEvento(String sobre) {
        return JsonPath.<Number>read(sobre, "$.aggregateVersion").intValue();
    }

    private int contar(String tabla) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
        return total == null ? 0 : total;
    }

    /** Eventos de ese tipo guardados en el outbox (se insertan en la misma transacción del cambio). */
    private int enOutbox(String tipoEvento) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM eventos_salida WHERE tipo_evento = ?",
                Integer.class, tipoEvento);
        return total == null ? 0 : total;
    }

    private String tipoAgregado(String tipoEvento, UUID agregadoId) {
        return jdbc.queryForObject("SELECT tipo_agregado FROM eventos_salida WHERE tipo_evento = ? AND agregado_id = ?",
                String.class, tipoEvento, agregadoId);
    }

    /**
     * Venta directa en la BD, respetando los CHECK de V5 y V8. Con cliente: EN_LINEA pagada con CARTERA; sin
     * cliente: PRESENCIAL en EFECTIVO con cajero. CONFIRMADA lleva boletos ACTIVO y asientos VENDIDO;
     * PENDIENTE_PAGO deja los asientos RESERVADO por 5 minutos.
     */
    private UUID insertarVenta(UUID viajeId, UUID clienteUsuarioId, String estado, LocalDate fechaPago,
                               Integer... asientos) {
        UUID id = UUID.randomUUID();
        boolean enLinea = clienteUsuarioId != null;
        UUID sucursalId = jdbc.queryForObject("SELECT sucursal_origen_id FROM vista_viajes WHERE viaje_id = ?",
                UUID.class, viajeId);
        jdbc.update("""
                INSERT INTO ventas (id, viaje_id, canal, cliente_usuario_id, cliente_nit, cliente_nombre, sucursal_id,
                                    cajero_usuario_id, metodo_pago, monto_total, cantidad_boletos, fecha_pago, estado)
                VALUES (?, ?, ?, ?, 'CF', 'Consumidor final', ?, ?, ?, ?, ?, ?, ?)""",
                id, viajeId, enLinea ? "EN_LINEA" : "PRESENCIAL", clienteUsuarioId, sucursalId,
                enLinea ? null : UUID.randomUUID(), enLinea ? "CARTERA" : "EFECTIVO",
                PRECIO.multiply(BigDecimal.valueOf(asientos.length)), asientos.length, fechaPago, estado);
        for (Integer numero : asientos) {
            if ("CONFIRMADA".equals(estado)) {
                jdbc.update("INSERT INTO boletos (venta_id, viaje_id, numero_asiento, precio) VALUES (?, ?, ?, 75.00)",
                        id, viajeId, numero);
                jdbc.update("""
                        UPDATE asientos_viaje SET estado = 'VENDIDO', venta_id = ?
                         WHERE viaje_id = ? AND numero_asiento = ?""", id, viajeId, numero);
            } else {
                jdbc.update("""
                        UPDATE asientos_viaje SET estado = 'RESERVADO', venta_id = ?,
                               reservado_hasta = now() + interval '5 minutes'
                         WHERE viaje_id = ? AND numero_asiento = ?""", id, viajeId, numero);
            }
        }
        return id;
    }

    private UUID insertarVenta(UUID viajeId, UUID clienteUsuarioId, String estado, Integer... asientos) {
        return insertarVenta(viajeId, clienteUsuarioId, estado, FECHA_PAGO, asientos);
    }

    // ----- S4 · mostrador -----

    @Test
    void venderEnMostradorConfirmaEmiteBoletosYPublicaBoletoVendido() {
        UUID cajero = UUID.randomUUID();
        tripsVendible(viaje);

        ResponseEntity<String> respuesta = post(MOSTRADOR, token(cajero, Roles.CAJERO, xela), mostrador(viaje, "[5,3]"));

        esStatus(respuesta, 201);
        UUID ventaId = UUID.fromString(json(respuesta, "$.ventaId"));
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.viajeId")).isEqualTo(viaje.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.canal")).isEqualTo("PRESENCIAL");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.metodoPago")).isEqualTo("EFECTIVO");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("CONFIRMADA");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.clienteNit")).isEqualTo("CF");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.clienteNombre")).isEqualTo("Consumidor final");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.sucursalId")).isEqualTo(xela.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.fechaPago")).isEqualTo(FECHA_PAGO.toString());
        assertThat(decimal(respuesta, "$.montoTotal")).isEqualByComparingTo("150.00");
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.version")).isZero();
        assertThat(PruebaIntegracion.<List<Integer>>json(respuesta, "$.boletos[*].numeroAsiento")).containsExactly(3, 5);
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$.boletos[*].estado"))
                .containsExactly("ACTIVO", "ACTIVO");
        assertThat(decimal(respuesta, "$.boletos[0].precio")).isEqualByComparingTo("75.00");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.viaje.origenNombre")).isEqualTo("Quetzaltenango");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.viaje.busPlaca")).isEqualTo("P-123ABC");
        assertThat(fecha(respuesta, "$.viaje.fechaSalida")).isEqualTo(SALIDA);

        // BD: asientos VENDIDO de esta venta, cantidad_boletos (V8) y cajero
        assertThat(estadosAsientos(viaje, 3, 5)).containsExactly("VENDIDO", "VENDIDO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM asientos_viaje WHERE venta_id = ?", Integer.class, ventaId))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT cantidad_boletos FROM ventas WHERE id = ?", Integer.class, ventaId))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT cajero_usuario_id FROM ventas WHERE id = ?", UUID.class, ventaId))
                .isEqualTo(cajero);
        assertThat(contar("boletos")).isEqualTo(2);

        // R2 con token de servicio
        List<ServidorFalso.Peticion> peticiones = TRIPS.peticiones();
        assertThat(peticiones).extracting(ServidorFalso.Peticion::ruta).containsExactly(rutaVendible(viaje));
        assertThat(peticiones.get(0).autorizacion()).startsWith("Bearer ");

        String evento = esperarEventos("BoletoVendido").get("BoletoVendido");
        assertThat(JsonPath.<String>read(evento, "$.aggregateId")).isEqualTo(ventaId.toString());
        assertThat(versionDelEvento(evento)).isZero();
        assertThat(JsonPath.<String>read(evento, "$.businessDate")).isEqualTo(FECHA_PAGO.toString());
        assertThat(JsonPath.<Integer>read(evento, "$.payload.cantidadBoletos")).isEqualTo(2);
        assertThat(JsonPath.<List<Integer>>read(evento, "$.payload.asientos")).containsExactly(3, 5);
        assertThat(decimal(evento, "$.payload.montoTotal")).isEqualByComparingTo("150.00");
        assertThat(JsonPath.<String>read(evento, "$.payload.fechaPago")).isEqualTo(FECHA_PAGO.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.canal")).isEqualTo("PRESENCIAL");
        assertThat(JsonPath.<String>read(evento, "$.payload.metodoPago")).isEqualTo("EFECTIVO");
        assertThat(JsonPath.<String>read(evento, "$.payload.sucursalId")).isEqualTo(xela.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.busPlaca")).isEqualTo("P-123ABC");
        assertThat(tipoAgregado("BoletoVendido", ventaId)).isEqualTo("Venta");
    }

    @Test
    void elNitDeUnClienteRegistradoCompletaNombreYCorreo() {
        insertarCliente(UUID.randomUUID(), "1234567-K", "Ana Lopez", "ana@correo.com");
        tripsVendible(viaje);

        ResponseEntity<String> registrado = post(MOSTRADOR, tokenAdminSucursal(xela),
                mostrador(viaje, "[1]", " 1234567-k ", null, null));
        ResponseEntity<String> conDatos = post(MOSTRADOR, tokenCajero(xela),
                mostrador(viaje, "[2]", "1234567-K", "  Ana L. ", "Ana@Otro.GT"));

        esStatus(registrado, 201);
        assertThat(PruebaIntegracion.<String>json(registrado, "$.clienteNit")).isEqualTo("1234567-K");
        assertThat(PruebaIntegracion.<String>json(registrado, "$.clienteNombre")).isEqualTo("Ana Lopez");
        UUID primera = UUID.fromString(json(registrado, "$.ventaId"));
        assertThat(jdbc.queryForObject("SELECT cliente_correo FROM ventas WHERE id = ?", String.class, primera))
                .isEqualTo("ana@correo.com");
        // en mostrador la venta no queda ligada a la cuenta del cliente
        assertThat(jdbc.queryForObject("SELECT cliente_usuario_id IS NULL FROM ventas WHERE id = ?", Boolean.class,
                primera)).isTrue();

        // lo que escribe el cajero tiene prioridad
        esStatus(conDatos, 201);
        assertThat(PruebaIntegracion.<String>json(conDatos, "$.clienteNombre")).isEqualTo("Ana L.");
        UUID segunda = UUID.fromString(json(conDatos, "$.ventaId"));
        assertThat(jdbc.queryForObject("SELECT cliente_correo FROM ventas WHERE id = ?", String.class, segunda))
                .isEqualTo("ana@otro.gt");
    }

    @Test
    void unaReservaVencidaSePuedeVenderEnMostrador() {
        jdbc.update("""
                UPDATE asientos_viaje SET estado = 'RESERVADO', venta_id = ?, reservado_hasta = now() - interval '1 minute'
                 WHERE viaje_id = ? AND numero_asiento = 5""", UUID.randomUUID(), viaje);
        tripsVendible(viaje);

        ResponseEntity<String> respuesta = post(MOSTRADOR, tokenCajero(xela), mostrador(viaje, "[5]"));

        esStatus(respuesta, 201);
        assertThat(estadosAsientos(viaje, 5)).containsExactly("VENDIDO");
        assertThat(jdbc.queryForObject("SELECT venta_id FROM asientos_viaje WHERE viaje_id = ? AND numero_asiento = 5",
                UUID.class, viaje)).isEqualTo(UUID.fromString(json(respuesta, "$.ventaId")));
    }

    @Test
    void unAsientoOcupadoEs409YNoSeGuardaNada() {
        tripsVendible(viaje);
        esStatus(post(MOSTRADOR, tokenCajero(xela), mostrador(viaje, "[1,2]")), 201);

        esError(post(MOSTRADOR, tokenCajero(xela), mostrador(viaje, "[2,3]")), 409, "ASIENTO_OCUPADO");

        // rollback: ni la venta, ni el asiento 3, ni boletos, ni evento de la segunda
        assertThat(contar("ventas")).isEqualTo(1);
        assertThat(contar("boletos")).isEqualTo(2);
        assertThat(estadosAsientos(viaje, 1, 2, 3)).containsExactly("VENDIDO", "VENDIDO", "DISPONIBLE");
        assertThat(enOutbox("BoletoVendido")).isEqualTo(1);
    }

    @Test
    void unViajeDeOtraSucursalEs403SinLlamarATrips() {
        esError(post(MOSTRADOR, tokenCajero(guate), mostrador(viaje, "[1]")), 403, "OTRA_SUCURSAL");
        esError(post(MOSTRADOR, tokenAdminSucursal(guate), mostrador(viaje, "[1]")), 403, "OTRA_SUCURSAL");
        assertThat(TRIPS.peticiones()).isEmpty();
        assertThat(contar("ventas")).isZero();
    }

    @Test
    void asientosRepetidosFueraDeRangoODatosInvalidosSon400SinLlamarATrips() {
        String token = tokenCajero(xela);

        esError(post(MOSTRADOR, token, mostrador(viaje, "[3,3]")), 400, "ASIENTOS_REPETIDOS");
        esError(post(MOSTRADOR, token, mostrador(viaje, "[40,41]")), 400, "ASIENTO_FUERA_DE_RANGO");
        // @Valid
        esError(post(MOSTRADOR, token, mostrador(viaje, "[]")), 400, "VALIDACION");
        esError(post(MOSTRADOR, token, mostrador(viaje, "[0]")), 400, "VALIDACION");
        esError(post(MOSTRADOR, token, mostrador(viaje, "[1]", null, null, "no-es-correo")), 400, "VALIDACION");
        esError(post(MOSTRADOR, token, "{\"asientos\":[1],\"fechaPago\":\"%s\"}".formatted(FECHA_PAGO)),
                400, "VALIDACION");
        esError(post(MOSTRADOR, token, "{\"viajeId\":\"%s\",\"asientos\":[1]}".formatted(viaje)), 400, "VALIDACION");
        assertThat(TRIPS.peticiones()).isEmpty();
        assertThat(contar("ventas")).isZero();
    }

    @Test
    void unViajeQueTripsNoVendeEs422() {
        tripsVendible(viaje, false, "VIAJE_YA_INICIO");

        esError(post(MOSTRADOR, tokenCajero(xela), mostrador(viaje, "[1]")), 422, "VIAJE_NO_VENDIBLE");

        assertThat(contar("ventas")).isZero();
        assertThat(estadosAsientos(viaje, 1)).containsExactly("DISPONIBLE");
    }

    @Test
    void siTripsFallaOLaRutaNoEstaConfiguradaEs503() {
        TRIPS.responder("GET", rutaVendible(viaje), 500,
                "{\"status\":500,\"codigo\":\"ERROR_INTERNO\",\"mensaje\":\"Falla\"}");

        esError(post(MOSTRADOR, tokenCajero(xela), mostrador(viaje, "[1]")), 503, "TRIPS_NO_DISPONIBLE");

        // sin respuesta configurada el falso responde 404 RUTA_INEXISTENTE: es un problema entre servicios → 503
        TRIPS.reiniciar();
        esError(post(MOSTRADOR, tokenCajero(xela), mostrador(viaje, "[1]")), 503, "TRIPS_NO_DISPONIBLE");
        assertThat(contar("ventas")).isZero();
    }

    @Test
    void unErrorDelUsuarioEnTripsLlegaConElMismoCodigo() {
        TRIPS.responder("GET", rutaVendible(viaje), 422,
                "{\"status\":422,\"codigo\":\"VIAJE_BLOQUEADO\",\"mensaje\":\"Bloqueado\"}");

        esError(post(MOSTRADOR, tokenCajero(xela), mostrador(viaje, "[1]")), 422, "VIAJE_BLOQUEADO");
    }

    @Test
    void unViajeQueNoEstaALaVentaEs404() {
        UUID enRuta = insertarViaje(xela, 40, "EN_RUTA");

        esError(post(MOSTRADOR, tokenCajero(xela), mostrador(enRuta, "[1]")), 404, "VIAJE_NO_DISPONIBLE");
        esError(post(MOSTRADOR, tokenCajero(xela), mostrador(UUID.randomUUID(), "[1]")), 404, "VIAJE_NO_DISPONIBLE");
        assertThat(TRIPS.peticiones()).isEmpty();
    }

    @Test
    void venderEnMostradorConOtroRolOSinSesionFalla() {
        esError(post(MOSTRADOR, tokenCliente(UUID.randomUUID()), mostrador(viaje, "[1]")), 403, "ROL_INSUFICIENTE");
        esError(post(MOSTRADOR, tokenAdminSistema(), mostrador(viaje, "[1]")), 403, "ROL_INSUFICIENTE");
        esError(post(MOSTRADOR, null, mostrador(viaje, "[1]")), 401, "NO_AUTENTICADO");
        esError(post(MOSTRADOR, "token.no.valido", mostrador(viaje, "[1]")), 401, "TOKEN_INVALIDO");
        assertThat(contar("ventas")).isZero();
    }

    // ----- S2 · en línea -----

    @Test
    void comprarEnLineaReservaLosAsientosYPideElDebito() {
        UUID cliente = UUID.randomUUID();
        insertarCliente(cliente, "1234567-K", "Ana Lopez", "ana@correo.com");
        tripsVendible(viaje);
        OffsetDateTime antes = OffsetDateTime.now();

        ResponseEntity<String> respuesta = post(EN_LINEA, tokenCliente(cliente), enLinea(viaje, "[13,12]"));

        esStatus(respuesta, 202);
        UUID ventaId = UUID.fromString(json(respuesta, "$.ventaId"));
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("PENDIENTE_PAGO");
        assertThat(decimal(respuesta, "$.montoTotal")).isEqualByComparingTo("150.00");
        assertThat(PruebaIntegracion.<List<Integer>>json(respuesta, "$.asientos")).containsExactly(12, 13);
        assertThat(fecha(respuesta, "$.reservadoHasta").toInstant())           // reserva de 5 minutos
                .isAfter(antes.plusMinutes(4).toInstant())
                .isBefore(OffsetDateTime.now().plusMinutes(6).toInstant());

        // BD: venta en línea con los datos del perfil y asientos RESERVADO vigentes, sin boletos todavía
        assertThat(estadoVenta(ventaId)).isEqualTo("PENDIENTE_PAGO");
        assertThat(estadosAsientos(viaje, 12, 13)).containsExactly("RESERVADO", "RESERVADO");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM asientos_viaje WHERE venta_id = ? AND reservado_hasta > now()""",
                Integer.class, ventaId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT canal || '|' || metodo_pago || '|' || cliente_nit || '|' || cliente_nombre || '|'
                       || cliente_correo || '|' || cantidad_boletos FROM ventas WHERE id = ?""",
                String.class, ventaId)).isEqualTo("EN_LINEA|CARTERA|1234567-K|Ana Lopez|ana@correo.com|2");
        assertThat(jdbc.queryForObject("SELECT cliente_usuario_id FROM ventas WHERE id = ?", UUID.class, ventaId))
                .isEqualTo(cliente);
        assertThat(contar("boletos")).isZero();
        assertThat(TRIPS.peticiones()).extracting(ServidorFalso.Peticion::ruta).containsExactly(rutaVendible(viaje));

        String evento = esperarEventos("DebitoCarteraSolicitado").get("DebitoCarteraSolicitado");
        assertThat(JsonPath.<String>read(evento, "$.aggregateId")).isEqualTo(ventaId.toString());
        assertThat(versionDelEvento(evento)).isZero();
        assertThat(JsonPath.<String>read(evento, "$.businessDate")).isEqualTo(FECHA_PAGO.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.referenciaTipo")).isEqualTo("VENTA");
        assertThat(JsonPath.<String>read(evento, "$.payload.referenciaId")).isEqualTo(ventaId.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.usuarioId")).isEqualTo(cliente.toString());
        assertThat(decimal(evento, "$.payload.monto")).isEqualByComparingTo("150.00");
        assertThat(JsonPath.<String>read(evento, "$.payload.fechaMovimiento")).isEqualTo(FECHA_PAGO.toString());
        assertThat(tipoAgregado("DebitoCarteraSolicitado", ventaId)).isEqualTo("Venta");
        assertThat(enOutbox("BoletoVendido")).isZero();
    }

    @Test
    void sinPerfilDeClienteEs422SinLlamarATrips() {
        tripsVendible(viaje);

        esError(post(EN_LINEA, tokenCliente(UUID.randomUUID()), enLinea(viaje, "[1]")), 422, "CLIENTE_SIN_PERFIL");

        assertThat(TRIPS.peticiones()).isEmpty();
        assertThat(contar("ventas")).isZero();
        assertThat(estadosAsientos(viaje, 1)).containsExactly("DISPONIBLE");
    }

    @Test
    void comprarEnLineaAsientosReservadosOUnViajeNoVendibleFalla() {
        UUID cliente = UUID.randomUUID();
        insertarCliente(cliente, "1234567-K", "Ana Lopez", "ana@correo.com");
        insertarVenta(viaje, UUID.randomUUID(), "PENDIENTE_PAGO", 13);           // reserva vigente de otro
        tripsVendible(viaje);
        String token = tokenCliente(cliente);

        esError(post(EN_LINEA, token, enLinea(viaje, "[12,13]")), 409, "ASIENTO_OCUPADO");
        assertThat(contar("ventas")).isEqualTo(1);
        assertThat(estadosAsientos(viaje, 12, 13)).containsExactly("DISPONIBLE", "RESERVADO");

        esError(post(EN_LINEA, token, enLinea(viaje, "[12,12]")), 400, "ASIENTOS_REPETIDOS");
        esError(post(EN_LINEA, token, enLinea(viaje, "[]")), 400, "VALIDACION");
        esError(post(EN_LINEA, token, enLinea(UUID.randomUUID(), "[1]")), 404, "VIAJE_NO_DISPONIBLE");

        tripsVendible(viaje, false, "VIAJE_YA_INICIO");
        esError(post(EN_LINEA, token, enLinea(viaje, "[12]")), 422, "VIAJE_NO_VENDIBLE");

        esError(post(EN_LINEA, tokenCajero(xela), enLinea(viaje, "[12]")), 403, "ROL_INSUFICIENTE");
        assertThat(contar("ventas")).isEqualTo(1);
        assertThat(enOutbox("DebitoCarteraSolicitado")).isZero();
    }

    // ----- S3 · obtener -----

    @Test
    void elClienteVeSuCompraPendienteYNoLaDeOtro() {
        UUID cliente = UUID.randomUUID();
        insertarCliente(cliente, "1234567-K", "Ana Lopez", "ana@correo.com");
        tripsVendible(viaje);
        String ventaId = json(post(EN_LINEA, tokenCliente(cliente), enLinea(viaje, "[7]")), "$.ventaId");

        ResponseEntity<String> respuesta = get(VENTAS + "/" + ventaId, tokenCliente(cliente));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.ventaId")).isEqualTo(ventaId);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("PENDIENTE_PAGO");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.canal")).isEqualTo("EN_LINEA");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.metodoPago")).isEqualTo("CARTERA");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.clienteNombre")).isEqualTo("Ana Lopez");
        assertThat(PruebaIntegracion.<List<Object>>json(respuesta, "$.boletos")).isEmpty();
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.viaje.destinoNombre")).isEqualTo("Guatemala");
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.version")).isZero();

        esError(get(VENTAS + "/" + ventaId, tokenCliente(UUID.randomUUID())), 403, "VENTA_AJENA");
        esError(get(VENTAS + "/" + ventaId, null), 401, "NO_AUTENTICADO");
    }

    @Test
    void elPersonalDeLaSucursalYElAdminDelSistemaVenLaVenta() {
        UUID venta = insertarVenta(viaje, null, "CONFIRMADA", 2, 1);
        String detalle = VENTAS + "/" + venta;

        ResponseEntity<String> delCajero = get(detalle, tokenCajero(xela));

        esStatus(delCajero, 200);
        assertThat(PruebaIntegracion.<String>json(delCajero, "$.estado")).isEqualTo("CONFIRMADA");
        assertThat(PruebaIntegracion.<List<Integer>>json(delCajero, "$.boletos[*].numeroAsiento")).containsExactly(1, 2);
        assertThat(PruebaIntegracion.<String>json(delCajero, "$.viaje.busPlaca")).isEqualTo("P-123ABC");
        esStatus(get(detalle, tokenAdminSucursal(xela)), 200);
        esStatus(get(detalle, tokenAdminSistema()), 200);
        esError(get(detalle, tokenCajero(guate)), 403, "VENTA_AJENA");
        esError(get(detalle, tokenAdminSucursal(guate)), 403, "VENTA_AJENA");
        esError(get(detalle, tokenCliente(UUID.randomUUID())), 403, "VENTA_AJENA");
        esError(get(VENTAS + "/" + UUID.randomUUID(), tokenAdminSistema()), 404, "VENTA_NO_EXISTE");
        esError(get(detalle, token(UUID.randomUUID(), Roles.CHOFER, null)), 403, "ROL_INSUFICIENTE");
    }

    @Test
    void unaVentaDeUnViajeEliminadoSeConservaSinElViaje() {
        UUID venta = insertarVenta(viaje, null, "CONFIRMADA", 4);
        jdbc.update("DELETE FROM vista_viajes WHERE viaje_id = ?", viaje);

        ResponseEntity<String> respuesta = get(VENTAS + "/" + venta, tokenCajero(xela));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.viajeId")).isEqualTo(viaje.toString());
        assertThat(respuesta.getBody()).doesNotContain("\"viaje\"");
        assertThat(PruebaIntegracion.<List<Integer>>json(respuesta, "$.boletos[*].numeroAsiento")).containsExactly(4);
    }

    // ----- S5 · cancelar -----

    @Test
    void cancelarUnaVentaDeMostradorLiberaLosAsientosYPublicaVentaCancelada() {
        tripsVendible(viaje);
        ResponseEntity<String> vendida = post(MOSTRADOR, tokenCajero(xela), mostrador(viaje, "[8,7]"));
        UUID ventaId = UUID.fromString(json(vendida, "$.ventaId"));
        int version = json(vendida, "$.version");

        ResponseEntity<String> respuesta = post(VENTAS + "/" + ventaId + "/cancelar", tokenAdminSucursal(xela),
                cancelacion("  Cliente no viaja ", FECHA_CANCELACION, version));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("CANCELADA");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.motivoCancelacion")).isEqualTo("Cliente no viaja");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.fechaCancelacion"))
                .isEqualTo(FECHA_CANCELACION.toString());
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$.boletos[*].estado"))
                .containsExactly("CANCELADO", "CANCELADO");
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.version")).isEqualTo(1);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.viaje.origenNombre")).isEqualTo("Quetzaltenango");

        assertThat(estadoVenta(ventaId)).isEqualTo("CANCELADA");
        assertThat(estadosAsientos(viaje, 7, 8)).containsExactly("DISPONIBLE", "DISPONIBLE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM asientos_viaje WHERE venta_id = ?", Integer.class, ventaId))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM boletos WHERE venta_id = ? AND estado = 'CANCELADO'",
                Integer.class, ventaId)).isEqualTo(2);

        String evento = esperarEventos("VentaCancelada").get("VentaCancelada");
        assertThat(JsonPath.<String>read(evento, "$.aggregateId")).isEqualTo(ventaId.toString());
        assertThat(versionDelEvento(evento)).isEqualTo(1);
        assertThat(JsonPath.<String>read(evento, "$.businessDate")).isEqualTo(FECHA_CANCELACION.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.motivo")).isEqualTo("Cliente no viaja");
        assertThat(JsonPath.<Integer>read(evento, "$.payload.cantidadBoletos")).isEqualTo(2);
        assertThat(JsonPath.<List<Integer>>read(evento, "$.payload.asientos")).containsExactly(7, 8);
        assertThat(decimal(evento, "$.payload.montoAnulado")).isEqualByComparingTo("150.00");
        assertThat(JsonPath.<String>read(evento, "$.payload.metodoPago")).isEqualTo("EFECTIVO");
        assertThat(JsonPath.<String>read(evento, "$.payload.origenNombre")).isEqualTo("Quetzaltenango");
        assertThat(tipoAgregado("VentaCancelada", ventaId)).isEqualTo("Venta");
        // pagada en efectivo: no hay reembolso a la cartera
        assertThat(enOutbox("ReembolsoCarteraSolicitado")).isZero();
    }

    @Test
    void cancelarUnaVentaEnLineaPideElReembolsoALaCartera() {
        UUID cliente = UUID.randomUUID();
        UUID venta = insertarVenta(viaje, cliente, "CONFIRMADA", 10, 11);

        ResponseEntity<String> respuesta = post(VENTAS + "/" + venta + "/cancelar", tokenAdminSucursal(xela),
                cancelacion("Viaje reprogramado", FECHA_CANCELACION, 0));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("CANCELADA");
        assertThat(estadosAsientos(viaje, 10, 11)).containsExactly("DISPONIBLE", "DISPONIBLE");

        Map<String, String> eventos = esperarEventos("VentaCancelada", "ReembolsoCarteraSolicitado");
        assertThat(JsonPath.<String>read(eventos.get("VentaCancelada"), "$.payload.metodoPago")).isEqualTo("CARTERA");
        String reembolso = eventos.get("ReembolsoCarteraSolicitado");
        assertThat(JsonPath.<String>read(reembolso, "$.aggregateId")).isEqualTo(venta.toString());
        assertThat(versionDelEvento(reembolso)).isEqualTo(1);
        assertThat(JsonPath.<String>read(reembolso, "$.businessDate")).isEqualTo(FECHA_CANCELACION.toString());
        assertThat(JsonPath.<String>read(reembolso, "$.payload.referenciaTipo")).isEqualTo("VENTA");
        assertThat(JsonPath.<String>read(reembolso, "$.payload.referenciaId")).isEqualTo(venta.toString());
        assertThat(JsonPath.<String>read(reembolso, "$.payload.usuarioId")).isEqualTo(cliente.toString());
        assertThat(decimal(reembolso, "$.payload.monto")).isEqualByComparingTo("150.00");
        assertThat(JsonPath.<String>read(reembolso, "$.payload.motivo")).isEqualTo("VENTA_CANCELADA");
        assertThat(JsonPath.<String>read(reembolso, "$.payload.fechaMovimiento"))
                .isEqualTo(FECHA_CANCELACION.toString());
        assertThat(tipoAgregado("ReembolsoCarteraSolicitado", venta)).isEqualTo("Venta");
    }

    @Test
    void unAsientoDeUnaVentaCanceladaSeVuelveAVender() {
        UUID venta = insertarVenta(viaje, null, "CONFIRMADA", 5);
        esStatus(post(VENTAS + "/" + venta + "/cancelar", tokenAdminSucursal(xela),
                cancelacion("Error de cajero", FECHA_PAGO, 0)), 200);
        tripsVendible(viaje);

        esStatus(post(MOSTRADOR, tokenCajero(xela), mostrador(viaje, "[5]")), 201);

        assertThat(estadosAsientos(viaje, 5)).containsExactly("VENDIDO");
        // el índice único solo cuenta boletos ACTIVO
        assertThat(jdbc.queryForList("SELECT estado FROM boletos WHERE viaje_id = ? AND numero_asiento = 5",
                String.class, viaje)).containsExactlyInAnyOrder("CANCELADO", "ACTIVO");
    }

    @Test
    void cancelarConVersionViejaEnOtroEstadoOConDatosInvalidosFalla() {
        UUID confirmada = insertarVenta(viaje, null, "CONFIRMADA", 1);
        UUID pendiente = insertarVenta(viaje, UUID.randomUUID(), "PENDIENTE_PAGO", 2);
        String admin = tokenAdminSucursal(xela);

        esError(post(VENTAS + "/" + confirmada + "/cancelar", admin, cancelacion("Otro", FECHA_CANCELACION, 3)),
                409, "MODIFICACION_CONCURRENTE");
        esError(post(VENTAS + "/" + pendiente + "/cancelar", admin, cancelacion("Otro", FECHA_CANCELACION, 0)),
                422, "ESTADO_INVALIDO");
        esError(post(VENTAS + "/" + confirmada + "/cancelar", admin, cancelacion("Otro", FECHA_PAGO.minusDays(1), 0)),
                400, "FECHA_CANCELACION_INVALIDA");
        esError(post(VENTAS + "/" + confirmada + "/cancelar", admin, cancelacion("   ", FECHA_CANCELACION, 0)),
                400, "VALIDACION");
        esError(post(VENTAS + "/" + confirmada + "/cancelar", admin, cancelacion("Otro", null, 0)), 400, "VALIDACION");
        esError(post(VENTAS + "/" + confirmada + "/cancelar", admin, cancelacion("Otro", FECHA_CANCELACION, null)),
                400, "VALIDACION");
        esError(post(VENTAS + "/" + confirmada + "/cancelar", tokenAdminSucursal(guate),
                cancelacion("Otro", FECHA_CANCELACION, 0)), 403, "SUCURSAL_AJENA");
        esError(post(VENTAS + "/" + confirmada + "/cancelar", tokenCajero(xela),
                cancelacion("Otro", FECHA_CANCELACION, 0)), 403, "ROL_INSUFICIENTE");
        esError(post(VENTAS + "/" + UUID.randomUUID() + "/cancelar", admin, cancelacion("Otro", FECHA_CANCELACION, 0)),
                404, "VENTA_NO_EXISTE");

        assertThat(estadoVenta(confirmada)).isEqualTo("CONFIRMADA");
        assertThat(estadoVenta(pendiente)).isEqualTo("PENDIENTE_PAGO");
        assertThat(estadosAsientos(viaje, 1, 2)).containsExactly("VENDIDO", "RESERVADO");
        assertThat(enOutbox("VentaCancelada")).isZero();
    }

    @Test
    void noSeCancelaUnaVentaDeUnViajeQueYaSalioOQueYaNoExiste() {
        UUID enRuta = insertarViaje(xela, 40, "EN_RUTA");
        UUID venta = insertarVenta(enRuta, null, "CONFIRMADA", 3);
        UUID deEliminado = insertarVenta(viaje, null, "CONFIRMADA", 4);
        jdbc.update("DELETE FROM vista_viajes WHERE viaje_id = ?", viaje);

        esError(post(VENTAS + "/" + venta + "/cancelar", tokenAdminSucursal(xela),
                cancelacion("Tarde", FECHA_CANCELACION, 0)), 422, "VIAJE_YA_INICIO");
        esError(post(VENTAS + "/" + deEliminado + "/cancelar", tokenAdminSucursal(xela),
                cancelacion("Tarde", FECHA_CANCELACION, 0)), 422, "VIAJE_YA_INICIO");

        // rollback: la venta, sus boletos y su asiento quedan como estaban
        assertThat(estadoVenta(venta)).isEqualTo("CONFIRMADA");
        assertThat(estadosAsientos(enRuta, 3)).containsExactly("VENDIDO");
        assertThat(jdbc.queryForObject("SELECT estado FROM boletos WHERE venta_id = ?", String.class, venta))
                .isEqualTo("ACTIVO");
        assertThat(contar("eventos_salida")).isZero();
    }

    // ----- S6 · listar -----

    @Test
    void elPersonalListaLasVentasDeSuSucursalConFiltros() {
        UUID viajeGuate = insertarViaje(guate);
        UUID presencial = insertarVenta(viaje, null, "CONFIRMADA", LocalDate.of(2026, 11, 1), 1);
        UUID enLineaConfirmada = insertarVenta(viaje, UUID.randomUUID(), "CONFIRMADA", LocalDate.of(2026, 11, 3), 3, 2);
        UUID pendiente = insertarVenta(viaje, UUID.randomUUID(), "PENDIENTE_PAGO", LocalDate.of(2026, 11, 2), 4);
        UUID deGuate = insertarVenta(viajeGuate, null, "CONFIRMADA", LocalDate.of(2026, 11, 4), 1);

        ResponseEntity<String> deXela = get(VENTAS, tokenCajero(xela));
        ResponseEntity<String> confirmadas = get(VENTAS + "?estado=CONFIRMADA", tokenAdminSucursal(xela));
        ResponseEntity<String> enLinea = get(VENTAS + "?canal=EN_LINEA&sucursalId=" + xela, tokenCajero(xela));
        ResponseEntity<String> delDia = get(VENTAS + "?desde=2026-11-02&hasta=2026-11-02", tokenCajero(xela));
        ResponseEntity<String> desde = get(VENTAS + "?desde=2026-11-02", tokenCajero(xela));
        ResponseEntity<String> hasta = get(VENTAS + "?hasta=2026-11-02", tokenCajero(xela));
        ResponseEntity<String> todas = get(VENTAS, tokenAdminSistema());
        ResponseEntity<String> soloGuate = get(VENTAS + "?sucursalId=" + guate, tokenAdminSistema());
        ResponseEntity<String> porViaje = get(VENTAS + "?viajeId=" + viajeGuate, tokenAdminSistema());

        esStatus(deXela, 200);
        assertThat(PruebaIntegracion.<List<String>>json(deXela, "$[*].ventaId"))      // fecha de pago más reciente primero
                .containsExactly(enLineaConfirmada.toString(), pendiente.toString(), presencial.toString());
        assertThat(PruebaIntegracion.<List<Integer>>json(deXela, "$[0].boletos[*].numeroAsiento")).containsExactly(2, 3);
        assertThat(PruebaIntegracion.<List<Object>>json(deXela, "$[1].boletos")).isEmpty();
        assertThat(deXela.getBody()).doesNotContain("\"viaje\"");                     // S6 no trae el viaje
        assertThat(PruebaIntegracion.<List<String>>json(confirmadas, "$[*].ventaId"))
                .containsExactly(enLineaConfirmada.toString(), presencial.toString());
        assertThat(PruebaIntegracion.<List<String>>json(enLinea, "$[*].ventaId"))
                .containsExactly(enLineaConfirmada.toString(), pendiente.toString());
        assertThat(PruebaIntegracion.<List<String>>json(delDia, "$[*].ventaId")).containsExactly(pendiente.toString());
        assertThat(PruebaIntegracion.<List<String>>json(desde, "$[*].ventaId"))
                .containsExactly(enLineaConfirmada.toString(), pendiente.toString());
        assertThat(PruebaIntegracion.<List<String>>json(hasta, "$[*].ventaId"))
                .containsExactly(pendiente.toString(), presencial.toString());
        assertThat(PruebaIntegracion.<List<String>>json(todas, "$[*].ventaId")).containsExactly(deGuate.toString(),
                enLineaConfirmada.toString(), pendiente.toString(), presencial.toString());
        assertThat(PruebaIntegracion.<List<String>>json(soloGuate, "$[*].ventaId")).containsExactly(deGuate.toString());
        assertThat(PruebaIntegracion.<List<String>>json(porViaje, "$[*].ventaId")).containsExactly(deGuate.toString());
    }

    @Test
    void listarVentasDeOtraSucursalOConFiltrosInvalidosFalla() {
        esStatus(get(VENTAS + "?estado=CANCELADA", tokenCajero(xela)), 200);
        assertThat(PruebaIntegracion.<List<Object>>json(get(VENTAS, tokenCajero(xela)), "$")).isEmpty();

        esError(get(VENTAS + "?sucursalId=" + guate, tokenCajero(xela)), 403, "SUCURSAL_AJENA");
        esError(get(VENTAS + "?desde=2026-11-05&hasta=2026-11-01", tokenAdminSistema()), 400, "RANGO_FECHAS_INVALIDO");
        esError(get(VENTAS + "?estado=VOLANDO", tokenAdminSistema()), 400, "PARAMETRO_INVALIDO");
        esError(get(VENTAS + "?desde=ayer", tokenAdminSistema()), 400, "PARAMETRO_INVALIDO");
        esError(get(VENTAS, tokenCliente(UUID.randomUUID())), 403, "ROL_INSUFICIENTE");
        esError(get(VENTAS, null), 401, "NO_AUTENTICADO");
    }
}
