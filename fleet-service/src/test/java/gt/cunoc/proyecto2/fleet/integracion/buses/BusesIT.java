package gt.cunoc.proyecto2.fleet.integracion.buses;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.fleet.integracion.soporte.PruebaIntegracion;
import gt.cunoc.proyecto2.fleet.integracion.soporte.ServidorFalso;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** F10–F14, F23 y R1a: buses con Trips simulado (R3). */
class BusesIT extends PruebaIntegracion {

    private static final String RUTA_R3 = "/trips/interno/viajes/activos";

    private UUID xela;

    @BeforeEach
    void datos() {
        xela = insertarSucursal("XELA", true);
    }

    private String nuevoBus(UUID sucursalId, String placa, String fotoUrl) {
        return """
                {"sucursalId":"%s","placa":"%s","marca":"Volvo","modelo":"B12","anioFabricacion":2020,
                 "capacidadPasajeros":48,"kilometrajeActual":150.50,"fotoUrl":"%s","fechaRegistro":"2026-10-20"}
                """.formatted(sucursalId, placa, fotoUrl);
    }

    private static String cambioDeEstado(String estado) {
        return "{\"estado\":\"%s\",\"fecha\":\"2026-10-20\",\"version\":0}".formatted(estado);
    }

    private static String viajesActivos(int cantidad) {
        return "{\"cantidad\":%d,\"viajes\":[]}".formatted(cantidad);
    }

    private String estadoEnBd(UUID busId) {
        return jdbc.queryForObject("SELECT estado FROM buses WHERE id = ?", String.class, busId);
    }

    // ----- F12 -----

    @Test
    void crearBusYPublicarBusCreado() {
        ResponseEntity<String> respuesta = post("/fleet/buses", tokenAdminSucursal(xela),
                nuevoBus(xela, "p-555xyz", FOTO_BUS));

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.placa")).isEqualTo("P-555XYZ");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("ACTIVO");
        String busCreado = esperarEventos("BusCreado").get("BusCreado");
        assertThat(JsonPath.<String>read(busCreado, "$.payload.placa")).isEqualTo("P-555XYZ");
        // La foto viaja en el evento: Trips se la muestra al pasajero cuando compra el boleto
        assertThat(JsonPath.<String>read(busCreado, "$.payload.fotoUrl")).isEqualTo(FOTO_BUS);
    }

    @Test
    void unaFotoQueNoEsDelBucketEs400() {
        esError(post("/fleet/buses", tokenAdminSucursal(xela),
                nuevoBus(xela, "P-555XYZ", "https://otro-sitio.com/bus.jpg")), 400, "FOTO_INVALIDA");
        assertThat(contar("buses")).isZero();
    }

    @Test
    void placaRepetidaEs409() {
        insertarBus(xela, "P-555XYZ", "ACTIVO", "0");

        esError(post("/fleet/buses", tokenAdminSucursal(xela), nuevoBus(xela, "P-555XYZ", FOTO_BUS)),
                409, "PLACA_EN_USO");
    }

    @Test
    void unAdminNoCreaBusesEnOtraSucursal() {
        UUID guate = insertarSucursal("GUATE", true);

        esError(post("/fleet/buses", tokenAdminSucursal(guate), nuevoBus(xela, "P-555XYZ", FOTO_BUS)),
                403, "SUCURSAL_AJENA");
    }

    // ----- F10, F11 y F13 -----

    @Test
    void elAdminDeSucursalSoloVeSusBuses() {
        UUID guate = insertarSucursal("GUATE", true);
        insertarBus(xela, "P-111AAA", "ACTIVO", "0");
        insertarBus(guate, "P-222BBB", "ACTIVO", "0");

        ResponseEntity<String> deXela = get("/fleet/buses", tokenAdminSucursal(xela));
        ResponseEntity<String> todos = get("/fleet/buses?estado=ACTIVO", tokenAdminSistema());

        assertThat(PruebaIntegracion.<List<String>>json(deXela, "$[*].placa")).containsExactly("P-111AAA");
        assertThat(PruebaIntegracion.<List<String>>json(todos, "$[*].placa")).containsExactly("P-111AAA", "P-222BBB");
    }

    @Test
    void elDetalleIncluyeLosUltimosViajes() {
        UUID bus = insertarBus(xela, "P-111AAA", "ACTIVO", "1300");
        insertarViajeEnBitacora(bus, "1000", "1100", OffsetDateTime.parse("2026-10-18T12:00:00-06:00"));
        insertarViajeEnBitacora(bus, "1100", "1300", OffsetDateTime.parse("2026-10-19T12:00:00-06:00"));

        ResponseEntity<String> respuesta = get("/fleet/buses/" + bus, tokenAdminSucursal(xela));

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.placa")).isEqualTo("P-111AAA");   // @JsonUnwrapped
        assertThat(PruebaIntegracion.<List<Object>>json(respuesta, "$.ultimosViajes")).hasSize(2);
        assertThat(PruebaIntegracion.<Number>json(respuesta, "$.ultimosViajes[0].kmRecorridos").doubleValue())
                .isEqualTo(200.0);                                                             // el más reciente primero
        esError(get("/fleet/buses/" + UUID.randomUUID(), tokenAdminSucursal(xela)), 404, "BUS_NO_EXISTE");
    }

