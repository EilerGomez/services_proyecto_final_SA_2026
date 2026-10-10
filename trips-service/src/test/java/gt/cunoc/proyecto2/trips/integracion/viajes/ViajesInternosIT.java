package gt.cunoc.proyecto2.trips.integracion.viajes;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.trips.integracion.soporte.PruebaIntegracion;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** R2 (Sales: ¿es vendible?) y R3 (Fleet: viajes activos de un bus o chofer), solo con token de servicio. */
class ViajesInternosIT extends PruebaIntegracion {

    private static final String INTERNO = "/trips/interno/viajes";

    private UUID xela;
    private UUID ruta;
    private UUID bus;
    private UUID chofer;

    @BeforeEach
    void datos() {
        xela = insertarSucursal("Quetzaltenango", true);
        ruta = insertarRuta(xela, insertarSucursal("Guatemala", true), true);
        bus = insertarBus(xela, "P-111AAA");
        chofer = insertarChofer(xela, "Juan Perez");
    }

    // ----- R2 -----

    @Test
    void unViajeRegularProgramadoEsVendible() {
        UUID viaje = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "PROGRAMADO");

        ResponseEntity<String> respuesta = get(INTERNO + "/" + viaje + "/vendible", tokenServicio());

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<Boolean>json(respuesta, "$.vendible")).isTrue();
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("PROGRAMADO");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.tipo")).isEqualTo("REGULAR");
        assertThat(decimal(respuesta, "$.precioBoleto")).isEqualByComparingTo("75.00");
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.capacidad")).isEqualTo(40);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.sucursalOrigenId")).isEqualTo(xela.toString());
        assertThat(fecha(respuesta, "$.fechaSalidaProgramada")).isEqualTo(SALIDA);
    }

    @Test
    void unViajeQueNoExisteNoEsVendiblePeroResponde200() {
        UUID viaje = UUID.randomUUID();

        ResponseEntity<String> respuesta = get(INTERNO + "/" + viaje + "/vendible", tokenServicio());

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.viajeId")).isEqualTo(viaje.toString());
        assertThat(PruebaIntegracion.<Boolean>json(respuesta, "$.vendible")).isFalse();
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.motivo")).isEqualTo("VIAJE_NO_EXISTE");
    }

    @Test
    void unViajePrivadoOQueYaSalioNoEsVendible() {
        UUID alquiler = insertarAlquiler(xela, UUID.randomUUID(), "ASIGNADO");
        UUID privado = insertarViajePrivado(alquiler, xela, bus, chofer, SALIDA, "PROGRAMADO");
        UUID enRuta = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA.minusDays(1), "EN_RUTA");

        ResponseEntity<String> dePrivado = get(INTERNO + "/" + privado + "/vendible", tokenServicio());
        ResponseEntity<String> deEnRuta = get(INTERNO + "/" + enRuta + "/vendible", tokenServicio());

        assertThat(PruebaIntegracion.<Boolean>json(dePrivado, "$.vendible")).isFalse();
        assertThat(PruebaIntegracion.<String>json(dePrivado, "$.motivo")).isEqualTo("VIAJE_PRIVADO");
        assertThat(PruebaIntegracion.<Boolean>json(deEnRuta, "$.vendible")).isFalse();
        assertThat(PruebaIntegracion.<String>json(deEnRuta, "$.motivo")).isEqualTo("VIAJE_YA_INICIO");
    }

    // ----- R3 -----

    @Test
    void viajesActivosDeUnBusOrdenadosPorSalida() {
        UUID programado = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "PROGRAMADO");
        UUID enRuta = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA.minusDays(1), "EN_RUTA");
        insertarViajeRegular(ruta, xela, bus, chofer, SALIDA.minusDays(5), "COMPLETADO");
        insertarViajeRegular(ruta, xela, UUID.randomUUID(), chofer, SALIDA.plusDays(1), "PROGRAMADO");

        ResponseEntity<String> respuesta = get(INTERNO + "/activos?busId=" + bus, tokenServicio());

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.cantidad")).isEqualTo(2);
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$.viajes[*].viajeId"))
                .containsExactly(enRuta.toString(), programado.toString());
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$.viajes[*].estado"))
                .containsExactly("EN_RUTA", "PROGRAMADO");
        assertThat(fecha(respuesta, "$.viajes[1].fechaSalidaProgramada")).isEqualTo(SALIDA);
    }

    @Test
    void viajesActivosDeUnChofer() {
        insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "PROGRAMADO");
        insertarViajeRegular(ruta, xela, UUID.randomUUID(), chofer, SALIDA.plusDays(1), "PROGRAMADO");
        insertarViajeRegular(ruta, xela, bus, UUID.randomUUID(), SALIDA.plusDays(2), "PROGRAMADO");

        ResponseEntity<String> respuesta = get(INTERNO + "/activos?choferId=" + chofer, tokenServicio());
        ResponseEntity<String> sinViajes = get(INTERNO + "/activos?choferId=" + UUID.randomUUID(), tokenServicio());

        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.cantidad")).isEqualTo(2);
        assertThat(PruebaIntegracion.<Integer>json(sinViajes, "$.cantidad")).isZero();
        assertThat(PruebaIntegracion.<List<Object>>json(sinViajes, "$.viajes")).isEmpty();
    }

    @Test
    void hayQueIndicarSoloBusOSoloChofer() {
        esError(get(INTERNO + "/activos", tokenServicio()), 400, "FILTRO_REQUERIDO");
        esError(get(INTERNO + "/activos?busId=" + bus + "&choferId=" + chofer, tokenServicio()),
                400, "FILTRO_REQUERIDO");
    }

    @Test
    void soloOtrosServiciosUsanLosEndpointsInternos() {
        UUID viaje = insertarViajeRegular(ruta, xela, bus, chofer, SALIDA, "PROGRAMADO");

        esError(get(INTERNO + "/" + viaje + "/vendible", tokenAdminSistema()), 403, "ROL_INSUFICIENTE");
        esError(get(INTERNO + "/activos?busId=" + bus, tokenAdminSucursal(xela)), 403, "ROL_INSUFICIENTE");
        esError(get(INTERNO + "/activos?busId=" + bus, null), 401, "NO_AUTENTICADO");
        esError(get(INTERNO + "/activos?busId=" + bus, "token-alterado"), 401, "TOKEN_INVALIDO");
    }
}
