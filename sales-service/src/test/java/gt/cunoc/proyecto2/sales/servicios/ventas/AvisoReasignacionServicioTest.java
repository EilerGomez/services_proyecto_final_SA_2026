package gt.cunoc.proyecto2.sales.servicios.ventas;

import static gt.cunoc.proyecto2.sales.DatosPrueba.SALIDA;
import static gt.cunoc.proyecto2.sales.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.eventos.PublicadorEventosSales;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeReasignado;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.repositorios.ventas.BoletoRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.VentaRepositorio;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * A quién se le avisa cuando a un viaje le cambian el bus o el piloto: solo a quien tiene la venta
 * confirmada y dejó un correo. La venta de mostrador puede no tener correo y a esa no hay cómo
 * escribirle.
 */
class AvisoReasignacionServicioTest {

    private static final UUID VIAJE = UUID.randomUUID();

    private final VentaRepositorio ventas = mock(VentaRepositorio.class);
    private final BoletoRepositorio boletos = mock(BoletoRepositorio.class);
    private final PublicadorEventosSales eventos = mock(PublicadorEventosSales.class);
    private final AvisoReasignacionServicio servicio = new AvisoReasignacionServicio(ventas, boletos, eventos);

    private static ViajeReasignado reasignado() {
        return new ViajeReasignado(VIAJE, "REGULAR", "Quetzaltenango", "Guatemala", SALIDA,
                true, "P-111AAA", "P-222BBB", true, "Juan Perez", "Ana Lopez");
    }

    private static VistaViajeEntidad viaje() {
        return DatosPrueba.vistaViaje(VIAJE, XELA, 40, EstadoViaje.PROGRAMADO);
    }

    /** Venta de mostrador confirmada; el correo se pone después porque la fábrica la crea sin NIT. */
    private static VentaEntidad venta(String correo) {
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje(), 2);
        ReflectionTestUtils.setField(venta, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(venta, "clienteNombre", "Ana López");
        ReflectionTestUtils.setField(venta, "clienteCorreo", correo);
        return venta;
    }

    @Test
    void avisaSoloALaVentaQueTieneCorreo() {
        VentaEntidad conCorreo = venta("ana@correo.com");
        VentaEntidad sinCorreo = venta(null);
        List<BoletoEntidad> suyos = DatosPrueba.boletos(conCorreo, 5, 6);
        when(ventas.findByViajeIdAndEstadoIn(VIAJE, List.of(EstadoVenta.CONFIRMADA)))
                .thenReturn(List.of(conCorreo, sinCorreo));
        when(boletos.findByVentaIdIn(List.of(conCorreo.getId()))).thenReturn(suyos);

        servicio.avisarALosPasajeros(reasignado());

        ArgumentCaptor<VentaEntidad> avisada = ArgumentCaptor.forClass(VentaEntidad.class);
        verify(eventos).boletoReasignado(avisada.capture(), anyList(), any(ViajeReasignado.class));
        assertThat(avisada.getValue().getId()).isEqualTo(conCorreo.getId());
    }

    @Test
    void sinVentasConfirmadasNoSeAvisaANadie() {
        when(ventas.findByViajeIdAndEstadoIn(VIAJE, List.of(EstadoVenta.CONFIRMADA))).thenReturn(List.of());

        servicio.avisarALosPasajeros(reasignado());

        verifyNoInteractions(boletos);
        verify(eventos, never()).boletoReasignado(any(), anyList(), any());
    }

    @Test
    void siNingunaVentaTieneCorreoNiSeConsultanLosBoletos() {
        when(ventas.findByViajeIdAndEstadoIn(VIAJE, List.of(EstadoVenta.CONFIRMADA)))
                .thenReturn(List.of(venta(null), venta("   ")));

        servicio.avisarALosPasajeros(reasignado());

        verifyNoInteractions(boletos);
        verify(eventos, never()).boletoReasignado(any(), anyList(), any());
    }
}
