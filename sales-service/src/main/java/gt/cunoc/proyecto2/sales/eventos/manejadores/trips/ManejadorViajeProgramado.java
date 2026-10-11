package gt.cunoc.proyecto2.sales.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.TipoEvento;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeEvento;
import gt.cunoc.proyecto2.sales.servicios.proyecciones.ProyeccionServicio;
import org.springframework.stereotype.Component;

/* ViajeProgramado (Trips): copia el viaje REGULAR a vista_viajes y crea sus asientos (1..capacidad). */
@Component
public class ManejadorViajeProgramado implements ManejadorEvento<ViajeEvento> {

    private final ProyeccionServicio proyecciones;

    public ManejadorViajeProgramado(ProyeccionServicio proyecciones) {
        this.proyecciones = proyecciones;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_PROGRAMADO.nombre();
    }

    @Override
    public Class<ViajeEvento> tipoPayload() {
        return ViajeEvento.class;
    }

    @Override
    public void manejar(EventoRecibido<ViajeEvento> evento) {
        if (evento.payload().esRegular()) {                  // los PRIVADO no se venden por boleto
            proyecciones.guardarViaje(evento.payload(), evento.aggregateVersion());
        }
    }
}
