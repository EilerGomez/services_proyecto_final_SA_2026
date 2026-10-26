package gt.cunoc.proyecto2.notification.services.correo;

import gt.cunoc.proyecto2.notification.clientes.AdminContacto;
import gt.cunoc.proyecto2.notification.clientes.ClienteIdentity;
import gt.cunoc.proyecto2.notification.evento.recibido.RutaActualizada;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorreoRutasServiceTest {

    private static final UUID EVENTO_ID = UUID.randomUUID();
    private static final UUID ORIGEN = UUID.randomUUID();
    private static final UUID DESTINO = UUID.randomUUID();

    @Mock private EnvioCorreoService envio;
    @Mock private ClienteIdentity identity;

    private CorreoRutasService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CorreoRutasService(envio, identity, JsonMapper.builder().build());
    }

    @Test
    void avisaALosAdministradoresDeOrigenYDestinoSinRepetir() {
        // Arrange: Ana administra las dos sucursales; Luis solo la de destino
        AdminContacto ana = new AdminContacto(UUID.randomUUID(), "Ana", "ana@correo.com");
        AdminContacto luis = new AdminContacto(UUID.randomUUID(), "Luis", "luis@correo.com");
        when(identity.administradores(ORIGEN)).thenReturn(List.of(ana));
        when(identity.administradores(DESTINO)).thenReturn(List.of(ana, luis));
        RutaActualizada ruta = new RutaActualizada(UUID.randomUUID(), ORIGEN, DESTINO,
                "Quetzaltenango", "Guatemala", new BigDecimal("205.50"), new BigDecimal("75.00"), true);

        // Act
        servicio.rutaActualizada(EVENTO_ID, ruta);

        // Assert
        ArgumentCaptor<String> correo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> datos = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(envio, times(2)).enviarPlantilla(eq(EVENTO_ID), correo.capture(),
                eq(CorreoRutasService.RUTA_ACTUALIZADA),
                eq("Cambió la ruta Quetzaltenango → Guatemala"), datos.capture(), cuerpo.capture());
        assertAll(
                () -> assertEquals(List.of("ana@correo.com", "luis@correo.com"), correo.getAllValues()),
                () -> assertTrue(datos.getValue().contains("\"precioBoleto\":75.00")),
                () -> assertTrue(cuerpo.getValue().contains("Distancia actual: 205.5 km")),
                () -> assertTrue(cuerpo.getValue().contains("Precio actual del boleto: Q 75.00")),
                () -> assertTrue(cuerpo.getValue().contains("Estado: activa")),
                () -> assertTrue(cuerpo.getValue().contains("conservan el precio")));
    }

    @Test
    void unaSolaSucursalSeConsultaUnaVez() {
        // Arrange
        AdminContacto ana = new AdminContacto(UUID.randomUUID(), "Ana", "ana@correo.com");
        when(identity.administradores(ORIGEN)).thenReturn(List.of(ana));
        RutaActualizada ruta = new RutaActualizada(UUID.randomUUID(), ORIGEN, ORIGEN,
                "Xela", "Xela", null, null, false);

        // Act
        servicio.rutaActualizada(EVENTO_ID, ruta);

        // Assert
        verify(identity).administradores(ORIGEN);
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(envio).enviarPlantilla(eq(EVENTO_ID), eq("ana@correo.com"),
                eq(CorreoRutasService.RUTA_ACTUALIZADA), anyString(), anyString(), cuerpo.capture());
        assertAll(
                () -> assertTrue(cuerpo.getValue().contains("Distancia actual: -")),
                () -> assertTrue(cuerpo.getValue().contains("Precio actual del boleto: -")),
                () -> assertTrue(cuerpo.getValue().contains("Estado: inactiva")));
    }

    @Test
    void sinSucursalesNoConsultaNiEnvia() {
        // Arrange
        RutaActualizada ruta = new RutaActualizada(UUID.randomUUID(), null, null, null, null, null, null, true);

        // Act
        servicio.rutaActualizada(EVENTO_ID, ruta);

        // Assert
        verifyNoInteractions(identity, envio);
    }

    @Test
    void administradoresSinCorreoSeIgnoran() {
        // Arrange
        when(identity.administradores(ORIGEN))
                .thenReturn(List.of(new AdminContacto(UUID.randomUUID(), "Sin correo", "  ")));
        RutaActualizada ruta = new RutaActualizada(UUID.randomUUID(), ORIGEN, null, "Xela", "Zunil",
                new BigDecimal("10"), new BigDecimal("20"), true);

        // Act
        servicio.rutaActualizada(EVENTO_ID, ruta);

        // Assert
        verifyNoInteractions(envio);
    }
}
