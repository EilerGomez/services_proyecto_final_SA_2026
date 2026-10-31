package gt.cunoc.proyecto2.notification.services.correo;

import gt.cunoc.proyecto2.notification.clientes.ClienteIdentity;
import gt.cunoc.proyecto2.notification.clientes.ContactoUsuario;
import gt.cunoc.proyecto2.notification.evento.recibido.AlquilerAsignado;
import gt.cunoc.proyecto2.notification.evento.recibido.BoletoReasignado;
import gt.cunoc.proyecto2.notification.evento.recibido.ViajeProgramado;
import gt.cunoc.proyecto2.notification.evento.recibido.ViajeReasignado;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Avisos de cambio de bus o de piloto: qué dice el asunto y qué dice el cuerpo. */
@ExtendWith(MockitoExtension.class)
class CorreoViajesServiceTest {

    private static final UUID EVENTO_ID = UUID.randomUUID();
    /** 5-nov 08:00 en Guatemala (14:00 UTC). */
    private static final OffsetDateTime SALIDA = OffsetDateTime.parse("2026-11-05T14:00:00Z");

    @Mock private EnvioCorreoService envio;
    @Mock private ClienteIdentity identity;

    private CorreoViajesService servicio;

    private final ArgumentCaptor<String> asunto = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> datos = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);

    @BeforeEach
    void preparar() {
        servicio = new CorreoViajesService(envio, identity, JsonMapper.builder().build());
    }

    private void capturar(String correo) {
        verify(envio).enviarPlantilla(eq(EVENTO_ID), eq(correo), eq(CorreoViajesService.VIAJE_REASIGNADO),
                asunto.capture(), datos.capture(), cuerpo.capture());
    }

    private static BoletoReasignado boleto(boolean cambioBus, boolean cambioChofer) {
        return new BoletoReasignado(UUID.randomUUID(), "A1B2C3D4E5F6", UUID.randomUUID(), "Ana López",
                "ana@correo.com", List.of(5, 6), "Quetzaltenango", "Guatemala", SALIDA,
                cambioBus, "P-111AAA", "P-222BBB", cambioChofer, "Juan Pérez", "Carla Ramos");
    }

    private static ViajeReasignado alquiler(String correo) {
        return new ViajeReasignado(UUID.randomUUID(), "PRIVADO", "Xela", "Antigua", SALIDA,
                true, "P-111AAA", "P-222BBB", false, "Juan Pérez", "Juan Pérez",
                "Colegio San José", correo);
    }

    @Test
    void alPasajeroSeLeDiceLaPlacaNuevaYSusAsientos() {
        servicio.boletoReasignado(EVENTO_ID, boleto(true, false));

        capturar("ana@correo.com");
        assertAll(
                () -> assertTrue(asunto.getValue().startsWith("Cambio de bus:")),
                () -> assertTrue(cuerpo.getValue().contains("P-222BBB")),
                () -> assertTrue(cuerpo.getValue().contains("P-111AAA")),
                () -> assertTrue(cuerpo.getValue().contains("5, 6")),
                () -> assertTrue(datos.getValue().contains("\"cambioBus\":true")));
    }

    @Test
    void siCambianLosDosElAsuntoLoDice() {
        servicio.boletoReasignado(EVENTO_ID, boleto(true, true));

        capturar("ana@correo.com");
        assertAll(
                () -> assertTrue(asunto.getValue().startsWith("Cambio de bus y de piloto:")),
                () -> assertTrue(cuerpo.getValue().contains("Carla Ramos")),
                () -> assertTrue(cuerpo.getValue().contains("Juan Pérez")));
    }

    @Test
    void siSoloCambiaElPilotoNoSeHablaDelBus() {
        servicio.boletoReasignado(EVENTO_ID, boleto(false, true));

        capturar("ana@correo.com");
        assertAll(
                () -> assertTrue(asunto.getValue().startsWith("Cambio de piloto:")),
                () -> assertTrue(cuerpo.getValue().contains("Carla Ramos")),
                () -> assertFalse(cuerpo.getValue().contains("El bus asignado ahora es")));
    }

    @Test
    void alClienteDelAlquilerSeLeEscribeASuCorreo() {
        servicio.viajeReasignado(EVENTO_ID, alquiler("colegio@correo.com"));

        capturar("colegio@correo.com");
        assertAll(
                () -> assertTrue(cuerpo.getValue().contains("Colegio San José")),
                () -> assertTrue(cuerpo.getValue().contains("P-222BBB")),
                () -> assertTrue(cuerpo.getValue().contains("no cambian")));
    }

    // ----- Aviso al piloto -----

    private static final UUID PILOTO = UUID.randomUUID();

    private void elPilotoTieneCuenta() {
        when(identity.contacto(PILOTO))
                .thenReturn(Optional.of(new ContactoUsuario(PILOTO, "Juan Pérez", "juan@correo.com", "55551111")));
    }

    private static ViajeProgramado viaje(String tipo, String sentido, UUID piloto) {
        return new ViajeProgramado(UUID.randomUUID(), tipo, UUID.randomUUID(), "Quetzaltenango", "Guatemala",
                "P-123ABC", UUID.randomUUID(), "Juan Pérez", piloto, SALIDA, SALIDA.plusHours(4), sentido);
    }

    private void capturarAlPiloto() {
        verify(envio).enviarPlantilla(eq(EVENTO_ID), eq("juan@correo.com"),
                eq(CorreoViajesService.VIAJE_ASIGNADO_PILOTO), asunto.capture(), datos.capture(), cuerpo.capture());
    }

    @Test
    void alPilotoSeLeAvisaElViajeQueSeLeAsigno() {
        elPilotoTieneCuenta();

        servicio.viajeAsignadoAlPiloto(EVENTO_ID, viaje("REGULAR", "IDA", PILOTO));

        capturarAlPiloto();
        assertAll(
                () -> assertTrue(asunto.getValue().startsWith("Viaje asignado:")),
                () -> assertTrue(cuerpo.getValue().contains("Quetzaltenango → Guatemala")),
                () -> assertTrue(cuerpo.getValue().contains("P-123ABC")),
                () -> assertTrue(cuerpo.getValue().contains("Juan Pérez")),
                () -> assertTrue(datos.getValue().contains("\"busPlaca\":\"P-123ABC\"")));
    }

    /** El de vuelta se nombra como tal para que el piloto no lo confunda con la ida. */
    @Test
    void elViajeDeRegresoSeAvisaComoRegreso() {
        elPilotoTieneCuenta();

        servicio.viajeAsignadoAlPiloto(EVENTO_ID, viaje("REGULAR", "REGRESO", PILOTO));

        capturarAlPiloto();
        assertTrue(cuerpo.getValue().contains("Viaje de regreso"));
    }

    /** Sin la cuenta del piloto no hay a quién escribirle: no se intenta ni se falla. */
    @Test
    void sinLaCuentaDelPilotoNoSeEscribe() {
        servicio.viajeAsignadoAlPiloto(EVENTO_ID, viaje("REGULAR", "IDA", null));

        verify(envio, never()).enviarPlantilla(any(), any(), any(), any(), any(), any());
    }

    @Test
    void alPilotoDeUnAlquilerSeLeAvisaConElCliente() {
        elPilotoTieneCuenta();
        AlquilerAsignado asignado = new AlquilerAsignado(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Colegio San José", "colegio@correo.com", "Xela", "Antigua", SALIDA, SALIDA.plusHours(10),
                "P-123ABC", "Juan Pérez", PILOTO);

        servicio.alquilerAsignadoAlPiloto(EVENTO_ID, asignado);

        capturarAlPiloto();
        assertAll(
                () -> assertTrue(asunto.getValue().startsWith("Alquiler privado asignado:")),
                () -> assertTrue(cuerpo.getValue().contains("Colegio San José")),
                () -> assertTrue(cuerpo.getValue().contains("Xela → Antigua")));
    }

    @Test
    void sinCorreoNoSeEscribeANadie() {
        // Es el caso de un viaje REGULAR: sus pasajeros llegan por BoletoReasignado, no por acá.
        servicio.viajeReasignado(EVENTO_ID, alquiler(null));

        verify(envio, never()).enviarPlantilla(any(), any(), any(), any(), any(), any());
    }
}
