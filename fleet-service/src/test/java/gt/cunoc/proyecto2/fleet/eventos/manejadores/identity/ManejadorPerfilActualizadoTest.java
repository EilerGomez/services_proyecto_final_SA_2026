package gt.cunoc.proyecto2.fleet.eventos.manejadores.identity;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.fleet.eventos.recibidos.identity.PerfilActualizado;
import gt.cunoc.proyecto2.fleet.servicios.choferes.ChoferServicio;
import gt.cunoc.proyecto2.fleet.servicios.personal.PersonalSucursalServicio;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManejadorPerfilActualizadoTest {

    private final ChoferServicio choferes = mock(ChoferServicio.class);
    private final PersonalSucursalServicio personal = mock(PersonalSucursalServicio.class);
    private final ManejadorPerfilActualizado manejador = new ManejadorPerfilActualizado(choferes, personal);
    private final UUID usuarioId = UUID.randomUUID();

    private static EventoRecibido<PerfilActualizado> evento(PerfilActualizado perfil) {
        return new EventoRecibido<>(UUID.randomUUID(), "PerfilActualizado", 1, perfil.usuarioId(), 3L,
                Instant.now(), FECHA, UUID.randomUUID(), perfil);
    }

    @Test
    void seSuscribeAPerfilActualizado() {
        assertThat(manejador.tipoEvento()).isEqualTo("PerfilActualizado");
        assertThat(manejador.tipoPayload()).isEqualTo(PerfilActualizado.class);
    }

    @Test
    void sinChoferSoloActualizaAlPersonal() {
        manejador.manejar(evento(new PerfilActualizado(usuarioId, "Ana", "Lopez", "ana@fleet.gt", null)));

        verify(personal).actualizarDatosDePerfil(usuarioId, "Ana Lopez", "ana@fleet.gt");
        verifyNoInteractions(choferes);
    }

    @Test
    void conChoferActualizaElNombreDelChoferYDelPersonal() {
        UUID choferId = UUID.randomUUID();

        manejador.manejar(evento(new PerfilActualizado(usuarioId, "Juan", "Perez", "juan@fleet.gt", choferId)));

        verify(choferes).actualizarNombreDesdePerfil(choferId, "Juan Perez", FECHA);
        verify(personal).actualizarDatosDePerfil(usuarioId, "Juan Perez", "juan@fleet.gt");
    }
}
