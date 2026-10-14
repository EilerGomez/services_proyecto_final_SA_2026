package gt.cunoc.proyecto2.identity.services.comun;

import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.Usuario;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Ramas de IdentidadUsuarioFactory: cada rol con sucursal la conserva, el resto da nulo. */
class IdentidadUsuarioFactoryTest {

    private final IdentidadUsuarioFactory fabrica = new IdentidadUsuarioFactory();

    @Test
    void adminSucursalConservaSucursal() {
        UUID sucursalId = UUID.randomUUID();

        var actual = fabrica.construir(usuarioCon("ADMIN_SUCURSAL", sucursalId));

        assertThat(actual.sucursalId()).isEqualTo(sucursalId);
    }

    @Test
    void cajeroConservaSucursal() {
        UUID sucursalId = UUID.randomUUID();

        var actual = fabrica.construir(usuarioCon("CAJERO", sucursalId));

        assertThat(actual.sucursalId()).isEqualTo(sucursalId);
    }

    @Test
    void choferConservaSucursal() {
        UUID sucursalId = UUID.randomUUID();

        var actual = fabrica.construir(usuarioCon("CHOFER", sucursalId));

        assertThat(actual.sucursalId()).isEqualTo(sucursalId);
    }

    @Test
    void clienteDaSucursalNula() {
        var actual = fabrica.construir(usuarioCon("CLIENTE", UUID.randomUUID()));

        assertThat(actual.sucursalId()).isNull();
    }

    @Test
    void adminSistemaDaSucursalNula() {
        var actual = fabrica.construir(usuarioCon("ADMIN_SISTEMA", UUID.randomUUID()));

        assertThat(actual.sucursalId()).isNull();
    }

    @Test
    void conservaCorreoRolesYChofer() {
        UUID choferId = UUID.randomUUID();
        Usuario usuario = usuarioCon("CLIENTE", null);
        usuario.setChoferId(choferId);

        var actual = fabrica.construir(usuario);

        assertThat(actual.correo()).isEqualTo("a@correo.com");
        assertThat(actual.roles()).containsExactly("CLIENTE");
        assertThat(actual.choferId()).isEqualTo(choferId);
    }

    private Usuario usuarioCon(String codigoRol, UUID sucursalId) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setCorreo("a@correo.com");
        Rol rol = new Rol();
        rol.setCodigo(codigoRol);
        usuario.getRoles().add(rol);
        usuario.setSucursalId(sucursalId);
        return usuario;
    }
}
