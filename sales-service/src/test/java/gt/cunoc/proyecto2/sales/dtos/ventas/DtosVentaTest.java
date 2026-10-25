package gt.cunoc.proyecto2.sales.dtos.ventas;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.errores.ventas.ErrorVenta;
import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoBoleto;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DtosVentaTest {

    private static final ZoneOffset GUATEMALA = ZoneOffset.ofHours(-6);

    private final UUID viajeId = UUID.randomUUID();

    private NuevaVentaMostradorSolicitud mostrador(String nit, String nombre, String correo) {
        return new NuevaVentaMostradorSolicitud(viajeId, List.of(1), nit, nombre, correo, FECHA_PAGO);
    }

    // ----- Asientos -----

    @Test
    void asientosValidosSalenOrdenados() {
        assertThat(Asientos.validar(List.of(12, 3, 7), 40)).containsExactly(3, 7, 12);
    }

    @Test
    void elUltimoAsientoDelBusEsValido() {
        assertThat(Asientos.validar(List.of(40, 1), 40)).containsExactly(1, 40);
    }

    @Test
    void asientoRepetidoEsAsientosRepetidos() {
        assertThatThrownBy(() -> Asientos.validar(List.of(3, 5, 3), 40))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorVenta.ASIENTOS_REPETIDOS.codigo());
                    assertThat(e.getStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void losRepetidosSeRevisanAntesQueElRango() {
        VerificarError.lanza(() -> Asientos.validar(List.of(41, 41), 40), ErrorVenta.ASIENTOS_REPETIDOS);
    }

    @Test
    void asientoMayorQueLaCapacidadEsFueraDeRango() {
        assertThatThrownBy(() -> Asientos.validar(List.of(2, 41), 40))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(ErrorVenta.ASIENTO_FUERA_DE_RANGO.codigo());
                    assertThat(e.getStatus().value()).isEqualTo(400);
                    assertThat(e.getMessage()).isEqualTo("El bus tiene asientos del 1 al 40; el 41 no existe");
                });
    }

    // ----- NuevaVentaEnLineaSolicitud -----

    @Test
    void ventaEnLineaValidaYOrdenaLosAsientos() {
        NuevaVentaEnLineaSolicitud solicitud = new NuevaVentaEnLineaSolicitud(viajeId, List.of(9, 2), FECHA_PAGO);

        assertThat(solicitud.asientosValidados(40)).containsExactly(2, 9);
        assertThat(solicitud.viajeId()).isEqualTo(viajeId);
        assertThat(solicitud.fechaPago()).isEqualTo(FECHA_PAGO);
    }

    @Test
    void ventaEnLineaConAsientosInvalidos() {
        NuevaVentaEnLineaSolicitud repetidos = new NuevaVentaEnLineaSolicitud(viajeId, List.of(2, 2), FECHA_PAGO);
        NuevaVentaEnLineaSolicitud fuera = new NuevaVentaEnLineaSolicitud(viajeId, List.of(31), FECHA_PAGO);

        VerificarError.lanza(() -> repetidos.asientosValidados(40), ErrorVenta.ASIENTOS_REPETIDOS);
        VerificarError.lanza(() -> fuera.asientosValidados(30), ErrorVenta.ASIENTO_FUERA_DE_RANGO);
    }

    // ----- NuevaVentaMostradorSolicitud -----

    @Test
    void ventaMostradorNormalizaLosDatosDelCliente() {
        NuevaVentaMostradorSolicitud solicitud = mostrador(" 1234567-k ", "  Ana López ", " Ana.Lopez@Correo.COM ");

        assertThat(solicitud.clienteNit()).isEqualTo("1234567-K");
        assertThat(solicitud.clienteNombre()).isEqualTo("Ana López");
        assertThat(solicitud.clienteCorreo()).isEqualTo("ana.lopez@correo.com");
    }

    @Test
    void ventaMostradorConDatosVaciosLosDejaEnNull() {
        NuevaVentaMostradorSolicitud solicitud = mostrador("   ", "", "  ");

        assertThat(solicitud.clienteNit()).isNull();
        assertThat(solicitud.clienteNombre()).isNull();
        assertThat(solicitud.clienteCorreo()).isNull();
    }

    @Test
    void ventaMostradorValidaYOrdenaLosAsientos() {
        NuevaVentaMostradorSolicitud solicitud = new NuevaVentaMostradorSolicitud(viajeId, List.of(5, 1, 3), null,
                null, null, FECHA_PAGO);

        assertThat(solicitud.asientosValidados(40)).containsExactly(1, 3, 5);
        VerificarError.lanza(() -> solicitud.asientosValidados(4), ErrorVenta.ASIENTO_FUERA_DE_RANGO);
    }

    @Test
    void sinNitNiRegistroEsConsumidorFinal() {
        ClienteVenta cliente = mostrador(null, null, null).cliente(Optional.empty());

        assertThat(cliente.usuarioId()).isNull();
        assertThat(cliente.nit()).isEqualTo("CF");
        assertThat(cliente.nombre()).isEqualTo("Consumidor final");
        assertThat(cliente.correo()).isNull();
        assertThat(cliente.esConsumidorFinal()).isTrue();
    }

    @Test
    void nitSinRegistroUsaLoQueEscribioElCajero() {
        ClienteVenta cliente = mostrador("9876543-2", "Luis Pérez", "luis@correo.com").cliente(Optional.empty());

        assertThat(cliente.nit()).isEqualTo("9876543-2");
        assertThat(cliente.nombre()).isEqualTo("Luis Pérez");
        assertThat(cliente.correo()).isEqualTo("luis@correo.com");
        assertThat(cliente.esConsumidorFinal()).isFalse();
    }

    @Test
    void clienteRegistradoCompletaNombreYCorreoQueFaltan() {
        UUID usuarioId = UUID.randomUUID();
        VistaClienteEntidad registrado = DatosPrueba.vistaCliente(usuarioId);

        ClienteVenta cliente = mostrador("1234567-8", null, null).cliente(Optional.of(registrado));

        assertThat(cliente.usuarioId()).isEqualTo(usuarioId);
        assertThat(cliente.nit()).isEqualTo("1234567-8");
        assertThat(cliente.nombre()).isEqualTo("Ana López");
        assertThat(cliente.correo()).isEqualTo("ana@correo.com");
    }

    @Test
    void loQueEscribioElCajeroTienePrioridadSobreElRegistro() {
        VistaClienteEntidad registrado = DatosPrueba.vistaCliente(UUID.randomUUID());

        ClienteVenta cliente = mostrador("1234567-8", "Ana María López", "otra@correo.com")
                .cliente(Optional.of(registrado));

        assertThat(cliente.nombre()).isEqualTo("Ana María López");
        assertThat(cliente.correo()).isEqualTo("otra@correo.com");
    }

    @Test
    void registroSoloCompletaElCampoQueFalta() {
        VistaClienteEntidad registrado = DatosPrueba.vistaCliente(UUID.randomUUID());

        ClienteVenta cliente = mostrador("1234567-8", "Ana M. López", null).cliente(Optional.of(registrado));

        assertThat(cliente.nombre()).isEqualTo("Ana M. López");
        assertThat(cliente.correo()).isEqualTo("ana@correo.com");
    }

    // ----- CancelarVentaSolicitud -----

    @Test
    void cancelarQuitaEspaciosDelMotivo() {
        CancelarVentaSolicitud solicitud = new CancelarVentaSolicitud("  El cliente ya no viaja ", FECHA_PAGO, 3);

        assertThat(solicitud.motivo()).isEqualTo("El cliente ya no viaja");
        assertThat(solicitud.fechaCancelacion()).isEqualTo(FECHA_PAGO);
        assertThat(solicitud.version()).isEqualTo(3);
    }

    @Test
    void cancelarSinMotivoLoDejaEnNull() {
        assertThat(new CancelarVentaSolicitud(null, FECHA_PAGO, 0).motivo()).isNull();
    }

    // ----- BoletoRespuesta -----

    @Test
    void boletoRespuestaDesdeLaEntidad() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(DatosPrueba.vistaViaje(), 1);
        BoletoEntidad boleto = DatosPrueba.boletos(venta, 8).get(0);

        BoletoRespuesta respuesta = new BoletoRespuesta(boleto);

        assertThat(respuesta.boletoId()).isEqualTo(boleto.getId()).isNotNull();
        assertThat(respuesta.numeroAsiento()).isEqualTo(8);
        assertThat(respuesta.precio()).isEqualByComparingTo("75.00");
        assertThat(respuesta.estado()).isEqualTo(EstadoBoleto.ACTIVO);
    }

    // ----- ViajeResumenRespuesta -----

    @Test
    void resumenDelViajeConLaSalidaEnGuatemala() {
        VistaViajeEntidad viaje = DatosPrueba.vistaViaje();
        // 14:00 UTC = 08:00 en Guatemala
        OffsetDateTime salidaUtc = OffsetDateTime.of(2026, 11, 5, 14, 0, 0, 0, ZoneOffset.UTC);
        ReflectionTestUtils.setField(viaje, "fechaSalida", salidaUtc);

        ViajeResumenRespuesta resumen = new ViajeResumenRespuesta(viaje);

        assertThat(resumen.origenNombre()).isEqualTo("Quetzaltenango");
        assertThat(resumen.destinoNombre()).isEqualTo("Guatemala");
        assertThat(resumen.busPlaca()).isEqualTo("P-123ABC");
        assertThat(resumen.fechaSalida()).isEqualTo(OffsetDateTime.of(2026, 11, 5, 8, 0, 0, 0, GUATEMALA));
        assertThat(resumen.fechaSalida().getOffset()).isEqualTo(GUATEMALA);
        assertThat(resumen.fechaSalida()).isAtSameInstantAs(salidaUtc);
    }

    // ----- VentaRespuesta -----

    @Test
    void ventaRespuestaConBoletosYViaje() {
        VistaViajeEntidad viaje = DatosPrueba.vistaViaje();
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 2);
        List<BoletoEntidad> boletos = DatosPrueba.boletos(venta, 4, 5);
        ViajeResumenRespuesta resumen = new ViajeResumenRespuesta(viaje);

        VentaRespuesta respuesta = new VentaRespuesta(venta, boletos, resumen);

        assertThat(respuesta.ventaId()).isEqualTo(venta.getId());
        assertThat(respuesta.viajeId()).isEqualTo(viaje.getViajeId());
        assertThat(respuesta.canal()).isEqualTo(CanalVenta.PRESENCIAL);
        assertThat(respuesta.clienteNit()).isEqualTo("CF");
        assertThat(respuesta.clienteNombre()).isEqualTo("Consumidor final");
        assertThat(respuesta.sucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(respuesta.metodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(respuesta.montoTotal()).isEqualByComparingTo("150.00");
        assertThat(respuesta.fechaPago()).isEqualTo(FECHA_PAGO);
        assertThat(respuesta.estado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(respuesta.motivoRechazo()).isNull();
        assertThat(respuesta.motivoCancelacion()).isNull();
        assertThat(respuesta.fechaCancelacion()).isNull();
        assertThat(respuesta.boletos()).extracting(BoletoRespuesta::numeroAsiento).containsExactly(4, 5);
        assertThat(respuesta.boletos()).extracting(BoletoRespuesta::boletoId)
                .containsExactly(boletos.get(0).getId(), boletos.get(1).getId());
        assertThat(respuesta.viaje()).isSameAs(resumen);
        assertThat(respuesta.version()).isZero();
    }

    @Test
    void ventaRespuestaCanceladaSinViajeNiBoletos() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(DatosPrueba.vistaViaje(), 1);
        LocalDate fecha = FECHA_PAGO.plusDays(1);
        venta.cancelar("Viaje reprogramado", fecha);

        VentaRespuesta respuesta = new VentaRespuesta(venta, List.of(), null);

        assertThat(respuesta.estado()).isEqualTo(EstadoVenta.CANCELADA);
        assertThat(respuesta.motivoCancelacion()).isEqualTo("Viaje reprogramado");
        assertThat(respuesta.fechaCancelacion()).isEqualTo(fecha);
        assertThat(respuesta.boletos()).isEmpty();
        assertThat(respuesta.viaje()).isNull();
    }

    @Test
    void ventaRespuestaRechazadaConMotivo() {
        VentaEntidad venta = DatosPrueba.ventaEnLinea(DatosPrueba.vistaViaje(), UUID.randomUUID(), 1);
        venta.rechazar("SALDO_INSUFICIENTE");

        VentaRespuesta respuesta = new VentaRespuesta(venta, List.of(), null);

        assertThat(respuesta.canal()).isEqualTo(CanalVenta.EN_LINEA);
        assertThat(respuesta.metodoPago()).isEqualTo(MetodoPago.CARTERA);
        assertThat(respuesta.clienteNit()).isEqualTo("1234567-8");
        assertThat(respuesta.estado()).isEqualTo(EstadoVenta.RECHAZADA);
        assertThat(respuesta.motivoRechazo()).isEqualTo("SALDO_INSUFICIENTE");
    }

    // ----- VentaPendienteRespuesta -----

    @Test
    void ventaPendienteConAsientosYReserva() {
        VentaEntidad venta = DatosPrueba.ventaEnLinea(DatosPrueba.vistaViaje(), UUID.randomUUID(), 2);
        OffsetDateTime reservadoHasta = OffsetDateTime.parse("2026-11-01T10:05:00-06:00");

        VentaPendienteRespuesta respuesta = new VentaPendienteRespuesta(venta, List.of(3, 4), reservadoHasta);

        assertThat(respuesta.ventaId()).isEqualTo(venta.getId());
        assertThat(respuesta.estado()).isEqualTo(EstadoVenta.PENDIENTE_PAGO);
        assertThat(respuesta.montoTotal()).isEqualByComparingTo("150.00");
        assertThat(respuesta.asientos()).containsExactly(3, 4);
        assertThat(respuesta.reservadoHasta()).isEqualTo(reservadoHasta);
    }
}
