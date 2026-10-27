package gt.cunoc.proyecto2.sales.eventos.manejadores.trips;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static gt.cunoc.proyecto2.sales.DatosPrueba.PRECIO_BOLETO;
import static gt.cunoc.proyecto2.sales.DatosPrueba.SALIDA;
import static gt.cunoc.proyecto2.sales.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.CotizacionAlquilerConfirmada;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeEliminado;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeEvento;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeIniciado;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeReasignado;
import gt.cunoc.proyecto2.sales.servicios.ventas.AvisoReasignacionServicio;
import gt.cunoc.proyecto2.sales.servicios.alquileres.OrdenAlquilerServicio;
import gt.cunoc.proyecto2.sales.servicios.proyecciones.ProyeccionServicio;
import gt.cunoc.proyecto2.sales.servicios.ventas.VentaServicio;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Los 6 manejadores de eventos de Trips: nombre, payload y a qué servicio delegan (solo viajes REGULAR). */
class ManejadoresTripsTest {

    private static final Long VERSION = 7L;
    private static final String REGULAR = "REGULAR";
    private static final String PRIVADO = "PRIVADO";

    private final ProyeccionServicio proyecciones = mock(ProyeccionServicio.class);
    private final VentaServicio ventas = mock(VentaServicio.class);
    private final OrdenAlquilerServicio ordenes = mock(OrdenAlquilerServicio.class);
    private final AvisoReasignacionServicio avisos = mock(AvisoReasignacionServicio.class);

    private <T> EventoRecibido<T> evento(String tipo, UUID agregadoId, Long version, T payload) {
        return new EventoRecibido<>(UUID.randomUUID(), tipo, 1, agregadoId, version, Instant.now(), FECHA_PAGO,
                UUID.randomUUID(), payload);
    }

    private <T> EventoRecibido<T> evento(String tipo, UUID agregadoId, T payload) {
        return evento(tipo, agregadoId, VERSION, payload);
    }

    private static ViajeEvento viaje(String tipo) {
        return new ViajeEvento(UUID.randomUUID(), tipo, UUID.randomUUID(), XELA, "Quetzaltenango", "Guatemala",
                UUID.randomUUID(), "P-123ABC", 40, PRECIO_BOLETO, SALIDA);
    }

    // ----- ViajeProgramado -----

    @Test
    void viajeProgramadoRegularGuardaElViaje() {
        ManejadorViajeProgramado manejador = new ManejadorViajeProgramado(proyecciones);
        ViajeEvento viaje = viaje(REGULAR);

        manejador.manejar(evento("ViajeProgramado", viaje.viajeId(), viaje));

        assertThat(manejador.tipoEvento()).isEqualTo("ViajeProgramado");
        assertThat(manejador.tipoPayload()).isEqualTo(ViajeEvento.class);
        verify(proyecciones).guardarViaje(viaje, VERSION);
        verifyNoMoreInteractions(proyecciones);
    }

    @Test
    void viajeProgramadoPrivadoSeIgnora() {
        ManejadorViajeProgramado manejador = new ManejadorViajeProgramado(proyecciones);
        ViajeEvento viaje = viaje(PRIVADO);

        manejador.manejar(evento("ViajeProgramado", viaje.viajeId(), viaje));

        verifyNoInteractions(proyecciones);
    }

    @Test
    void viajeProgramadoSinAggregateVersionPasaNulo() {
        ManejadorViajeProgramado manejador = new ManejadorViajeProgramado(proyecciones);
        ViajeEvento viaje = viaje(REGULAR);

        manejador.manejar(evento("ViajeProgramado", viaje.viajeId(), null, viaje));

        verify(proyecciones).guardarViaje(viaje, null);
    }

    // ----- ViajeActualizado -----

    @Test
    void viajeActualizadoRegularGuardaElViaje() {
        ManejadorViajeActualizado manejador = new ManejadorViajeActualizado(proyecciones);
        ViajeEvento viaje = viaje(REGULAR);

        manejador.manejar(evento("ViajeActualizado", viaje.viajeId(), viaje));

        assertThat(manejador.tipoEvento()).isEqualTo("ViajeActualizado");
        assertThat(manejador.tipoPayload()).isEqualTo(ViajeEvento.class);
        verify(proyecciones).guardarViaje(viaje, VERSION);
        verifyNoMoreInteractions(proyecciones);
    }

    @Test
    void viajeActualizadoPrivadoSeIgnora() {
        ManejadorViajeActualizado manejador = new ManejadorViajeActualizado(proyecciones);
        ViajeEvento viaje = viaje(PRIVADO);

        manejador.manejar(evento("ViajeActualizado", viaje.viajeId(), viaje));

        verifyNoInteractions(proyecciones);
    }

    // ----- ViajeIniciado -----

