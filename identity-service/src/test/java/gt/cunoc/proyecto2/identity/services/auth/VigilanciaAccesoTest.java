package gt.cunoc.proyecto2.identity.services.auth;

import gt.cunoc.proyecto2.identity.models.CodigoDobleFactor;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CodigoDobleFactorRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VigilanciaAccesoTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID DESAFIO_ID = UUID.randomUUID();

    @Mock private UsuarioRepository usuarios;
    @Mock private CodigoDobleFactorRepository desafios;

    @InjectMocks
    private VigilanciaAcceso vigilancia;

    @Test
    void falloContrasenaSumaSinBloquear() {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setIntentosFallidos(2);
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        // Act
        vigilancia.falloContrasena(USUARIO_ID);

        // Assert
        assertAll(
                () -> assertEquals(3, usuario.getIntentosFallidos()),
                () -> assertEquals("ACTIVO", usuario.getEstado()),
                () -> verify(usuarios).save(usuario));
    }

    @Test
    void quintoFalloBloqueaConFecha() {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setIntentosFallidos(4);
        usuario.setEstado("ACTIVO");
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        // Act
        vigilancia.falloContrasena(USUARIO_ID);

        // Assert
        assertAll(
                () -> assertEquals("BLOQUEADO", usuario.getEstado()),
                () -> assertNotNull(usuario.getBloqueadoHasta()),
                () -> assertEquals(0, usuario.getIntentosFallidos()));
    }

    @Test
    void falloContrasenaSinUsuarioNoHaceNada() {
        // Arrange
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.empty());

        // Act
        vigilancia.falloContrasena(USUARIO_ID);

        // Assert
        verify(usuarios, never()).save(any());
    }

    @Test
    void falloCodigoAlQuintoBloqueaYDevuelveTrue() {
        // Arrange
        CodigoDobleFactor desafio = new CodigoDobleFactor();
        desafio.setIntentos(4);
        Usuario usuario = new Usuario();
        usuario.setEstado("ACTIVO");
        when(desafios.findById(DESAFIO_ID)).thenReturn(Optional.of(desafio));
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        // Act
        boolean bloqueada = vigilancia.falloCodigo(DESAFIO_ID, USUARIO_ID);

        // Assert
        assertAll(
                () -> assertTrue(bloqueada),
                () -> assertEquals("BLOQUEADO", usuario.getEstado()));
    }

    @Test
    void falloCodigoNormalNoBloquea() {
        // Arrange
        CodigoDobleFactor desafio = new CodigoDobleFactor();
        desafio.setIntentos(1);
        when(desafios.findById(DESAFIO_ID)).thenReturn(Optional.of(desafio));

        // Act
        boolean bloqueada = vigilancia.falloCodigo(DESAFIO_ID, USUARIO_ID);

        // Assert
        assertAll(
                () -> assertFalse(bloqueada),
                () -> assertEquals(2, desafio.getIntentos()),
                () -> verify(usuarios, never()).save(any()));
    }
}
