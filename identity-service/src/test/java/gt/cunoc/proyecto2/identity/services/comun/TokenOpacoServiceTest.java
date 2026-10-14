package gt.cunoc.proyecto2.identity.services.comun;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

/** TokenOpacoService: generación, vector SHA-256 conocido y rama de algoritmo ausente. */
class TokenOpacoServiceTest {

    private final TokenOpacoService servicio = new TokenOpacoService();

    @Test
    void generarProduceDistintosNoVacios() {
        String primero = servicio.generar();
        String segundo = servicio.generar();

        assertThat(primero).isNotBlank();
        assertThat(segundo).isNotBlank();
        assertThat(primero).isNotEqualTo(segundo);
    }

    @Test
    void calcularHashCoincideConSha256() {
        assertThat(servicio.calcularHash("abc")).isEqualTo(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void sinAlgoritmoDaEstadoIlegal() {
        try (var mensajes = mockStatic(MessageDigest.class)) {
            mensajes.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("no existe"));

            var error = assertThrows(IllegalStateException.class,
                    () -> servicio.calcularHash("cualquiera"));

            assertThat(error.getMessage()).isEqualTo("SHA-256 no disponible");
        }
    }
}
