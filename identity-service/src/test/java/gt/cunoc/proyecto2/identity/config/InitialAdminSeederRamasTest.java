package gt.cunoc.proyecto2.identity.config;

import gt.cunoc.proyecto2.identity.repositories.PerfilRepository;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** InitialAdminSeeder con contraseña en blanco: avisa y no crea nada. */
@ExtendWith(MockitoExtension.class)
class InitialAdminSeederRamasTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private PerfilRepository perfiles;
    @Mock private RolRepository roles;

    @Spy private PasswordEncoder claves = new BCryptPasswordEncoder();

    @Test
    void sinContrasenaConfiguradaSoloAvisa() {
        // Arrange
        when(usuarios.countByEstadoAndRoles_Codigo("ACTIVO", "ADMIN_SISTEMA")).thenReturn(0L);
        InitialAdminSeeder sinClave = new InitialAdminSeeder(usuarios, perfiles, roles,
                claves, "admin@correo.com", "", "Administrador", "Sistema",
                "CF", "0000000000000", "00000000", "Ciudad");

        // Act
        sinClave.run(null);

        // Assert
        verify(usuarios, never()).save(any());
    }
}
