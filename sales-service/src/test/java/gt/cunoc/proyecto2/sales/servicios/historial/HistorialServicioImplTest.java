package gt.cunoc.proyecto2.sales.servicios.historial;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static gt.cunoc.proyecto2.sales.DatosPrueba.SALIDA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.dtos.historial.AlquilerHistorialRespuesta;
import gt.cunoc.proyecto2.sales.dtos.historial.HistorialRespuesta;
import gt.cunoc.proyecto2.sales.dtos.historial.VentaHistorialRespuesta;
import gt.cunoc.proyecto2.sales.errores.historial.ErrorHistorial;
import gt.cunoc.proyecto2.sales.errores.ventas.ErrorVenta;
import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.repositorios.alquileres.OrdenAlquilerRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.BoletoRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.VentaRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.viajes.VistaViajeRepositorio;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class HistorialServicioImplTest {

    private static final Sort RECIENTES_PRIMERO = Sort.by(Sort.Order.desc("fechaPago"), Sort.Order.desc("id"));

    private final VistaClienteRepositorio clientes = mock(VistaClienteRepositorio.class);
    private final VentaRepositorio ventas = mock(VentaRepositorio.class);
    private final BoletoRepositorio boletos = mock(BoletoRepositorio.class);
    private final VistaViajeRepositorio viajes = mock(VistaViajeRepositorio.class);
    private final OrdenAlquilerRepositorio ordenes = mock(OrdenAlquilerRepositorio.class);

    private final HistorialServicioImpl servicio = new HistorialServicioImpl(clientes, ventas, boletos, viajes,
            ordenes);

    private final VistaViajeEntidad viaje = DatosPrueba.vistaViaje();

    /** Compra en mostrador con NIT y nombre escritos por el cajero. */
    private VentaEntidad ventaMostradorDe(String nit, String nombre) {
        return VentaEntidad.registrarEnMostrador(viaje, new ClienteVenta(null, nit, nombre, null), 1, FECHA_PAGO,
                UUID.randomUUID());
    }

    private void comprasDelNit(String nit, VentaEntidad... suyas) {
        when(ventas.findByClienteNit(eq(nit), any(Sort.class))).thenReturn(Arrays.asList(suyas));
    }

    private void alquileresDelNit(String nit, OrdenAlquilerEntidad... suyos) {
        when(ordenes.findByClienteNitOrderByFechaCreacionDesc(nit)).thenReturn(Arrays.asList(suyos));
    }

    // ----- S7: delCliente -----

    @Test
    void historialDelClienteSinPerfil() {
        UsuarioActual cliente = DatosPrueba.cliente();
        when(clientes.findById(cliente.usuarioId())).thenReturn(Optional.empty());

        VerificarError.lanza(() -> servicio.delCliente(cliente), ErrorVenta.CLIENTE_SIN_PERFIL);
        verifyNoInteractions(ventas, boletos, viajes, ordenes);
    }

    @Test
    void historialDeConsumidorFinalNoSePermite() {
        VerificarError.lanza(() -> servicio.porNit("CF"), ErrorHistorial.NIT_CONSUMIDOR_FINAL);
        VerificarError.lanza(() -> servicio.porNit(" cf "), ErrorHistorial.NIT_CONSUMIDOR_FINAL);
        verifyNoInteractions(clientes, ventas, boletos, viajes, ordenes);
    }

    @Test
    void historialPorNitLoNormalizaYUsaElNombreDelPerfil() {
        when(clientes.findFirstByNit("1234567-K")).thenReturn(Optional.of(
                DatosPrueba.vistaCliente(UUID.randomUUID(), "1234567-K", "Ana López", "ana@correo.com")));
        comprasDelNit("1234567-K");
        alquileresDelNit("1234567-K");

        HistorialRespuesta historial = servicio.porNit(" 1234567-k ");

        assertThat(historial.nit()).isEqualTo("1234567-K");
        assertThat(historial.nombre()).isEqualTo("Ana López");
        assertThat(historial.ventas()).isEmpty();
        assertThat(historial.alquileres()).isEmpty();
        verify(ventas).findByClienteNit("1234567-K", RECIENTES_PRIMERO);
        // Sin ventas no se consultan boletos
        verifyNoInteractions(boletos);
    }

    @Test
    void historialPorNitSinPerfilTomaElNombreDeLaCompraMasReciente() {
        when(clientes.findFirstByNit("555555-5")).thenReturn(Optional.empty());
        VentaEntidad reciente = ventaMostradorDe("555555-5", "Pedro Ruiz");
        VentaEntidad anterior = ventaMostradorDe("555555-5", "P. Ruiz");
        comprasDelNit("555555-5", reciente, anterior);
        when(boletos.findByVentaIdIn(List.of(reciente.getId(), anterior.getId()))).thenReturn(List.of());
        when(viajes.findAllById(any())).thenReturn(List.of(viaje));
        alquileresDelNit("555555-5", DatosPrueba.orden(null));

        HistorialRespuesta historial = servicio.porNit("555555-5");

        assertThat(historial.nombre()).isEqualTo("Pedro Ruiz");
        assertThat(historial.ventas()).hasSize(2);
        assertThat(historial.ventas()).allSatisfy(v -> assertThat(v.asientos()).isEmpty());
        assertThat(historial.alquileres()).hasSize(1);
        // Las dos ventas son del mismo viaje: se busca una sola vez
        verify(viajes).findAllById(List.of(viaje.getViajeId()));
    }

    @Test
    void historialPorNitSinPerfilNiComprasTomaElNombreDelAlquiler() {
        when(clientes.findFirstByNit("1234567-8")).thenReturn(Optional.empty());
        comprasDelNit("1234567-8");
        alquileresDelNit("1234567-8", DatosPrueba.orden(null), DatosPrueba.orden(UUID.randomUUID()));

        HistorialRespuesta historial = servicio.porNit("1234567-8");

        assertThat(historial.nombre()).isEqualTo("Colegio San José");
        assertThat(historial.ventas()).isEmpty();
        assertThat(historial.alquileres()).hasSize(2);
        verifyNoInteractions(boletos);
    }

    @Test
    void historialPorNitSinNadaNoTieneNombre() {
        when(clientes.findFirstByNit("999999-9")).thenReturn(Optional.empty());
        comprasDelNit("999999-9");
        alquileresDelNit("999999-9");

        HistorialRespuesta historial = servicio.porNit("999999-9");

        assertThat(historial.nit()).isEqualTo("999999-9");
        assertThat(historial.nombre()).isNull();
        assertThat(historial.ventas()).isEmpty();
        assertThat(historial.alquileres()).isEmpty();
    }
}
