package gt.cunoc.proyecto2.identity.config;

import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialAdminSeederTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private PerfilRepository perfiles;
    @Mock private RolRepository roles;

    @Spy private PasswordEncoder claves = new BCryptPasswordEncoder();

    private InitialAdminSeeder seeder() {
        // Constructor manual: las variables ADMIN_* son String y Mockito no las inyecta.
        return new InitialAdminSeeder(usuarios, perfiles, roles, claves,
                "admin@correo.com", "Admin12345", "Administrador", "Sistema",
                "CF", "0000000000000", "00000000", "Ciudad");
    }

    @Test
    void conAdminExistenteNoHaceNada() {
        // Arrange
        when(usuarios.countByEstadoAndRoles_Codigo("ACTIVO", "ADMIN_SISTEMA")).thenReturn(2L);

        // Act
        seeder().run(null);

        // Assert
        verify(usuarios, never()).save(any());
        verify(perfiles, never()).save(any());
    }

    @Test
    void sinCorreoConfiguradoSoloAvisa() {
        // Arrange
        when(usuarios.countByEstadoAndRoles_Codigo("ACTIVO", "ADMIN_SISTEMA")).thenReturn(0L);
        InitialAdminSeeder sinCorreo = new InitialAdminSeeder(usuarios, perfiles, roles,
                claves, "", "", "Administrador", "Sistema", "CF", "0000000000000", "00000000", "Ciudad");

        // Act
        sinCorreo.run(null);

        // Assert
        verify(usuarios, never()).save(any());
    }

    private static Rol rol(String codigo) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        return rol;
    }

    @Test
    void sinAdminCreaCuentaYPerfil() {
        // Arrange
        when(usuarios.countByEstadoAndRoles_Codigo("ACTIVO", "ADMIN_SISTEMA")).thenReturn(0L);
        when(usuarios.existsByCorreo("admin@correo.com")).thenReturn(false);
        when(roles.findAllById(List.of("ADMIN_SISTEMA", "CLIENTE")))
                .thenReturn(List.of(rol("ADMIN_SISTEMA"), rol("CLIENTE")));

        // Act
        seeder().run(null);

        // Assert
        verify(usuarios).save(any(Usuario.class));
        verify(perfiles).save(any(gt.cunoc.proyecto2.identity.models.Perfil.class));
    }

    @Test
    void elAdminInicialSoloTieneAdminSistemaYCliente() {
        // Arrange
        when(usuarios.countByEstadoAndRoles_Codigo("ACTIVO", "ADMIN_SISTEMA")).thenReturn(0L);
        when(usuarios.existsByCorreo("admin@correo.com")).thenReturn(false);
        when(roles.findAllById(List.of("ADMIN_SISTEMA", "CLIENTE")))
                .thenReturn(List.of(rol("ADMIN_SISTEMA"), rol("CLIENTE")));
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);

        // Act
        seeder().run(null);

        // Assert
        verify(usuarios).save(guardado.capture());
        assertThat(guardado.getValue().getRoles()).extracting(Rol::getCodigo)
                .containsExactlyInAnyOrder("ADMIN_SISTEMA", "CLIENTE");
        verify(roles, never()).findAll();
    }

    @Test
    void siFaltaUnRolEnLaBaseNoCreaNada() {
        // Arrange
        when(usuarios.countByEstadoAndRoles_Codigo("ACTIVO", "ADMIN_SISTEMA")).thenReturn(0L);
        when(usuarios.existsByCorreo("admin@correo.com")).thenReturn(false);
        when(roles.findAllById(List.of("ADMIN_SISTEMA", "CLIENTE"))).thenReturn(List.of(rol("ADMIN_SISTEMA")));

        // Act
        seeder().run(null);

        // Assert
        verify(usuarios, never()).save(any());
        verify(perfiles, never()).save(any());
    }

    @Test
    void correoExistenteNoDuplica() {
        // Arrange
        when(usuarios.countByEstadoAndRoles_Codigo("ACTIVO", "ADMIN_SISTEMA")).thenReturn(0L);
        when(usuarios.existsByCorreo("admin@correo.com")).thenReturn(true);

        // Act
        seeder().run(null);

        // Assert
        verify(usuarios, never()).save(any());
    }
}
