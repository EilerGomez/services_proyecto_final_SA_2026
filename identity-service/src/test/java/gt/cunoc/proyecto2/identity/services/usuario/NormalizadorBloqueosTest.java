package gt.cunoc.proyecto2.identity.services.usuario;

import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NormalizadorBloqueosTest {

    @Mock private UsuarioRepository usuarios;

    @Test
    void reactivaLosBloqueosTemporalesVencidos() {
        // Arrange
        Usuario vencido = new Usuario();
        vencido.setEstado("BLOQUEADO");
        vencido.setBloqueadoHasta(Instant.now().minusSeconds(60));
        vencido.setIntentosFallidos(5);
        when(usuarios.findByEstadoAndBloqueadoHastaBefore(eq("BLOQUEADO"), any())).thenReturn(List.of(vencido));

        // Act
        int total = new NormalizadorBloqueos(usuarios).normalizar();

        // Assert
        assertAll(
                () -> assertEquals(1, total),
                () -> assertEquals("ACTIVO", vencido.getEstado()),
                () -> assertEquals(null, vencido.getBloqueadoHasta()),
                () -> assertEquals(0, vencido.getIntentosFallidos()),
                () -> verify(usuarios).saveAll(List.of(vencido)));
    }

    @Test
    void sinVencidosNoGuardaNada() {
        // Arrange
        when(usuarios.findByEstadoAndBloqueadoHastaBefore(eq("BLOQUEADO"), any())).thenReturn(List.of());

        // Act
        int total = new NormalizadorBloqueos(usuarios).normalizar();

        // Assert
        assertAll(
                () -> assertEquals(0, total),
                () -> verify(usuarios, never()).saveAll(any()));
    }
}
