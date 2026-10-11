package gt.cunoc.proyecto2.sales.eventos.manejadores.identity;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.TipoEvento;
import gt.cunoc.proyecto2.sales.eventos.recibidos.identity.ClienteRegistrado;
import gt.cunoc.proyecto2.sales.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/*
 * ClienteRegistrado (Identity): agrega el cliente a vista_clientes; desde aquí puede comprar en línea (S2).
 */
@Component
public class ManejadorClienteRegistrado implements ManejadorEvento<ClienteRegistrado> {

    private final ProyeccionServicio proyecciones;

    public ManejadorClienteRegistrado(ProyeccionServicio proyecciones) {
        this.proyecciones = proyecciones;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.CLIENTE_REGISTRADO.nombre();
    }

    @Override
    public Class<ClienteRegistrado> tipoPayload() {
        return ClienteRegistrado.class;
    }

    @Override
    public void manejar(EventoRecibido<ClienteRegistrado> evento) {
        ClienteRegistrado cliente = evento.payload();
        proyecciones.guardarCliente(cliente.usuarioId(), cliente.nit(), cliente.nombre(), cliente.correo());
    }
}
