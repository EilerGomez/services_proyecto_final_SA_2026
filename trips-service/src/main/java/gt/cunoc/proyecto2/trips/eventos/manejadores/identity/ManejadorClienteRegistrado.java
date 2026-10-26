package gt.cunoc.proyecto2.trips.eventos.manejadores.identity;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.trips.eventos.TipoEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.identity.ClienteRegistrado;
import gt.cunoc.proyecto2.trips.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/**
 * ClienteRegistrado (Identity): guarda al cliente en vista_clientes. Con esa copia, un alquiler
 * pedido en ventanilla con su NIT queda ligado a su cuenta y le aparece en "Mis alquileres".
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
