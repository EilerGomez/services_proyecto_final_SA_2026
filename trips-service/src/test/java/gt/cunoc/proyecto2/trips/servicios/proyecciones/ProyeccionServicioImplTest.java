package gt.cunoc.proyecto2.trips.servicios.proyecciones;

import static gt.cunoc.proyecto2.trips.DatosPrueba.FECHA;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.ChoferEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.SucursalEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.identity.TarifaDepreciacionCambiada;
import gt.cunoc.proyecto2.trips.repositorios.buses.VistaBusRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.choferes.VistaChoferRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.sucursales.VistaSucursalRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.tarifas.VistaTarifaDepreciacionRepositorio;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProyeccionServicioImplTest {

    private static final BigDecimal LATITUD = new BigDecimal("14.834999");
    private static final BigDecimal LONGITUD = new BigDecimal("-91.518639");
    private static final BigDecimal SALARIO = new BigDecimal("350.00");
    private static final BigDecimal MONTO_POR_KM = new BigDecimal("1.2500");

    private final VistaSucursalRepositorio sucursales = mock(VistaSucursalRepositorio.class);
    private final VistaBusRepositorio buses = mock(VistaBusRepositorio.class);
    private final VistaChoferRepositorio choferes = mock(VistaChoferRepositorio.class);
    private final VistaTarifaDepreciacionRepositorio tarifas = mock(VistaTarifaDepreciacionRepositorio.class);

    private ProyeccionServicioImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new ProyeccionServicioImpl(sucursales, buses, choferes, tarifas);
    }

    private static SucursalEvento sucursal() {
        return new SucursalEvento(XELA, "Xela Central", LATITUD, LONGITUD, true);
    }

    private static BusEvento bus(UUID busId) {
        return new BusEvento(busId, XELA, "P-123ABC", 45, "ACTIVO");
    }

    private static ChoferEvento chofer(UUID choferId) {
        return new ChoferEvento(choferId, XELA, "Juan Pérez", SALARIO, "ACTIVO");
    }

    //  Sucursales 

    @Test
    void guardarSucursalPasaTodosLosDatosYLaVersion() {
        when(sucursales.guardar(XELA, "Xela Central", LATITUD, LONGITUD, true, 4L)).thenReturn(1);

        servicio.guardarSucursal(sucursal(), 4L);

        verify(sucursales).guardar(XELA, "Xela Central", LATITUD, LONGITUD, true, 4L);
    }

    @Test
    void guardarSucursalSinVersionUsaCero() {
        servicio.guardarSucursal(sucursal(), null);

        verify(sucursales).guardar(XELA, "Xela Central", LATITUD, LONGITUD, true, 0L);
    }

    @Test
    void guardarSucursalConEventoViejoNoFalla() {
        // el mock devuelve 0 filas: la fila ya tiene una versión más nueva
        assertThatCode(() -> servicio.guardarSucursal(sucursal(), 1L)).doesNotThrowAnyException();
        verify(sucursales).guardar(XELA, "Xela Central", LATITUD, LONGITUD, true, 1L);
    }

    // uses

    @Test
    void guardarBusPasaTodosLosDatosYLaVersion() {
        UUID busId = UUID.randomUUID();
        when(buses.guardar(busId, XELA, "P-123ABC", 45, "ACTIVO", 2L)).thenReturn(1);

        servicio.guardarBus(bus(busId), 2L);

        verify(buses).guardar(busId, XELA, "P-123ABC", 45, "ACTIVO", 2L);
    }

    @Test
    void guardarBusSinVersionNiFilasNoFalla() {
        UUID busId = UUID.randomUUID();

        assertThatCode(() -> servicio.guardarBus(bus(busId), null)).doesNotThrowAnyException();
        verify(buses).guardar(busId, XELA, "P-123ABC", 45, "ACTIVO", 0L);
    }

    @Test
    void desactivarBusPasaElIdYLaVersion() {
        UUID busId = UUID.randomUUID();
        when(buses.desactivar(busId, 5L)).thenReturn(1);

        servicio.desactivarBus(busId, 5L);

        verify(buses).desactivar(busId, 5L);
    }

    @Test
    void desactivarBusSinVersionNiFilasNoFalla() {
        UUID busId = UUID.randomUUID();

        assertThatCode(() -> servicio.desactivarBus(busId, null)).doesNotThrowAnyException();
        verify(buses).desactivar(busId, 0L);
    }

    // hoferes 

    @Test
    void guardarChoferPasaTodosLosDatosYLaVersion() {
        UUID choferId = UUID.randomUUID();
        when(choferes.guardar(choferId, XELA, "Juan Pérez", SALARIO, "ACTIVO", 3L)).thenReturn(1);

        servicio.guardarChofer(chofer(choferId), 3L);

        verify(choferes).guardar(choferId, XELA, "Juan Pérez", SALARIO, "ACTIVO", 3L);
    }

    @Test
    void guardarChoferSinVersionNiFilasNoFalla() {
        UUID choferId = UUID.randomUUID();

        assertThatCode(() -> servicio.guardarChofer(chofer(choferId), null)).doesNotThrowAnyException();
        verify(choferes).guardar(choferId, XELA, "Juan Pérez", SALARIO, "ACTIVO", 0L);
    }

    @Test
    void desactivarChoferPasaElIdYLaVersion() {
        UUID choferId = UUID.randomUUID();
        when(choferes.desactivar(choferId, 6L)).thenReturn(1);

        servicio.desactivarChofer(choferId, 6L);

        verify(choferes).desactivar(choferId, 6L);
    }

    @Test
    void desactivarChoferSinVersionNiFilasNoFalla() {
        UUID choferId = UUID.randomUUID();

        assertThatCode(() -> servicio.desactivarChofer(choferId, null)).doesNotThrowAnyException();
        verify(choferes).desactivar(choferId, 0L);
    }

    // arifas

    @Test
    void guardarTarifaPasaElIdMontoYFecha() {
        UUID tarifaId = UUID.randomUUID();
        when(tarifas.guardar(tarifaId, MONTO_POR_KM, FECHA)).thenReturn(1);

        servicio.guardarTarifa(new TarifaDepreciacionCambiada(tarifaId, MONTO_POR_KM, FECHA));

        verify(tarifas).guardar(tarifaId, MONTO_POR_KM, FECHA);
    }

    @Test
    void guardarTarifaRepetidaNoFalla() {
        UUID tarifaId = UUID.randomUUID();

        assertThatCode(() -> servicio.guardarTarifa(new TarifaDepreciacionCambiada(tarifaId, MONTO_POR_KM, FECHA)))
                .doesNotThrowAnyException();
        verify(tarifas).guardar(tarifaId, MONTO_POR_KM, FECHA);
    }
}
