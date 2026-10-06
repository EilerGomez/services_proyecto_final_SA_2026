package gt.cunoc.proyecto2.identity.evento.handler;


import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.identity.evento.payload.DebitoRechazadoPayload;
import gt.cunoc.proyecto2.identity.evento.payload.MovimientoCarteraPayload;
import gt.cunoc.proyecto2.identity.evento.recibido.DebitoCarteraSolicitado;
import gt.cunoc.proyecto2.identity.models.MovimientoCartera;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.CarteraRepository;
import gt.cunoc.proyecto2.identity.repositories.MovimientoRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import gt.cunoc.proyecto2.identity.services.cartera.WalletService;
import org.springframework.stereotype.Component;

import java.util.Optional;


/**
 * Saga de pago: debita y responde CarteraDebitada o DebitoCarteraRechazado.
 *
 * <p>El rechazo es un resultado normal: se publica y la transacción se confirma.
 */
@Component
public class ManejadorDebitoCartera implements ManejadorEvento<DebitoCarteraSolicitado> {

    private final UsuarioRepository usuarios;

    private final CarteraRepository carteras;

    private final MovimientoRepository movimientos;

    private final WalletService servicio;

    private final PublicadorEventos publicador;

    public ManejadorDebitoCartera(
            UsuarioRepository usuarios,
            CarteraRepository carteras,
            MovimientoRepository movimientos,
            WalletService servicio,
            PublicadorEventos publicador) {
        this.usuarios = usuarios;
        this.carteras = carteras;
        this.movimientos = movimientos;
        this.servicio = servicio;
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
        Usuario usuario = usuarios.findById(solicitud.usuarioId()).orElse(null);
        if (usuario == null || !"ACTIVO".equals(usuario.getEstado())) {
            rechazar(solicitud, evento, "USUARIO_INACTIVO");
            return;
        }
        if (carteras.findByUsuarioId(solicitud.usuarioId()).isEmpty()) {
            rechazar(solicitud, evento, "CARTERA_INEXISTENTE");
            return;
        }
        Optional<MovimientoCartera> previo =
                movimientos.findByTipoAndReferenciaId("DEBITO", solicitud.referenciaId());
        if (previo.isPresent()) {
            publicarDebitada(solicitud, evento, previo.get());
            return;
        }
        Optional<MovimientoCartera> debito = servicio.intentarDebitar(
                solicitud.usuarioId(), solicitud.monto(), solicitud.referenciaTipo(),
                solicitud.referenciaId(), evento.businessDate());
        if (debito.isPresent()) {
            publicarDebitada(solicitud, evento, debito.get());
        } else {
            rechazar(solicitud, evento, "SALDO_INSUFICIENTE");
        }
    }

    private void publicarDebitada(
            DebitoCarteraSolicitado solicitud, EventoRecibido<DebitoCarteraSolicitado> evento,
            MovimientoCartera movimiento) {
        publicador.publicar(
                "Cartera", solicitud.usuarioId(), "CarteraDebitada",
                new MovimientoCarteraPayload(
                        movimiento.getId(), solicitud.referenciaTipo(), solicitud.referenciaId(),
                        solicitud.usuarioId(), solicitud.monto(),
                        movimiento.getSaldoResultante(), movimiento.getFechaMovimiento()),
                evento.businessDate());
    }

    private void rechazar(
            DebitoCarteraSolicitado solicitud, EventoRecibido<DebitoCarteraSolicitado> evento, String motivo) {
        publicador.publicar(
                "Cartera", solicitud.usuarioId(), "DebitoCarteraRechazado",
                new DebitoRechazadoPayload(
                        solicitud.referenciaTipo(), solicitud.referenciaId(), solicitud.usuarioId(),
                        solicitud.monto(), motivo),
                evento.businessDate());
    }
}