    @Test
    void actualizarDatosDelBus() {
        UUID bus = insertarBus(xela, "P-111AAA", "ACTIVO", "0");

        ResponseEntity<String> respuesta = put("/fleet/buses/" + bus, tokenAdminSucursal(xela), """
                {"marca":"Scania","modelo":"K410","anioFabricacion":2022,"capacidadPasajeros":55,
                 "fotoUrl":"%s","version":0}""".formatted(FOTO_BUS));

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.marca")).isEqualTo("Scania");
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.version")).isEqualTo(1);
        esperarEventos("BusActualizado");
    }

    // ----- F14 con R3 -----

    @Test
    void noSeDesactivaSiTripsReportaViajesActivos() {
        UUID bus = insertarBus(xela, "P-111AAA", "ACTIVO", "0");
        TRIPS.responder("GET", RUTA_R3, 200, viajesActivos(2));

        esError(patch("/fleet/buses/" + bus + "/estado", tokenAdminSucursal(xela), cambioDeEstado("INACTIVO")),
                422, "BUS_CON_VIAJES_ACTIVOS");

        ServidorFalso.Peticion r3 = TRIPS.peticiones().get(0);
        assertThat(r3.consulta()).isEqualTo("busId=" + bus);
        assertThat(r3.autorizacion()).startsWith("Bearer ");
        assertThat(estadoEnBd(bus)).isEqualTo("ACTIVO");
    }

    @Test
    void desactivarSinViajesPublicaBusDesactivado() {
        UUID bus = insertarBus(xela, "P-111AAA", "ACTIVO", "0");
        TRIPS.responder("GET", RUTA_R3, 200, viajesActivos(0));

        ResponseEntity<String> respuesta = patch("/fleet/buses/" + bus + "/estado", tokenAdminSucursal(xela),
                cambioDeEstado("INACTIVO"));

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(200);
        assertThat(estadoEnBd(bus)).isEqualTo("INACTIVO");
        esperarEventos("BusDesactivado");
    }

    @Test
    void siTripsNoRespondeNoSeDesactiva() {
        UUID bus = insertarBus(xela, "P-111AAA", "ACTIVO", "0");
        TRIPS.responder("GET", RUTA_R3, 503, errorRemoto(503, "SERVICIO_NO_DISPONIBLE", "caido"));

        esError(patch("/fleet/buses/" + bus + "/estado", tokenAdminSucursal(xela), cambioDeEstado("INACTIVO")),
                503, "TRIPS_NO_DISPONIBLE");
        assertThat(estadoEnBd(bus)).isEqualTo("ACTIVO");
    }

    @Test
    void mandarAlTallerNoConsultaTrips() {
        UUID bus = insertarBus(xela, "P-111AAA", "ACTIVO", "0");

        ResponseEntity<String> respuesta = patch("/fleet/buses/" + bus + "/estado", tokenAdminSucursal(xela),
                cambioDeEstado("EN_TALLER"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(TRIPS.peticiones()).isEmpty();
        esError(patch("/fleet/buses/" + bus + "/estado", tokenAdminSucursal(xela), cambioDeEstado("EN_RUTA")),
                409, "MODIFICACION_CONCURRENTE");                                         // la version ya es 1
    }

    // ----- F23 -----

    @Test
    void bitacoraPorRangoDeFechas() {
        UUID bus = insertarBus(xela, "P-111AAA", "ACTIVO", "500");
        insertarViajeEnBitacora(bus, "0", "100", OffsetDateTime.parse("2026-09-30T23:30:00-06:00"));
        insertarViajeEnBitacora(bus, "100", "300", OffsetDateTime.parse("2026-10-01T00:10:00-06:00"));
        insertarViajeEnBitacora(bus, "300", "500", OffsetDateTime.parse("2026-10-31T23:59:00-06:00"));

        ResponseEntity<String> octubre = get("/fleet/buses/" + bus + "/bitacora?desde=2026-10-01&hasta=2026-10-31",
                tokenAdminSucursal(xela));

        assertThat(PruebaIntegracion.<List<Object>>json(octubre, "$")).hasSize(2);
        esError(get("/fleet/buses/" + bus + "/bitacora?desde=2026-10-31&hasta=2026-10-01", tokenAdminSucursal(xela)),
                400, "RANGO_FECHAS_INVALIDO");
    }

    // ----- F22 (sin tocar el bucket: el archivo se rechaza antes) -----

    @Test
    void unArchivoQueNoEsImagenEs400AunqueSeLlamePng() {
        ResponseEntity<String> respuesta = subirArchivo("/fleet/archivos/fotos?tipo=BUS", tokenAdminSucursal(xela),
                "bus.png", "no soy una imagen".getBytes());

        esError(respuesta, 400, "ARCHIVO_INVALIDO");
        assertThat(subirArchivo("/fleet/archivos/fotos?tipo=BUS", tokenCliente(), "bus.png", new byte[] {1})
                .getStatusCode().value()).isEqualTo(403);
    }

    // ----- R1a (interno) -----

    @Test
    void disponibilidadSoloConTokenDeServicio() {
        UUID bus = insertarBus(xela, "P-111AAA", "EN_TALLER", "0");

        ResponseEntity<String> respuesta = get("/fleet/interno/buses/" + bus + "/disponibilidad", tokenServicio());

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(PruebaIntegracion.<Boolean>json(respuesta, "$.disponible")).isFalse();
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.motivo")).isEqualTo("BUS_EN_TALLER");
        assertThat(get("/fleet/interno/buses/" + bus + "/disponibilidad", tokenCliente())
                .getStatusCode().value()).isEqualTo(403);
    }
}
