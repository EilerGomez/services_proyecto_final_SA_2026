package gt.cunoc.proyecto2.identity.messaging;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.identity.evento.payload.ClienteRegistradoPayload;
import gt.cunoc.proyecto2.identity.evento.payload.CuentaCreadaPayload;
import gt.cunoc.proyecto2.identity.evento.payload.PerfilActualizadoPayload;
import gt.cunoc.proyecto2.identity.evento.payload.RecuperacionContrasenaPayload;
import gt.cunoc.proyecto2.identity.evento.payload.TarifaDepreciacionPayload;
import gt.cunoc.proyecto2.identity.messaging.publisher.TarifaEventPublisher;
import gt.cunoc.proyecto2.identity.messaging.publisher.UsuarioEventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.mockito.Mockito.*;

/** Fija los contratos de los eventos de Identity (los de cartera ahora los publica wallet-service). */
class PublishersTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID CHOFER = UUID.randomUUID();
    private static final BigDecimal MONTO = new BigDecimal("25.50");
    private static final LocalDate FECHA = LocalDate.of(2026, 9, 20);
    private static final LocalDate FECHA_MOVIMIENTO = LocalDate.of(2026, 9, 19);
    private static final Instant EXPIRACION = Instant.parse("2026-09-21T12:00:00Z");

    private final PublicadorEventos outbox = mock(PublicadorEventos.class);
    private final UsuarioEventPublisher usuarios = new UsuarioEventPublisher(outbox);
    private final TarifaEventPublisher tarifas = new TarifaEventPublisher(outbox);

    @ParameterizedTest(name = "{1}")
    @MethodSource("contratos")
    void conservaTipoAgregadoIdentificadorEventoPayloadYFecha(String agregado, String evento,
            Object payload, Consumer<PublishersTest> publicar) {
        publicar.accept(this);
        verify(outbox).publicar(agregado, USUARIO, evento, payload, FECHA);
        verifyNoMoreInteractions(outbox);
    }

    static Stream<Arguments> contratos() {
        return Stream.of(
                contrato("Usuario", "CuentaCreada",
                        new CuentaCreadaPayload(USUARIO, "correo", "nombre", List.of("CLIENTE"), null),
                        t -> t.usuarios.publicarCuentaCreada(USUARIO, "correo", "nombre", List.of("CLIENTE"), null, FECHA)),
                contrato("Usuario", "ClienteRegistrado",
                        new ClienteRegistradoPayload(USUARIO, "nit", "nombre", "correo"),
                        t -> t.usuarios.publicarClienteRegistrado(USUARIO, "nit", "nombre", "correo", FECHA)),
                contrato("Usuario", "RecuperacionContrasenaSolicitada",
                        new RecuperacionContrasenaPayload(USUARIO, "correo", "nombre", "url", EXPIRACION),
                        t -> t.usuarios.publicarRecuperacionContrasenaSolicitada(
                                USUARIO, "correo", "nombre", "url", EXPIRACION, FECHA)),
                contrato("Usuario", "PerfilActualizado",
                        new PerfilActualizadoPayload(USUARIO, "nit", "nombres", "apellidos", "correo", "telefono", "foto", CHOFER),
                        t -> t.usuarios.publicarPerfilActualizado(
                                USUARIO, "nit", "nombres", "apellidos", "correo", "telefono", "foto", CHOFER, FECHA)),
                contrato("TarifaDepreciacion", "TarifaDepreciacionCambiada",
                        new TarifaDepreciacionPayload(USUARIO, MONTO, FECHA_MOVIMIENTO),
                        t -> t.tarifas.publicarTarifaDepreciacionCambiada(USUARIO, MONTO, FECHA_MOVIMIENTO, FECHA)));
    }

    private static Arguments contrato(String agregado, String evento, Object payload, Consumer<PublishersTest> publicar) {
        return Arguments.of(agregado, evento, payload, publicar);
    }
}
