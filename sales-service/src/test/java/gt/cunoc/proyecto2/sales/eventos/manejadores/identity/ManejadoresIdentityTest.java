package gt.cunoc.proyecto2.sales.eventos.manejadores.identity;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.ReferenciaCartera;
import gt.cunoc.proyecto2.sales.eventos.recibidos.identity.ClienteRegistrado;
import gt.cunoc.proyecto2.sales.eventos.recibidos.identity.DebitoCarteraRechazado;
import gt.cunoc.proyecto2.sales.eventos.recibidos.identity.MovimientoCartera;
import gt.cunoc.proyecto2.sales.eventos.recibidos.identity.PerfilActualizado;
import gt.cunoc.proyecto2.sales.servicios.pagos.PagoCarteraServicio;
import gt.cunoc.proyecto2.sales.servicios.proyecciones.ProyeccionServicio;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Los 5 manejadores de eventos de Identity: nombre, payload y a qué servicio delegan. */
class ManejadoresIdentityTest {

    private final PagoCarteraServicio pagos = mock(PagoCarteraServicio.class);
    private final ProyeccionServicio proyecciones = mock(ProyeccionServicio.class);

    private final UUID usuarioId = UUID.randomUUID();

    /** Identity no manda aggregateVersion. */
    private <T> EventoRecibido<T> evento(String tipo, UUID agregadoId, T payload) {
        return new EventoRecibido<>(UUID.randomUUID(), tipo, 1, agregadoId, null, Instant.now(), FECHA_PAGO,
                UUID.randomUUID(), payload);
    }

    private MovimientoCartera movimiento(String referenciaTipo, UUID referenciaId) {
        return new MovimientoCartera(UUID.randomUUID(), referenciaTipo, referenciaId, usuarioId,
                new BigDecimal("150.00"), new BigDecimal("350.00"), FECHA_PAGO);
    }

    private DebitoCarteraRechazado rechazo(String referenciaTipo, UUID referenciaId, String motivo) {
        return new DebitoCarteraRechazado(referenciaTipo, referenciaId, usuarioId, new BigDecimal("150.00"),
                motivo);
    }

    // ----- CarteraDebitada -----

    @Test
    void carteraDebitadaDeVentaDelegaALaSaga() {
        ManejadorCarteraDebitada manejador = new ManejadorCarteraDebitada(pagos);
        UUID ventaId = UUID.randomUUID();

        manejador.manejar(evento("CarteraDebitada", usuarioId, movimiento("VENTA", ventaId)));

        assertThat(manejador.tipoEvento()).isEqualTo("CarteraDebitada");
        assertThat(manejador.tipoPayload()).isEqualTo(MovimientoCartera.class);
        verify(pagos).carteraDebitada(ReferenciaCartera.VENTA, ventaId);
        verifyNoMoreInteractions(pagos);
        verifyNoInteractions(proyecciones);
    }

    @Test
    void carteraDebitadaDeAlquilerDelegaALaSaga() {
        ManejadorCarteraDebitada manejador = new ManejadorCarteraDebitada(pagos);
        UUID ordenId = UUID.randomUUID();

        manejador.manejar(evento("CarteraDebitada", usuarioId, movimiento("ALQUILER", ordenId)));

        verify(pagos).carteraDebitada(ReferenciaCartera.ALQUILER, ordenId);
        verifyNoMoreInteractions(pagos);
    }

