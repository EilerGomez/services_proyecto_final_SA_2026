package gt.cunoc.proyecto2.notification.services.correo;

import gt.cunoc.proyecto2.notification.evento.recibido.CuentaCreada;
import gt.cunoc.proyecto2.notification.evento.recibido.RolAsignado;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CorreoCuentasServiceTest {

    private static final UUID EVENTO_ID = UUID.randomUUID();
    private static final String URL = "http://localhost:5173/restablecer?token=abc";

    @Mock private EnvioCorreoService envio;

    private CorreoCuentasService servicio;

    private final ArgumentCaptor<String> datos = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);

    @BeforeEach
    void preparar() {
        servicio = new CorreoCuentasService(envio, JsonMapper.builder().build());
    }

    @Test
    void clienteRecibeLaBienvenidaDeSiempre() {
        servicio.bienvenida(EVENTO_ID, new CuentaCreada(UUID.randomUUID(), "ana@correo.com", "Ana",
                List.of("CLIENTE"), null, null));

        verify(envio).enviarBienvenida(EVENTO_ID, "ana@correo.com", "Ana", null);
        verify(envio, never()).enviarPlantilla(any(), any(), any(), any(), any(), any());
    }

    @Test
    void sinRolesTambienEsBienvenidaDeCliente() {
        servicio.bienvenida(EVENTO_ID, new CuentaCreada(UUID.randomUUID(), "ana@correo.com", "Ana",
                null, URL, null));

        verify(envio).enviarBienvenida(EVENTO_ID, "ana@correo.com", "Ana", URL);
    }

    @Test
    void adminDeSucursalNuevoRecibeSuPuestoSucursalYEnlace() {
        servicio.bienvenida(EVENTO_ID, new CuentaCreada(UUID.randomUUID(), "luis@correo.com", "Luis Gomez",
                List.of("ADMIN_SUCURSAL", "CLIENTE"), URL, "Xela Central"));

        verify(envio).enviarPlantilla(eq(EVENTO_ID), eq("luis@correo.com"), eq("CUENTA_CREADA"),
                eq("Bienvenido a la plataforma"), datos.capture(), cuerpo.capture());
        assertThat(cuerpo.getValue()).contains("Hola Luis Gomez", "Administrador de sucursal en «Xela Central»",
                "define tu contraseña", URL);
        assertThat(datos.getValue()).contains("\"rol\":\"ADMIN_SUCURSAL\"", "\"sucursal\":\"Xela Central\"");
    }

    @Test
    void adminDelSistemaSinEnlaceSoloInvitaAIniciarSesion() {
        servicio.bienvenida(EVENTO_ID, new CuentaCreada(UUID.randomUUID(), "eva@correo.com", "Eva",
                List.of("CLIENTE", "ADMIN_SISTEMA"), null, "Ignorada"));

        verify(envio).enviarPlantilla(eq(EVENTO_ID), eq("eva@correo.com"), eq("CUENTA_CREADA"), anyString(),
                anyString(), cuerpo.capture());
        assertThat(cuerpo.getValue()).contains("como Administrador del sistema.", "Ya puedes iniciar sesión")
                .doesNotContain("Ignorada");
    }

    @Test
    void rolAsignadoPideVolverAIniciarSesion() {
        servicio.rolAsignado(EVENTO_ID, new RolAsignado(UUID.randomUUID(), "ana@correo.com", "Ana López",
                "CAJERO", UUID.randomUUID(), "Xela Central"));

        verify(envio).enviarPlantilla(eq(EVENTO_ID), eq("ana@correo.com"), eq("ROL_ASIGNADO"),
                eq("Tienes un nuevo rol en la plataforma"), datos.capture(), cuerpo.capture());
        assertThat(cuerpo.getValue()).contains("Hola Ana López", "Cajero en «Xela Central»",
                "cierra sesión y vuelve a iniciar sesión");
        assertThat(datos.getValue()).contains("\"rol\":\"CAJERO\"");
    }

    @Test
    void descripcionesDeRoles() {
        assertThat(CorreoCuentasService.descripcion("ADMIN_SISTEMA", null)).isEqualTo("Administrador del sistema");
        assertThat(CorreoCuentasService.descripcion("ADMIN_SUCURSAL", " ")).isEqualTo("Administrador de sucursal");
        assertThat(CorreoCuentasService.descripcion("CHOFER", "Xela")).isEqualTo("Chofer en «Xela»");
        assertThat(CorreoCuentasService.descripcion(null, null)).isEqualTo("usuario");
        assertThat(CorreoCuentasService.descripcion("OTRO", null)).isEqualTo("usuario");
    }
}
