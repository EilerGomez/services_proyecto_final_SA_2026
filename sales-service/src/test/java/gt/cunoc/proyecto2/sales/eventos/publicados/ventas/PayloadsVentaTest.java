package gt.cunoc.proyecto2.sales.eventos.publicados.ventas;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PayloadsVentaTest {

    private static final ZoneOffset GUATEMALA = ZoneOffset.ofHours(-6);
    /** 14:00 UTC = 08:00 en Guatemala. */
    private static final OffsetDateTime SALIDA_UTC = OffsetDateTime.of(2026, 11, 5, 14, 0, 0, 0, ZoneOffset.UTC);

    private final UUID clienteId = UUID.randomUUID();

    private VistaViajeEntidad viajeConSalidaEnUtc() {
        VistaViajeEntidad viaje = DatosPrueba.vistaViaje();
        ReflectionTestUtils.setField(viaje, "fechaSalida", SALIDA_UTC);
        return viaje;
    }

    // ----- BoletoVendido -----

    @Test
    void boletoVendidoDeUnaVentaEnLinea() {
        VistaViajeEntidad viaje = viajeConSalidaEnUtc();
        VentaEntidad venta = DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 3);
        List<BoletoEntidad> boletos = DatosPrueba.boletos(venta, 7, 3, 5);

        BoletoVendido evento = BoletoVendido.desde(venta, boletos, viaje);

        assertThat(evento.ventaId()).isEqualTo(venta.getId());
        assertThat(evento.viajeId()).isEqualTo(viaje.getViajeId());
        assertThat(evento.rutaId()).isEqualTo(viaje.getRutaId());
        assertThat(evento.busId()).isEqualTo(viaje.getBusId());
        assertThat(evento.busPlaca()).isEqualTo("P-123ABC");
        assertThat(evento.sucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(evento.canal()).isEqualTo(CanalVenta.EN_LINEA);
        assertThat(evento.clienteUsuarioId()).isEqualTo(clienteId);
        assertThat(evento.clienteNit()).isEqualTo("1234567-8");
        assertThat(evento.clienteNombre()).isEqualTo("Ana López");
        assertThat(evento.clienteCorreo()).isEqualTo("ana@correo.com");
        assertThat(evento.asientos()).containsExactly(3, 5, 7);
        assertThat(evento.cantidadBoletos()).isEqualTo(3);
        assertThat(evento.montoTotal()).isEqualByComparingTo("225.00");
        assertThat(evento.metodoPago()).isEqualTo(MetodoPago.CARTERA);
        assertThat(evento.fechaPago()).isEqualTo(FECHA_PAGO);
        assertThat(evento.origenNombre()).isEqualTo("Quetzaltenango");
        assertThat(evento.destinoNombre()).isEqualTo("Guatemala");
        assertThat(evento.fechaSalida()).isEqualTo(OffsetDateTime.of(2026, 11, 5, 8, 0, 0, 0, GUATEMALA));
        assertThat(evento.fechaSalida().getOffset()).isEqualTo(GUATEMALA);
        assertThat(evento.fechaSalida()).isAtSameInstantAs(SALIDA_UTC);
    }

    @Test
    void boletoVendidoEnMostradorAConsumidorFinal() {
        VistaViajeEntidad viaje = DatosPrueba.vistaViaje();
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 1);

        BoletoVendido evento = BoletoVendido.desde(venta, DatosPrueba.boletos(venta, 12), viaje);

        assertThat(evento.canal()).isEqualTo(CanalVenta.PRESENCIAL);
        assertThat(evento.metodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(evento.clienteUsuarioId()).isNull();
        assertThat(evento.clienteNit()).isEqualTo("CF");
        assertThat(evento.clienteNombre()).isEqualTo("Consumidor final");
        assertThat(evento.clienteCorreo()).isNull();
        assertThat(evento.asientos()).containsExactly(12);
        assertThat(evento.cantidadBoletos()).isEqualTo(1);
        assertThat(evento.fechaSalida()).isEqualTo(DatosPrueba.SALIDA);
    }

    // ----- VentaCancelada -----

    @Test
    void ventaCanceladaConElViajeTodaviaEnLaVista() {
        VistaViajeEntidad viaje = viajeConSalidaEnUtc();
        VentaEntidad venta = DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 2);
        LocalDate fecha = FECHA_PAGO.plusDays(1);
        venta.cancelar("El cliente ya no viaja", fecha);

        VentaCancelada evento = VentaCancelada.desde(venta, DatosPrueba.boletos(venta, 10, 4), viaje);

        assertThat(evento.ventaId()).isEqualTo(venta.getId());
        assertThat(evento.viajeId()).isEqualTo(viaje.getViajeId());
        assertThat(evento.sucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(evento.clienteUsuarioId()).isEqualTo(clienteId);
        assertThat(evento.clienteNit()).isEqualTo("1234567-8");
        assertThat(evento.clienteNombre()).isEqualTo("Ana López");
        assertThat(evento.clienteCorreo()).isEqualTo("ana@correo.com");
        assertThat(evento.asientos()).containsExactly(4, 10);
        assertThat(evento.cantidadBoletos()).isEqualTo(2);
        assertThat(evento.montoAnulado()).isEqualByComparingTo("150.00");
        assertThat(evento.metodoPago()).isEqualTo(MetodoPago.CARTERA);
        assertThat(evento.motivo()).isEqualTo("El cliente ya no viaja");
        assertThat(evento.fechaCancelacion()).isEqualTo(fecha);
        assertThat(evento.origenNombre()).isEqualTo("Quetzaltenango");
        assertThat(evento.destinoNombre()).isEqualTo("Guatemala");
        assertThat(evento.fechaSalida()).isEqualTo(OffsetDateTime.of(2026, 11, 5, 8, 0, 0, 0, GUATEMALA));
        assertThat(evento.fechaSalida().getOffset()).isEqualTo(GUATEMALA);
    }

    @Test
    void ventaCanceladaDeUnViajeQueYaNoEstaEnLaVista() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(DatosPrueba.vistaViaje(), 1);
        venta.cancelarPorViajeEliminado("VIAJE_ELIMINADO", FECHA_PAGO.minusDays(2));

        VentaCancelada evento = VentaCancelada.desde(venta, DatosPrueba.boletos(venta, 6), null);

        assertThat(evento.viajeId()).isEqualTo(venta.getViajeId());
        assertThat(evento.clienteUsuarioId()).isNull();
        assertThat(evento.clienteNit()).isEqualTo("CF");
        assertThat(evento.asientos()).containsExactly(6);
        assertThat(evento.cantidadBoletos()).isEqualTo(1);
        assertThat(evento.montoAnulado()).isEqualByComparingTo("75.00");
        assertThat(evento.metodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(evento.motivo()).isEqualTo("VIAJE_ELIMINADO");
        assertThat(evento.fechaCancelacion()).isEqualTo(FECHA_PAGO);
        assertThat(evento.origenNombre()).isNull();
        assertThat(evento.destinoNombre()).isNull();
        assertThat(evento.fechaSalida()).isNull();
    }

    @Test
    void ventaCanceladaSinBoletos() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(DatosPrueba.vistaViaje(), 2);
        venta.cancelar("Motivo", FECHA_PAGO);

        VentaCancelada evento = VentaCancelada.desde(venta, List.of(), null);

        assertThat(evento.asientos()).isEmpty();
        assertThat(evento.cantidadBoletos()).isZero();
        assertThat(evento.montoAnulado()).isEqualByComparingTo("150.00");
    }

    // ----- VentaReembolsada -----

    @Test
    void ventaReembolsadaDeUnBoletoDevueltoEnLaSucursal() {
        UUID cajeroId = UUID.randomUUID();
        VistaViajeEntidad viaje = viajeConSalidaEnUtc();
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 2);
        venta.cancelar("Viaje eliminado · Quetzaltenango → Guatemala", FECHA_PAGO);
        venta.reembolsarEnEfectivo(FECHA_PAGO.plusDays(1), cajeroId);
        List<BoletoEntidad> boletos = DatosPrueba.boletos(venta, 11, 4);

        VentaReembolsada evento = VentaReembolsada.desde(venta, boletos, viaje);

        assertThat(evento.ventaId()).isEqualTo(venta.getId());
        assertThat(evento.codigo()).isEqualTo(venta.codigo());
        assertThat(evento.viajeId()).isEqualTo(viaje.getViajeId());
        assertThat(evento.asientos()).containsExactly(4, 11);
        assertThat(evento.cantidadBoletos()).isEqualTo(2);
        assertThat(evento.montoReembolsado()).isEqualByComparingTo("150.00");
        assertThat(evento.metodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(evento.motivo()).isEqualTo("Viaje eliminado · Quetzaltenango → Guatemala");
        assertThat(evento.fechaReembolso()).isEqualTo(FECHA_PAGO.plusDays(1));
        assertThat(evento.reembolsadoPor()).isEqualTo(cajeroId);
        assertThat(evento.origenNombre()).isEqualTo("Quetzaltenango");
        assertThat(evento.fechaSalida()).isEqualTo(OffsetDateTime.of(2026, 11, 5, 8, 0, 0, 0, GUATEMALA));
    }

    @Test
    void ventaReembolsadaDeUnViajeQueYaNoEstaEnLaVista() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(DatosPrueba.vistaViaje(), 1);
        venta.cancelarPorViajeEliminado("Viaje eliminado · Bus averiado", FECHA_PAGO);
        venta.reembolsarEnEfectivo(FECHA_PAGO, UUID.randomUUID());

        VentaReembolsada evento = VentaReembolsada.desde(venta, DatosPrueba.boletos(venta, 6), null);

        assertThat(evento.clienteUsuarioId()).isNull();
        assertThat(evento.clienteNit()).isEqualTo("CF");
        assertThat(evento.motivo()).isEqualTo("Viaje eliminado · Bus averiado");
        assertThat(evento.origenNombre()).isNull();
        assertThat(evento.destinoNombre()).isNull();
        assertThat(evento.fechaSalida()).isNull();
    }
}