    @Test
    void carteraDebitadaConReferenciaDesconocidaFalla() {
        ManejadorCarteraDebitada manejador = new ManejadorCarteraDebitada(pagos);
        EventoRecibido<MovimientoCartera> evento = evento("CarteraDebitada", usuarioId,
                movimiento("ENCOMIENDA", UUID.randomUUID()));

        assertThatThrownBy(() -> manejador.manejar(evento)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(pagos);
    }

    // ----- CarteraReembolsada -----

    @Test
    void carteraReembolsadaDeVentaDelegaALaSaga() {
        ManejadorCarteraReembolsada manejador = new ManejadorCarteraReembolsada(pagos);
        UUID ventaId = UUID.randomUUID();

        manejador.manejar(evento("CarteraReembolsada", usuarioId, movimiento("VENTA", ventaId)));

        assertThat(manejador.tipoEvento()).isEqualTo("CarteraReembolsada");
        assertThat(manejador.tipoPayload()).isEqualTo(MovimientoCartera.class);
        verify(pagos).carteraReembolsada(ReferenciaCartera.VENTA, ventaId);
        verifyNoMoreInteractions(pagos);
    }

    @Test
    void carteraReembolsadaDeAlquilerDelegaALaSaga() {
        ManejadorCarteraReembolsada manejador = new ManejadorCarteraReembolsada(pagos);
        UUID ordenId = UUID.randomUUID();

        manejador.manejar(evento("CarteraReembolsada", usuarioId, movimiento("ALQUILER", ordenId)));

        verify(pagos).carteraReembolsada(ReferenciaCartera.ALQUILER, ordenId);
        verifyNoMoreInteractions(pagos);
    }

    @Test
    void carteraReembolsadaConReferenciaDesconocidaFalla() {
        ManejadorCarteraReembolsada manejador = new ManejadorCarteraReembolsada(pagos);
        EventoRecibido<MovimientoCartera> evento = evento("CarteraReembolsada", usuarioId,
                movimiento("venta", UUID.randomUUID()));

        assertThatThrownBy(() -> manejador.manejar(evento)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(pagos);
    }

    // ----- DebitoCarteraRechazado -----

    @Test
    void debitoRechazadoDeVentaPasaElMotivo() {
        ManejadorDebitoCarteraRechazado manejador = new ManejadorDebitoCarteraRechazado(pagos);
        UUID ventaId = UUID.randomUUID();

        manejador.manejar(evento("DebitoCarteraRechazado", usuarioId,
                rechazo("VENTA", ventaId, "SALDO_INSUFICIENTE")));

        assertThat(manejador.tipoEvento()).isEqualTo("DebitoCarteraRechazado");
        assertThat(manejador.tipoPayload()).isEqualTo(DebitoCarteraRechazado.class);
        verify(pagos).debitoRechazado(ReferenciaCartera.VENTA, ventaId, "SALDO_INSUFICIENTE");
        verifyNoMoreInteractions(pagos);
    }

    @Test
    void debitoRechazadoDeAlquilerPasaElMotivo() {
        ManejadorDebitoCarteraRechazado manejador = new ManejadorDebitoCarteraRechazado(pagos);
        UUID ordenId = UUID.randomUUID();

        manejador.manejar(evento("DebitoCarteraRechazado", usuarioId,
                rechazo("ALQUILER", ordenId, "USUARIO_INACTIVO")));

        verify(pagos).debitoRechazado(ReferenciaCartera.ALQUILER, ordenId, "USUARIO_INACTIVO");
        verifyNoMoreInteractions(pagos);
    }

    @Test
    void debitoRechazadoConReferenciaDesconocidaFalla() {
        ManejadorDebitoCarteraRechazado manejador = new ManejadorDebitoCarteraRechazado(pagos);
        EventoRecibido<DebitoCarteraRechazado> evento = evento("DebitoCarteraRechazado", usuarioId,
                rechazo("OTRO", UUID.randomUUID(), "SALDO_INSUFICIENTE"));

        assertThatThrownBy(() -> manejador.manejar(evento)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(pagos);
    }

    // ----- ClienteRegistrado -----

    @Test
    void clienteRegistradoGuardaElCliente() {
        ManejadorClienteRegistrado manejador = new ManejadorClienteRegistrado(proyecciones);

        manejador.manejar(evento("ClienteRegistrado", usuarioId,
                new ClienteRegistrado(usuarioId, "1234567-8", "Ana López", "ana@correo.com")));

        assertThat(manejador.tipoEvento()).isEqualTo("ClienteRegistrado");
        assertThat(manejador.tipoPayload()).isEqualTo(ClienteRegistrado.class);
        verify(proyecciones).guardarCliente(usuarioId, "1234567-8", "Ana López", "ana@correo.com");
        verifyNoMoreInteractions(proyecciones);
        verifyNoInteractions(pagos);
    }

    // ----- PerfilActualizado -----

    @Test
    void perfilActualizadoGuardaElNombreCompleto() {
        ManejadorPerfilActualizado manejador = new ManejadorPerfilActualizado(proyecciones);

        manejador.manejar(evento("PerfilActualizado", usuarioId,
                new PerfilActualizado(usuarioId, "7654321-0", " Ana María ", "López", "ana.maria@correo.com")));

        assertThat(manejador.tipoEvento()).isEqualTo("PerfilActualizado");
        assertThat(manejador.tipoPayload()).isEqualTo(PerfilActualizado.class);
        verify(proyecciones).guardarCliente(usuarioId, "7654321-0", "Ana María López", "ana.maria@correo.com");
        verifyNoMoreInteractions(proyecciones);
        verifyNoInteractions(pagos);
    }

    @Test
    void perfilActualizadoSinApellidosUsaSoloLosNombres() {
        ManejadorPerfilActualizado manejador = new ManejadorPerfilActualizado(proyecciones);

        manejador.manejar(evento("PerfilActualizado", usuarioId,
                new PerfilActualizado(usuarioId, null, "Ana", null, "ana@correo.com")));

        verify(proyecciones).guardarCliente(usuarioId, null, "Ana", "ana@correo.com");
    }
}
