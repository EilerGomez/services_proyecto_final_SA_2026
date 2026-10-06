package gt.cunoc.proyecto2.identity.evento.handler;


import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.identity.evento.payload.DebitoRechazadoPayload;
import gt.cunoc.proyecto2.identity.evento.payload.MovimientoCarteraPayload;
import gt.cunoc.proyecto2.identity.evento.recibido.DebitoCarteraSolicitado;
import gt.cunoc.proyecto2.identity.models.MovimientoCartera;
import gt.cunoc.proyecto2.identity.services.cartera.WalletService;
import org.springframework.stereotype.Component;

import java.util.Optional;


/**
 * Debita la cartera y responde CarteraDebitada o DebitoCarteraRechazado.
 */
@Component
public class ManejadorDebitoCartera implements ManejadorEvento<DebitoCarteraSolicitado> {

    private final WalletService cartera;

    private final PublicadorEventos publicador;

    public ManejadorDebitoCartera(WalletService cartera, PublicadorEventos publicador) {
        this.cartera = cartera;
        this.publicador = publicador;
    }

    @Override
    public String tipoEvento() {
        return "DebitoCarteraSolicitado";
    }

    @Override
    public Class<DebitoCarteraSolicitado> tipoPayload() {
        return DebitoCarteraSolicitado.class;
    }

    @Override
    public void manejar(EventoRecibido<DebitoCarteraSolicitado> evento) {
        DebitoCarteraSolicitado solicitud = evento.payload();
        Optional<MovimientoCartera> debito = cartera.intentarDebitar(
                solicitud.usuarioId(),
                solicitud.monto(),
                solicitud.referenciaTipo(),
                solicitud.referenciaId(),
                evento.businessDate());
        if (debito.isPresent()) {
            MovimientoCartera movimiento = debito.get();
            publicador.publicar(
                    "Cartera",
                    solicitud.usuarioId(),
                    "CarteraDebitada",
                    new MovimientoCarteraPayload(
                            movimiento.getId(),
                            solicitud.referenciaTipo(),
                            solicitud.referenciaId(),
                            solicitud.usuarioId(),
                            solicitud.monto(),
                            movimiento.getSaldoResultante(),
                            movimiento.getFechaMovimiento()),
                    evento.businessDate());
        } else {
            publicador.publicar(
                    "Cartera",
                    solicitud.usuarioId(),
                    "DebitoCarteraRechazado",
                    new DebitoRechazadoPayload(
                            solicitud.referenciaTipo(),
                            solicitud.referenciaId(),
                            solicitud.usuarioId(),
                            solicitud.monto(),
                            "SALDO_INSUFICIENTE"),
                    evento.businessDate());
        }
    }
}
