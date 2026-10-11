package gt.cunoc.proyecto2.sales.dtos.historial;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DtosHistorialTest {

    private static final ZoneOffset GUATEMALA = ZoneOffset.ofHours(-6);

    // ----- VentaHistorialRespuesta -----

    @Test
    void ventaConViajeYAsientosOrdenados() {
        VistaViajeEntidad viaje = DatosPrueba.vistaViaje();
        // 14:00 UTC = 08:00 en Guatemala
        ReflectionTestUtils.setField(viaje, "fechaSalida", OffsetDateTime.of(2026, 11, 5, 14, 0, 0, 0, ZoneOffset.UTC));
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 3);

        VentaHistorialRespuesta respuesta = new VentaHistorialRespuesta(venta, viaje,
                DatosPrueba.boletos(venta, 9, 2, 5));

        assertThat(respuesta.ventaId()).isEqualTo(venta.getId());
        assertThat(respuesta.canal()).isEqualTo(CanalVenta.PRESENCIAL);
        assertThat(respuesta.origenNombre()).isEqualTo("Quetzaltenango");
        assertThat(respuesta.destinoNombre()).isEqualTo("Guatemala");
        assertThat(respuesta.fechaSalida()).isEqualTo(OffsetDateTime.of(2026, 11, 5, 8, 0, 0, 0, GUATEMALA));
        assertThat(respuesta.fechaSalida().getOffset()).isEqualTo(GUATEMALA);
        assertThat(respuesta.asientos()).containsExactly(2, 5, 9);
        assertThat(respuesta.montoTotal()).isEqualByComparingTo("225.00");
        assertThat(respuesta.fechaPago()).isEqualTo(FECHA_PAGO);
        assertThat(respuesta.estado()).isEqualTo(EstadoVenta.CONFIRMADA);
    }

    @Test
    void ventaDeViajeEliminadoSinBoletos() {
        VentaEntidad venta = DatosPrueba.ventaEnLinea(DatosPrueba.vistaViaje(), UUID.randomUUID(), 2);
        venta.rechazar("VIAJE_ELIMINADO");

        VentaHistorialRespuesta respuesta = new VentaHistorialRespuesta(venta, null, List.of());

        assertThat(respuesta.canal()).isEqualTo(CanalVenta.EN_LINEA);
        assertThat(respuesta.origenNombre()).isNull();
        assertThat(respuesta.destinoNombre()).isNull();
        assertThat(respuesta.fechaSalida()).isNull();
        assertThat(respuesta.asientos()).isEmpty();
        assertThat(respuesta.montoTotal()).isEqualByComparingTo("150.00");
        assertThat(respuesta.estado()).isEqualTo(EstadoVenta.RECHAZADA);
    }

    // ----- AlquilerHistorialRespuesta -----

    @Test
    void alquilerPagadoEnElHistorial() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(UUID.randomUUID());
        orden.pagarEnEfectivo(FECHA_PAGO);

        AlquilerHistorialRespuesta respuesta = new AlquilerHistorialRespuesta(orden);

        assertThat(respuesta.ordenId()).isEqualTo(orden.getId());
        assertThat(respuesta.alquilerId()).isEqualTo(orden.getAlquilerId());
        assertThat(respuesta.origen()).isEqualTo("Quetzaltenango");
        assertThat(respuesta.destino()).isEqualTo("Antigua Guatemala");
        assertThat(respuesta.monto()).isEqualByComparingTo("2600.00");
        assertThat(respuesta.metodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(respuesta.fechaPago()).isEqualTo(FECHA_PAGO);
        assertThat(respuesta.estado()).isEqualTo(EstadoOrdenAlquiler.PAGADA);
    }

    @Test
    void alquilerPendienteSinMetodoNiFecha() {
        AlquilerHistorialRespuesta respuesta = new AlquilerHistorialRespuesta(DatosPrueba.orden(null));

        assertThat(respuesta.metodoPago()).isNull();
        assertThat(respuesta.fechaPago()).isNull();
        assertThat(respuesta.estado()).isEqualTo(EstadoOrdenAlquiler.PENDIENTE_PAGO);
    }

    // ----- HistorialRespuesta -----

    @Test
    void historialAgrupaVentasYAlquileresDelNit() {
        VistaViajeEntidad viaje = DatosPrueba.vistaViaje();
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 1);
        VentaHistorialRespuesta compra = new VentaHistorialRespuesta(venta, viaje, DatosPrueba.boletos(venta, 1));
        AlquilerHistorialRespuesta alquiler = new AlquilerHistorialRespuesta(DatosPrueba.orden(null));

        HistorialRespuesta historial = new HistorialRespuesta("1234567-8", "Ana López", List.of(compra),
                List.of(alquiler));

        assertThat(historial.nit()).isEqualTo("1234567-8");
        assertThat(historial.nombre()).isEqualTo("Ana López");
        assertThat(historial.ventas()).containsExactly(compra);
        assertThat(historial.alquileres()).containsExactly(alquiler);
    }
}
