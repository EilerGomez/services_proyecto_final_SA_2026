package gt.cunoc.proyecto2.sales.servicios.tarifas;

import gt.cunoc.proyecto2.sales.dtos.tarifas.ActualizarTarifaReembolsoSolicitud;
import gt.cunoc.proyecto2.sales.dtos.tarifas.NuevaTarifaReembolsoSolicitud;
import gt.cunoc.proyecto2.sales.dtos.tarifas.ReglaReembolsoRespuesta;
import gt.cunoc.proyecto2.sales.dtos.tarifas.TarifaReembolsoRespuesta;
import gt.cunoc.proyecto2.sales.modelos.tarifas.EstadoAlquilerTarifa;
import java.util.List;
import java.util.UUID;

/** Política de reembolso por cancelación de alquiler (tabla tarifas_reembolso, V13). */
public interface TarifaReembolsoServicio {

    List<TarifaReembolsoRespuesta> listar();

    TarifaReembolsoRespuesta crear(NuevaTarifaReembolsoSolicitud solicitud);

    TarifaReembolsoRespuesta actualizar(UUID id, ActualizarTarifaReembolsoSolicitud solicitud);

    /**
     * La tarifa que aplica a una cancelación: la fila activa de horas_minimas_antes más grande que no
     * pase de las horas que faltaban, prefiriendo el estado específico sobre CUALQUIERA. Si ninguna
     * aplica, devuelve la regla por defecto (permite cancelar, 100%).
     */
    ReglaReembolsoRespuesta aplicable(EstadoAlquilerTarifa estado, long horasAntes);
}