    @Test
    void viajeIniciadoRegularIniciaElViaje() {
        ManejadorViajeIniciado manejador = new ManejadorViajeIniciado(proyecciones);
        UUID viajeId = UUID.randomUUID();

        manejador.manejar(evento("ViajeIniciado", viajeId, new ViajeIniciado(viajeId, REGULAR)));

        assertThat(manejador.tipoEvento()).isEqualTo("ViajeIniciado");
        assertThat(manejador.tipoPayload()).isEqualTo(ViajeIniciado.class);
        verify(proyecciones).iniciarViaje(viajeId, VERSION);
        verifyNoMoreInteractions(proyecciones);
    }

    @Test
    void viajeIniciadoPrivadoSeIgnora() {
        ManejadorViajeIniciado manejador = new ManejadorViajeIniciado(proyecciones);
        UUID viajeId = UUID.randomUUID();

        manejador.manejar(evento("ViajeIniciado", viajeId, new ViajeIniciado(viajeId, PRIVADO)));

        verifyNoInteractions(proyecciones);
    }

    // ----- ViajeEliminado -----

    @Test
    void viajeEliminadoCompensaLasVentasAntesDeBorrarElViaje() {
        ManejadorViajeEliminado manejador = new ManejadorViajeEliminado(ventas, proyecciones);
        UUID viajeId = UUID.randomUUID();

        manejador.manejar(evento("ViajeEliminado", viajeId,
                new ViajeEliminado(viajeId, REGULAR, XELA, "Bus en mantenimiento")));

        assertThat(manejador.tipoEvento()).isEqualTo("ViajeEliminado");
        assertThat(manejador.tipoPayload()).isEqualTo(ViajeEliminado.class);
        InOrder orden = inOrder(ventas, proyecciones);
        orden.verify(ventas).compensarViajeEliminado(viajeId, "Bus en mantenimiento");
        orden.verify(proyecciones).eliminarViaje(viajeId, VERSION);
        verifyNoMoreInteractions(ventas, proyecciones);
    }

    @Test
    void viajeEliminadoPrivadoSeIgnora() {
        ManejadorViajeEliminado manejador = new ManejadorViajeEliminado(ventas, proyecciones);
        UUID viajeId = UUID.randomUUID();

        manejador.manejar(evento("ViajeEliminado", viajeId, new ViajeEliminado(viajeId, PRIVADO, XELA, null)));

        verifyNoInteractions(ventas, proyecciones);
    }

    // ----- ViajeReasignado -----

    private static ViajeReasignado reasignado(String tipo) {
        return new ViajeReasignado(UUID.randomUUID(), tipo, "Quetzaltenango", "Guatemala", SALIDA,
                true, "P-111AAA", "P-222BBB", false, "Juan Perez", "Juan Perez");
    }

    @Test
    void viajeReasignadoRegularAvisaALosPasajeros() {
        ManejadorViajeReasignado manejador = new ManejadorViajeReasignado(avisos);
        ViajeReasignado viaje = reasignado(REGULAR);

        manejador.manejar(evento("ViajeReasignado", viaje.viajeId(), viaje));

        assertThat(manejador.tipoEvento()).isEqualTo("ViajeReasignado");
        assertThat(manejador.tipoPayload()).isEqualTo(ViajeReasignado.class);
        verify(avisos).avisarALosPasajeros(viaje);
        verifyNoMoreInteractions(avisos);
    }

    @Test
    void viajeReasignadoPrivadoSeIgnora() {
        // Un privado no tiene boletos: a su cliente le escribe Notification con el correo del evento.
        ManejadorViajeReasignado manejador = new ManejadorViajeReasignado(avisos);
        ViajeReasignado viaje = reasignado(PRIVADO);

        manejador.manejar(evento("ViajeReasignado", viaje.viajeId(), viaje));

        verifyNoInteractions(avisos);
    }

    // ----- CotizacionAlquilerConfirmada -----

    @Test
    void cotizacionConfirmadaRegistraLaOrden() {
        ManejadorCotizacionAlquilerConfirmada manejador = new ManejadorCotizacionAlquilerConfirmada(ordenes);
        CotizacionAlquilerConfirmada cotizacion = new CotizacionAlquilerConfirmada(UUID.randomUUID(), XELA,
                UUID.randomUUID(), "1234567-8", "Colegio San José", "colegio@correo.com", "Quetzaltenango",
                "Antigua Guatemala", null, null, null, null, null, null, new BigDecimal("2600.00"));

        manejador.manejar(evento("CotizacionAlquilerConfirmada", cotizacion.alquilerId(), cotizacion));

        assertThat(manejador.tipoEvento()).isEqualTo("CotizacionAlquilerConfirmada");
        assertThat(manejador.tipoPayload()).isEqualTo(CotizacionAlquilerConfirmada.class);
        verify(ordenes).registrarOrden(cotizacion);
        verifyNoMoreInteractions(ordenes);
        verifyNoInteractions(proyecciones, ventas);
    }
}
