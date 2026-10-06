package gt.cunoc.proyecto2.identity.evento.handler;


import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.identity.evento.payload.MovimientoCarteraPayload;
import gt.cunoc.proyecto2.identity.evento.recibido.ReembolsoCarteraSolicitado;
import gt.cunoc.proyecto2.identity.models.MovimientoCartera;
import gt.cunoc.proyecto2.identity.services.cartera.WalletService;
import org.springframework.stereotype.Component;

import java.util.Optional;


/**
 * Acredita el reembolso y publica CarteraReembolsada.
 */
@Component
public class ManejadorReembolsoCartera implements ManejadorEvento<ReembolsoCarteraSolicitado> {

    private final WalletService cartera;

    private final PublicadorEventos publicador;

    public ManejadorReembolsoCartera(WalletService cartera, PublicadorEventos publicador) {
        this.cartera = cartera;
        this.publicador = publicador;
    }

    @Override
    public String tipoEvento() {
        return "ReembolsoCarteraSolicitado";
    }

    @Override
    public Class<ReembolsoCarteraSolicitado> tipoPayload() {
        return ReembolsoCarteraSolicitado.class;
    }

    @Override
    public void manejar(EventoRecibido<ReembolsoCarteraSolicitado> evento) {
        ReembolsoCarteraSolicitado solicitud = evento.payload();
        Optional<MovimientoCartera> reembolso = cartera.solicitarReembolso(
                solicitud.usuarioId(),
                solicitud.monto(),
                solicitud.referenciaTipo(),
                solicitud.referenciaId(),
                solicitud.motivo(),
                evento.businessDate());
        if (reembolso.isEmpty()) {
            return;
        }
        MovimientoCartera movimiento = reembolso.get();
        publicador.publicar(
                "Cartera",
                solicitud.usuarioId(),
                "CarteraReembolsada",
                new MovimientoCarteraPayload(
                        movimiento.getId(),
                        solicitud.referenciaTipo(),
                        solicitud.referenciaId(),
                        solicitud.usuarioId(),
                        movimiento.getMonto(),
                        movimiento.getSaldoResultante(),
                        movimiento.getFechaMovimiento()),
                evento.businessDate());
    }
}
