package gt.cunoc.proyecto2.trips.integracion.viajes;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.trips.integracion.soporte.PruebaIntegracion;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * T10 y T11 por la API pública: programar un viaje regular (con su regreso opcional) y reprogramarlo.
 * Aquí es donde se ven de verdad R1a y R1b contra el Fleet falso, y los eventos que salen al outbox.
 */
class ViajesIT extends PruebaIntegracion {

    private static final OffsetDateTime SALIDA_REGRESO = OffsetDateTime.parse("2026-11-20T14:00:00-06:00");
    private static final OffsetDateTime LLEGADA_REGRESO = OffsetDateTime.parse("2026-11-20T18:00:00-06:00");

    private UUID xela;
    private UUID guate;
    private UUID ruta;
    private UUID bus;
    private UUID chofer;

    private int viajesDeLaRuta(UUID rutaId) {
        return jdbc.queryForObject("SELECT count(*) FROM viajes WHERE ruta_id = ?", Integer.class, rutaId);
    }

    @BeforeEach
    void datos() {
        xela = insertarSucursal("Quetzaltenango", true);
        guate = insertarSucursal("Guatemala", true);
        ruta = insertarRuta(xela, guate, true);
        bus = insertarBus(xela, "P-111AAA");
        chofer = insertarChofer(xela, "Juan Perez");
        fleetDisponible(xela, bus, 40, chofer);
    }

    /** Cuerpo de POST /trips/viajes solo con la ida. */
    private String soloIda(UUID rutaId, UUID busId, UUID choferId) {
        return """
                {"rutaId":"%s","busId":"%s","choferId":"%s","fechaSalidaProgramada":"%s",
                 "fechaLlegadaEstimada":"%s","ventaPorChofer":true}
                """.formatted(rutaId, busId, choferId, SALIDA, LLEGADA);
    }

    /** Cuerpo de POST /trips/viajes con ida y regreso, eligiendo el bus del regreso. */
    private String conRegreso(UUID busDelRegreso) {
        return """
                {"rutaId":"%s","busId":"%s","choferId":"%s","fechaSalidaProgramada":"%s",
                 "fechaLlegadaEstimada":"%s","ventaPorChofer":false,
                 "regreso":{"busId":"%s","choferId":"%s","fechaSalidaProgramada":"%s",
                            "fechaLlegadaEstimada":"%s","ventaPorChofer":true}}
                """.formatted(ruta, bus, chofer, SALIDA, LLEGADA,
                busDelRegreso, chofer, SALIDA_REGRESO, LLEGADA_REGRESO);
    }

    private String cambio(UUID busId, UUID choferId, OffsetDateTime salida, int version) {
        return """
                {"busId":"%s","choferId":"%s","fechaSalidaProgramada":"%s","fechaLlegadaEstimada":"%s",
                 "version":%d}
                """.formatted(busId, choferId, salida, salida.plusHours(4), version);
    }

    // ----- T10 -----

