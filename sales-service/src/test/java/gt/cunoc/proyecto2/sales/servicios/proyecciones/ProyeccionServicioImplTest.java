package gt.cunoc.proyecto2.sales.servicios.proyecciones;

import static gt.cunoc.proyecto2.sales.DatosPrueba.PRECIO_BOLETO;
import static gt.cunoc.proyecto2.sales.DatosPrueba.SALIDA;
import static gt.cunoc.proyecto2.sales.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeEvento;
import gt.cunoc.proyecto2.sales.repositorios.asientos.AsientoViajeRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.viajes.VistaViajeRepositorio;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProyeccionServicioImplTest {

    private final VistaViajeRepositorio viajes = mock(VistaViajeRepositorio.class);
    private final AsientoViajeRepositorio asientos = mock(AsientoViajeRepositorio.class);
    private final VistaClienteRepositorio clientes = mock(VistaClienteRepositorio.class);

    private final UUID viajeId = UUID.randomUUID();
    private final UUID rutaId = UUID.randomUUID();
    private final UUID busId = UUID.randomUUID();

    private ProyeccionServicioImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new ProyeccionServicioImpl(viajes, asientos, clientes);
    }

    private ViajeEvento viaje(int capacidad) {
        return new ViajeEvento(viajeId, "REGULAR", rutaId, XELA, "Quetzaltenango", "Guatemala", busId, "P-123ABC",
                capacidad, PRECIO_BOLETO, SALIDA);
    }

    private void guardarDevuelve(int capacidad, long version, int filas) {
        when(viajes.guardar(viajeId, rutaId, XELA, "Quetzaltenango", "Guatemala", SALIDA, busId, "P-123ABC",
                capacidad, PRECIO_BOLETO, version)).thenReturn(filas);
    }

    // ----- Viajes -----

    @Test
    void guardarViajeAplicadoCreaLosAsientosConLaCapacidad() {
        guardarDevuelve(40, 3L, 1);
        when(asientos.crearAsientos(viajeId, 40)).thenReturn(40);

        servicio.guardarViaje(viaje(40), 3L);

        verify(viajes).guardar(viajeId, rutaId, XELA, "Quetzaltenango", "Guatemala", SALIDA, busId, "P-123ABC",
                40, PRECIO_BOLETO, 3L);
        verify(asientos).crearAsientos(viajeId, 40);
        verifyNoInteractions(clientes);
    }

    @Test
    void guardarViajeSinAsientosNuevosNoFalla() {
        guardarDevuelve(45, 4L, 1);
        when(asientos.crearAsientos(viajeId, 45)).thenReturn(0);

        assertThatCode(() -> servicio.guardarViaje(viaje(45), 4L)).doesNotThrowAnyException();

        verify(asientos).crearAsientos(viajeId, 45);
    }

    @Test
    void guardarViajeViejoNoCreaAsientos() {
        assertThatCode(() -> servicio.guardarViaje(viaje(40), 1L)).doesNotThrowAnyException();

        verify(viajes).guardar(viajeId, rutaId, XELA, "Quetzaltenango", "Guatemala", SALIDA, busId, "P-123ABC",
                40, PRECIO_BOLETO, 1L);
        verifyNoInteractions(asientos);
    }

    @Test
    void guardarViajeSinVersionUsaCero() {
        guardarDevuelve(40, 0L, 1);

        servicio.guardarViaje(viaje(40), null);

        verify(viajes).guardar(viajeId, rutaId, XELA, "Quetzaltenango", "Guatemala", SALIDA, busId, "P-123ABC",
                40, PRECIO_BOLETO, 0L);
        verify(asientos).crearAsientos(viajeId, 40);
    }

    @Test
    void iniciarViajePasaElIdYLaVersion() {
        when(viajes.iniciar(viajeId, 5L)).thenReturn(1);

        servicio.iniciarViaje(viajeId, 5L);

        verify(viajes).iniciar(viajeId, 5L);
        verifyNoInteractions(asientos, clientes);
    }


    @Test
    void eliminarViajePasaElIdYLaVersion() {
        when(viajes.eliminar(viajeId, 6L)).thenReturn(1);

        servicio.eliminarViaje(viajeId, 6L);

        verify(viajes).eliminar(viajeId, 6L);
        verifyNoInteractions(asientos, clientes);
    }

    // ----- Clientes -----

    @Test
    void guardarClientePasaSusDatos() {
        UUID usuarioId = UUID.randomUUID();

        servicio.guardarCliente(usuarioId, "1234567-8", "Ana López", "ana@correo.com");

        verify(clientes).guardar(usuarioId, "1234567-8", "Ana López", "ana@correo.com");
        verifyNoInteractions(viajes, asientos);
    }

    @Test
    void guardarClienteSinNitEsConsumidorFinal() {
        UUID usuarioId = UUID.randomUUID();

        servicio.guardarCliente(usuarioId, null, "Ana López", "ana@correo.com");

        verify(clientes).guardar(usuarioId, "CF", "Ana López", "ana@correo.com");
    }

}
