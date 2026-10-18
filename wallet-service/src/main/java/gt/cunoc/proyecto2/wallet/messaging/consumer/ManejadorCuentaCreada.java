package gt.cunoc.proyecto2.wallet.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.wallet.evento.recibido.CuentaCreada;
import gt.cunoc.proyecto2.wallet.services.WalletService;
import org.springframework.stereotype.Component;

/**
 * CuentaCreada (Identity): si la cuenta es de cliente, le abre la cartera en cero.
 * Identity ya no crea carteras; las cuentas sin este evento (p. ej. el admin inicial) la obtienen
 * la primera vez que entran a su cartera.
 */
@Component
public class ManejadorCuentaCreada implements ManejadorEvento<CuentaCreada> {

    private final WalletService servicio;

    public ManejadorCuentaCreada(WalletService servicio) {
        this.servicio = servicio;
    }

    @Override
    public String tipoEvento() {
        return "CuentaCreada";
    }

    @Override
    public Class<CuentaCreada> tipoPayload() {
        return CuentaCreada.class;
    }

    @Override
    public void manejar(EventoRecibido<CuentaCreada> evento) {
        CuentaCreada cuenta = evento.payload();
        if (cuenta.usuarioId() != null && cuenta.esCliente()) {
            servicio.abrirCartera(cuenta.usuarioId());
        }
    }
}
