package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.BoletoReasignado;
import gt.cunoc.proyecto2.notification.evento.recibido.ViajeProgramado;
import gt.cunoc.proyecto2.notification.evento.recibido.ViajeReasignado;
import gt.cunoc.proyecto2.notification.services.correo.CorreoViajesService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

/** Los dos manejadores del cambio de bus o de piloto: nombre, payload y a quién delegan. */
@ExtendWith(MockitoExtension.class)
class ManejadoresReasignacionTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 1);
    private static final OffsetDateTime SALIDA = OffsetDateTime.parse("2026-11-05T08:00:00-06:00");

    @Mock private CorreoViajesService correos;

    @InjectMocks private ManejadorViajeReasignado manejadorViaje;
    @InjectMocks private ManejadorViajeProgramado manejadorProgramado;
    @InjectMocks private ManejadorBoletoReasignado manejadorBoleto;

    private static <T> EventoRecibido<T> evento(String tipo, T payload) {
        return new EventoRecibido<>(UUID.randomUUID(), tipo, 1, UUID.randomUUID(), 0L, Instant.now(), FECHA,
                UUID.randomUUID(), payload);
    }

    @Test
    void viajeReasignadoLeEscribeAlClienteDelAlquiler() {
        ViajeReasignado viaje = new ViajeReasignado(UUID.randomUUID(), "PRIVADO", "Xela", "Antigua", SALIDA,
                true, "P-111AAA", "P-222BBB", false, "Juan Pérez", "Juan Pérez",
                "Colegio San José", "colegio@correo.com");
        EventoRecibido<ViajeReasignado> recibido = evento("ViajeReasignado", viaje);

        manejadorViaje.manejar(recibido);

        assertEquals("ViajeReasignado", manejadorViaje.tipoEvento());
        assertEquals(ViajeReasignado.class, manejadorViaje.tipoPayload());
        verify(correos).viajeReasignado(recibido.eventId(), viaje);
    }

    @Test
    void viajeProgramadoLeEscribeAlPiloto() {
        ViajeProgramado viaje = new ViajeProgramado(UUID.randomUUID(), "REGULAR", UUID.randomUUID(),
                "Quetzaltenango", "Guatemala", "P-123ABC", UUID.randomUUID(), "Juan Pérez", UUID.randomUUID(),
                SALIDA, SALIDA.plusHours(4), "IDA");
        EventoRecibido<ViajeProgramado> recibido = evento("ViajeProgramado", viaje);

        manejadorProgramado.manejar(recibido);

        assertEquals("ViajeProgramado", manejadorProgramado.tipoEvento());
        assertEquals(ViajeProgramado.class, manejadorProgramado.tipoPayload());
        verify(correos).viajeAsignadoAlPiloto(recibido.eventId(), viaje);
    }

    @Test
    void boletoReasignadoLeEscribeAlPasajero() {
        BoletoReasignado boleto = new BoletoReasignado(UUID.randomUUID(), "A1B2C3D4E5F6", UUID.randomUUID(),
                "Ana López", "ana@correo.com", List.of(5, 6), "Quetzaltenango", "Guatemala", SALIDA,
                true, "P-111AAA", "P-222BBB", true, "Juan Pérez", "Carla Ramos");
        EventoRecibido<BoletoReasignado> recibido = evento("BoletoReasignado", boleto);

        manejadorBoleto.manejar(recibido);

        assertEquals("BoletoReasignado", manejadorBoleto.tipoEvento());
        assertEquals(BoletoReasignado.class, manejadorBoleto.tipoPayload());
        verify(correos).boletoReasignado(recibido.eventId(), boleto);
    }
}
