package gt.cunoc.proyecto2.identity.services.auth;

import gt.cunoc.proyecto2.identity.models.CodigoDobleFactor;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CodigoDobleFactorRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ramas de VigilanciaAcceso sin cubrir: desafío inexistente y usuario inexistente al bloquear. */
@ExtendWith(MockitoExtension.class)
class VigilanciaAccesoRamasTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID DESAFIO_ID = UUID.randomUUID();

    @Mock private UsuarioRepository usuarios;
    @Mock private CodigoDobleFactorRepository desafios;

    @InjectMocks
    private VigilanciaAcceso vigilancia;

    @Test
    void falloCodigoSinDesafioDevuelveFalse() {
        // Arrange
        when(desafios.findById(DESAFIO_ID)).thenReturn(Optional.empty());

        // Act
        boolean bloqueada = vigilancia.falloCodigo(DESAFIO_ID, USUARIO_ID);

        // Assert
        assertAll(
                () -> assertFalse(bloqueada),
                () -> verify(desafios, never()).save(any()),
                () -> verify(usuarios, never()).save(any()));
    }

    @Test
    void falloCodigoAlQuintoSinUsuarioDevuelveTrue() {
        // Arrange
        CodigoDobleFactor desafio = new CodigoDobleFactor();
        desafio.setIntentos(4);
        when(desafios.findById(DESAFIO_ID)).thenReturn(Optional.of(desafio));
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.empty());

        // Act
        boolean bloqueada = vigilancia.falloCodigo(DESAFIO_ID, USUARIO_ID);

        // Assert
        assertAll(
                () -> assertTrue(bloqueada),
                () -> assertEquals(5, desafio.getIntentos()),
                () -> verify(desafios).save(desafio),
                () -> verify(usuarios, never()).save(any()));
    }
}
