package gt.cunoc.proyecto2.sales.servicios.tarifas;

import gt.cunoc.proyecto2.sales.dtos.tarifas.ActualizarTarifaReembolsoSolicitud;
import gt.cunoc.proyecto2.sales.dtos.tarifas.NuevaTarifaReembolsoSolicitud;
import gt.cunoc.proyecto2.sales.dtos.tarifas.ReglaReembolsoRespuesta;
import gt.cunoc.proyecto2.sales.dtos.tarifas.TarifaReembolsoRespuesta;
import gt.cunoc.proyecto2.sales.errores.tarifas.ErrorTarifaReembolso;
import gt.cunoc.proyecto2.sales.eventos.PublicadorEventosSales;
import gt.cunoc.proyecto2.sales.modelos.tarifas.EstadoAlquilerTarifa;
import gt.cunoc.proyecto2.sales.modelos.tarifas.TarifaReembolsoEntidad;
import gt.cunoc.proyecto2.sales.repositorios.tarifas.TarifaReembolsoRepositorio;
import gt.cunoc.proyecto2.sales.utilidades.VerificadorVersion;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarifaReembolsoServicioImpl implements TarifaReembolsoServicio {

    /**
     * Gana el tramo más alto que la cancelación alcanza; si dos tramos empatan, el del estado
     * específico antes que el comodín (una regla escrita para ASIGNADO manda sobre una de CUALQUIERA).
     */
    private static final Comparator<TarifaReembolsoEntidad> MAS_ESPECIFICA =
            Comparator.comparingInt(TarifaReembolsoEntidad::getHorasMinimasAntes).reversed()
                    .thenComparing(Comparator.comparing(
                            (TarifaReembolsoEntidad tarifa) -> tarifa.getEstadoAlquiler().esComodin()));

    private final TarifaReembolsoRepositorio repositorio;
    private final PublicadorEventosSales eventos;

    public TarifaReembolsoServicioImpl(TarifaReembolsoRepositorio repositorio, PublicadorEventosSales eventos) {
        this.repositorio = repositorio;
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TarifaReembolsoRespuesta> listar() {
        return repositorio.findAllByOrderByEstadoAlquilerAscHorasMinimasAntesDesc().stream()
                .map(TarifaReembolsoRespuesta::new)
                .toList();
    }

    @Override
    @Transactional
    public TarifaReembolsoRespuesta crear(NuevaTarifaReembolsoSolicitud solicitud) {
        repositorio.findByEstadoAlquilerAndHorasMinimasAntes(solicitud.estadoAlquiler(),
                solicitud.horasMinimasAntes()).ifPresent(existente -> {
                    throw ErrorTarifaReembolso.TRAMO_DUPLICADO.excepcion(solicitud.estadoAlquiler(),
                            solicitud.horasMinimasAntes());
                });
        TarifaReembolsoEntidad tarifa = repositorio.saveAndFlush(TarifaReembolsoEntidad.crear(
                solicitud.estadoAlquiler(), solicitud.horasMinimasAntes(), solicitud.permiteCancelacion(),
                solicitud.porcentajeReembolso(), solicitud.justificacion()));
        eventos.tarifaReembolsoCambiada(tarifa);           // Trips actualiza su copia de solo lectura
        return new TarifaReembolsoRespuesta(tarifa);
    }

    @Override
    @Transactional
    public TarifaReembolsoRespuesta actualizar(UUID id, ActualizarTarifaReembolsoSolicitud solicitud) {
        TarifaReembolsoEntidad tarifa = repositorio.findById(id)
                .orElseThrow(() -> ErrorTarifaReembolso.TARIFA_NO_EXISTE.excepcion());
        VerificadorVersion.verificar(tarifa.getVersion(), solicitud.version());
        tarifa.editar(solicitud.permiteCancelacion(), solicitud.porcentajeReembolso(), solicitud.justificacion());
        if (solicitud.activa()) {
            tarifa.reactivar();
        } else {
            tarifa.desactivar();
        }
        TarifaReembolsoEntidad guardada = repositorio.saveAndFlush(tarifa);
        eventos.tarifaReembolsoCambiada(guardada);
        return new TarifaReembolsoRespuesta(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public ReglaReembolsoRespuesta aplicable(EstadoAlquilerTarifa estado, long horasAntes) {
        EstadoAlquilerTarifa buscado = estado == null ? EstadoAlquilerTarifa.CUALQUIERA : estado;
        long horas = Math.max(horasAntes, 0);              // el viaje ya salió: aplica el tramo de 0 horas
        return repositorio.findByActivaTrueAndEstadoAlquilerIn(
                        List.of(buscado, EstadoAlquilerTarifa.CUALQUIERA)).stream()
                .filter(tarifa -> tarifa.cubre(buscado, horas))
                .min(MAS_ESPECIFICA)
                .map(ReglaReembolsoRespuesta::de)
                .orElseGet(ReglaReembolsoRespuesta::porDefecto);
    }
}
