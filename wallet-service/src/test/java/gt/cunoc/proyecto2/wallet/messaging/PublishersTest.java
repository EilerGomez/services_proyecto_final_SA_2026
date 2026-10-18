package gt.cunoc.proyecto2.wallet.messaging;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.wallet.evento.payload.DebitoRechazadoPayload;
import gt.cunoc.proyecto2.wallet.evento.payload.MovimientoCarteraPayload;
import gt.cunoc.proyecto2.wallet.messaging.publisher.CarteraEventPublisher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/** Fija los contratos de los tres eventos de la saga: iguales a los que publicaba Identity. */
class PublishersTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID REFERENCIA = UUID.randomUUID();
    private static final UUID MOVIMIENTO = UUID.randomUUID();
    private static final BigDecimal MONTO = new BigDecimal("25.50");
    private static final BigDecimal SALDO = new BigDecimal("80.00");
    private static final LocalDate FECHA = LocalDate.of(2026, 9, 20);
    private static final LocalDate FECHA_MOVIMIENTO = LocalDate.of(2026, 9, 19);

    private final PublicadorEventos outbox = mock(PublicadorEventos.class);
    private final CarteraEventPublisher cartera = new CarteraEventPublisher(outbox);

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
                Arguments.of("Cartera", "CarteraDebitada",
                        new MovimientoCarteraPayload(MOVIMIENTO, "VENTA", REFERENCIA, USUARIO, MONTO, SALDO, FECHA_MOVIMIENTO),
                        (Consumer<PublishersTest>) t -> t.cartera.publicarCarteraDebitada(
                                MOVIMIENTO, "VENTA", REFERENCIA, USUARIO, MONTO, SALDO, FECHA_MOVIMIENTO, FECHA)),
                Arguments.of("Cartera", "CarteraReembolsada",
                        new MovimientoCarteraPayload(MOVIMIENTO, "VENTA", REFERENCIA, USUARIO, MONTO, SALDO, FECHA_MOVIMIENTO),
                        (Consumer<PublishersTest>) t -> t.cartera.publicarCarteraReembolsada(
                                MOVIMIENTO, "VENTA", REFERENCIA, USUARIO, MONTO, SALDO, FECHA_MOVIMIENTO, FECHA)),
                Arguments.of("Cartera", "DebitoCarteraRechazado",
                        new DebitoRechazadoPayload("VENTA", REFERENCIA, USUARIO, MONTO, "SALDO_INSUFICIENTE"),
                        (Consumer<PublishersTest>) t -> t.cartera.publicarDebitoCarteraRechazado(
                                "VENTA", REFERENCIA, USUARIO, MONTO, "SALDO_INSUFICIENTE", FECHA)));
    }
}