    @Test
    void programarIdaYRegresoCreaLosDosViajesYPublicaLosDosEventos() {
        // Act
        ResponseEntity<String> respuesta = post("/trips/viajes", tokenAdminSucursal(xela), conRegreso(bus));

        // Assert: la respuesta es la ida, con los datos que Fleet devolvió
        esStatus(respuesta, 201);
        UUID ida = UUID.fromString(json(respuesta, "$.viajeId"));
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("PROGRAMADO");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.tipo")).isEqualTo("REGULAR");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.sentido")).isEqualTo("IDA");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.busPlaca")).isEqualTo("P-111AAA");
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.capacidad")).isEqualTo(40);
        assertThat(decimal(respuesta, "$.precioBoleto")).isEqualByComparingTo("75.00");
        // El salario no sale en la respuesta: se guarda en el viaje con lo que dijo Fleet (R1b)
        assertThat(jdbc.queryForObject("SELECT salario_base_chofer FROM viajes WHERE id = ?",
                java.math.BigDecimal.class, ida)).isEqualByComparingTo("350.00");

        // Assert: en la base quedaron los dos, y el regreso apunta a su ida
        assertThat(viajesDeLaRuta(ruta)).isEqualTo(2);
        UUID regreso = jdbc.queryForObject(
                "SELECT id FROM viajes WHERE ruta_id = ? AND sentido = 'REGRESO'", UUID.class, ruta);
        assertThat(jdbc.queryForObject("SELECT viaje_ida_id FROM viajes WHERE id = ?", UUID.class, regreso))
                .isEqualTo(ida);
        assertThat(estadoDelViaje(regreso)).isEqualTo("PROGRAMADO");

        // Assert: dos ViajeProgramado en el outbox (uno por viaje), con el payload que leen los demás
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM eventos_salida WHERE tipo_evento = 'ViajeProgramado'", Integer.class))
                .isEqualTo(2);
        String evento = esperarEventos("ViajeProgramado").get("ViajeProgramado");
        assertThat(JsonPath.<String>read(evento, "$.payload.busId")).isEqualTo(bus.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.choferId")).isEqualTo(chofer.toString());
        assertThat(JsonPath.<Integer>read(evento, "$.payload.capacidad")).isEqualTo(40);
    }

    @Test
    void sinElBloqueDeRegresoSoloSeProgramaLaIda() {
        // Act
        ResponseEntity<String> respuesta = post("/trips/viajes", tokenAdminSucursal(xela),
                soloIda(ruta, bus, chofer));

        // Assert
        esStatus(respuesta, 201);
        assertThat(PruebaIntegracion.<Boolean>json(respuesta, "$.ventaPorChofer")).isTrue();
        assertThat(viajesDeLaRuta(ruta)).isEqualTo(1);
        esperarEventos("ViajeProgramado");
    }

    @Test
    void elRegresoNoPuedeIrEnOtroBusYNoSeCreaNiLaIda() {
        // Arrange: el regreso pide un bus distinto al de la ida
        UUID otroBus = insertarBus(xela, "P-222BBB");

        // Act
        ResponseEntity<String> respuesta = post("/trips/viajes", tokenAdminSucursal(xela), conRegreso(otroBus));

        // Assert: se rechaza antes de crear nada, por eso la ida tampoco queda programada
        esError(respuesta, 422, "REGRESO_CON_OTRO_BUS");
        assertThat(viajesDeLaRuta(ruta)).isZero();
    }

    @Test
    void enUnaRutaInactivaNoSeProgramaYFleetNiSeEntera() {
        // Arrange: otra sucursal de destino, porque rutas tiene UNIQUE (origen, destino).
        // El origen sigue siendo Xela para que el admin tenga acceso y el 422 sea por la ruta inactiva.
        UUID peten = insertarSucursal("Peten", true);
        UUID inactiva = insertarRuta(xela, peten, false);

        // Act + Assert
        esError(post("/trips/viajes", tokenAdminSucursal(xela), soloIda(inactiva, bus, chofer)),
                422, "RUTA_INACTIVA");
        assertThat(viajesDeLaRuta(inactiva)).isZero();
    }

    @Test
    void unAdminDeOtraSucursalNoProgramaEnEstaRuta() {
        // Act + Assert
        esError(post("/trips/viajes", tokenAdminSucursal(guate), soloIda(ruta, bus, chofer)),
                403, "SUCURSAL_AJENA");
    }

    @Test
    void siFleetDiceQueElBusEstaEnTallerNoSeProgramaElViaje() {
        // Arrange: R1a responde que el bus no está disponible
        FLEET.responder("GET", rutaDisponibilidad(bus), 200, busFleet(bus, xela, "P-111AAA", 40, false));

        // Act + Assert
        esError(post("/trips/viajes", tokenAdminSucursal(xela), soloIda(ruta, bus, chofer)),
                422, "BUS_NO_DISPONIBLE");
        assertThat(viajesDeLaRuta(ruta)).isZero();
    }

    // ----- T11 -----

    @Test
    void reprogramarCambiandoElBusValidaConFleetYPublicaViajeActualizado() {
        // Arrange: un viaje ya programado y un bus nuevo con la misma capacidad
        UUID viaje = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "PROGRAMADO");
        UUID otroBus = insertarBus(xela, "P-222BBB");
        FLEET.responder("GET", rutaDisponibilidad(otroBus), 200, busFleet(otroBus, xela, "P-222BBB", 40, true));

        // Act
        ResponseEntity<String> respuesta = put("/trips/viajes/" + viaje, tokenAdminSucursal(xela),
                cambio(otroBus, chofer, SALIDA, 0));

        // Assert
        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.busPlaca")).isEqualTo("P-222BBB");
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.version")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT bus_id FROM viajes WHERE id = ?", UUID.class, viaje))
                .isEqualTo(otroBus);
        esperarEventos("ViajeActualizado", "ViajeReasignado");
    }

    @Test
    void reprogramarAUnBusConMenosAsientosSeRechaza() {
        // Arrange: el viaje se creó con 40 asientos y el bus nuevo trae 20
        UUID viaje = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "PROGRAMADO");
        UUID chico = insertarBus(xela, "P-333CCC");
        FLEET.responder("GET", rutaDisponibilidad(chico), 200, busFleet(chico, xela, "P-333CCC", 20, true));

        // Act + Assert
        esError(put("/trips/viajes/" + viaje, tokenAdminSucursal(xela), cambio(chico, chofer, SALIDA, 0)),
                422, "CAPACIDAD_MENOR");
        assertThat(jdbc.queryForObject("SELECT bus_id FROM viajes WHERE id = ?", UUID.class, viaje))
                .isEqualTo(bus);
    }
}
