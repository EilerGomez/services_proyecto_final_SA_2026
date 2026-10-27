package gt.cunoc.proyecto2.sales.eventos.manejadores.trips;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.sales.eventos.TipoEvento;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeReasignado;
import gt.cunoc.proyecto2.sales.servicios.ventas.AvisoReasignacionServicio;
import org.springframework.stereotype.Component;

/*
 * ViajeReasignado (Trips): le cambiaron el bus o el piloto a un viaje programado. Sales publica un
 * BoletoReasignado por cada pasajero con boleto confirmado y correo, para que Notification le avise.
 * De un viaje PRIVADO no se ocupa: ese no tiene boletos y a su cliente le escribe Notification.
 */
@Component
public class ManejadorViajeReasignado implements ManejadorEvento<ViajeReasignado> {

    private final AvisoReasignacionServicio avisos;

    public ManejadorViajeReasignado(AvisoReasignacionServicio avisos) {
        this.avisos = avisos;
    }

    @Override
    public String tipoEvento() {
        return TipoEvento.VIAJE_REASIGNADO.nombre();
    }

    @Override
    public Class<ViajeReasignado> tipoPayload() {
        return ViajeReasignado.class;
    }

    @Override
    public void manejar(EventoRecibido<ViajeReasignado> evento) {
        if (evento.payload().esRegular()) {
            avisos.avisarALosPasajeros(evento.payload());
        }
    }
}
