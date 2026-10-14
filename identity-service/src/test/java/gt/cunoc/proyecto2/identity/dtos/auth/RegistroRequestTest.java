package gt.cunoc.proyecto2.identity.dtos.auth;

import gt.cunoc.proyecto2.identity.models.Usuario;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** RegistroRequest.createEntity: instrucción sin cubrir. */
class RegistroRequestTest {

    @Test
    void createEntityMapeaCamposAlPerfil() {
        var solicitud = new RegistroRequest("ana@correo.com", "Segura#2026", "Ana", "López",
                "1234567-8", "1234567890101", "55551234", "Xela", "https://fotos/foto.jpg");
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());

        var perfil = solicitud.createEntity(usuario);

        assertThat(perfil.getUsuario()).isSameAs(usuario);
        assertThat(perfil.getNombres()).isEqualTo("Ana");
        assertThat(perfil.getApellidos()).isEqualTo("López");
        assertThat(perfil.getNit()).isEqualTo("1234567-8");
        assertThat(perfil.getDpi()).isEqualTo("1234567890101");
        assertThat(perfil.getTelefono()).isEqualTo("55551234");
        assertThat(perfil.getDireccion()).isEqualTo("Xela");
        assertThat(perfil.getFotoUrl()).isEqualTo("https://fotos/foto.jpg");
    }
}
