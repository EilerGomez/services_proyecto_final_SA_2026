package gt.cunoc.proyecto2.trips.eventos.manejadores.identity;

import static gt.cunoc.proyecto2.trips.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.recibidos.identity.ClienteRegistrado;
import gt.cunoc.proyecto2.trips.eventos.recibidos.identity.PerfilActualizado;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Copia de los clientes de Identity. Es lo que permite que un alquiler pedido en ventanilla con el
 * NIT de un cliente quede ligado a su cuenta.
 */
class ManejadoresClienteTest {

    private final ProyeccionServicio proyecciones = mock(ProyeccionServicio.class);
    private final ManejadorClienteRegistrado registrado = new ManejadorClienteRegistrado(proyecciones);
    private final ManejadorPerfilActualizado actualizado = new ManejadorPerfilActualizado(proyecciones);

    private static <T> EventoRecibido<T> evento(String tipo, T payload) {
        return new EventoRecibido<>(UUID.randomUUID(), tipo, 1, UUID.randomUUID(), 0L, Instant.now(), FECHA,
                UUID.randomUUID(), payload);
    }

    @Test
    void nombresYPayloads() {
        assertThat(registrado.tipoEvento()).isEqualTo("ClienteRegistrado");
        assertThat(registrado.tipoPayload()).isEqualTo(ClienteRegistrado.class);
        assertThat(actualizado.tipoEvento()).isEqualTo("PerfilActualizado");
        assertThat(actualizado.tipoPayload()).isEqualTo(PerfilActualizado.class);
    }

    @Test
    void clienteRegistradoGuardaLaCopia() {
        ClienteRegistrado cliente = new ClienteRegistrado(UUID.randomUUID(), "1234567-K", "Ana López",
                "ana@correo.com");

        registrado.manejar(evento("ClienteRegistrado", cliente));

        verify(proyecciones).guardarCliente(cliente.usuarioId(), "1234567-K", "Ana López", "ana@correo.com");
        verifyNoMoreInteractions(proyecciones);
    }

    @Test
    void perfilActualizadoJuntaNombresYApellidos() {
        PerfilActualizado perfil = new PerfilActualizado(UUID.randomUUID(), "7654321-0", "Ana María",
                "López Coyoy", "ana@correo.com");

        actualizado.manejar(evento("PerfilActualizado", perfil));

        verify(proyecciones).guardarCliente(perfil.usuarioId(), "7654321-0", "Ana María López Coyoy",
                "ana@correo.com");
    }

    @Test
    void unPerfilSinApellidosNoDejaEspaciosDeMas() {
        PerfilActualizado perfil = new PerfilActualizado(UUID.randomUUID(), "7654321-0", "Ana", null,
                "ana@correo.com");

        actualizado.manejar(evento("PerfilActualizado", perfil));

        verify(proyecciones).guardarCliente(perfil.usuarioId(), "7654321-0", "Ana", "ana@correo.com");
    }
}
