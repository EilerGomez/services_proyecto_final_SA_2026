package gt.cunoc.proyecto2.notification.messaging.consumer;

import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.notification.evento.recibido.LicenciaChoferActualizada;
import gt.cunoc.proyecto2.notification.services.recordatorios.RecordatorioLicenciaService;
import org.springframework.stereotype.Component;

/*
 * Programa los recordatorios de vencimiento de licencia 30, 15 y 3 días antes (de Fleet). No envía correos.
 */
@Component
public class ManejadorLicenciaChoferActualizada implements ManejadorEvento<LicenciaChoferActualizada> {

    private final RecordatorioLicenciaService recordatorios;

    public ManejadorLicenciaChoferActualizada(RecordatorioLicenciaService recordatorios) {
        this.recordatorios = recordatorios;
    }

    @Override
    public String tipoEvento() {
        return "LicenciaChoferActualizada";
    }

    @Override
    public Class<LicenciaChoferActualizada> tipoPayload() {
        return LicenciaChoferActualizada.class;
    }

    @Override
    public void manejar(EventoRecibido<LicenciaChoferActualizada> evento) {
        recordatorios.programar(evento.payload(), evento.businessDate());
    }
}