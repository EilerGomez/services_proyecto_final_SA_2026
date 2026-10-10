package gt.cunoc.proyecto2.reporting.servicios;

import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.ChoferEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.SucursalEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.identity.TarifaDepreciacionCambiada;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.maintenance.GastoTallerRegistrado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.AlquilerPagado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.BoletoVendido;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.VentaCancelada;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.RutaEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeCompletado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeEvento;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Proyección local: aplica los eventos de RabbitMQ a las tablas de lectura.
 * Reglas del spec: upserts con descarte de versiones viejas (`version <=`), ingresos y gastos
 * idempotentes por evento_origen_id, y excepción para reintentar cuando un evento llega antes
 * que su predecesor (ViajeIniciado/ViajeCompletado sin viaje, VentaCancelada sin venta).
 */
public interface ProyeccionReportingServicio {

    void aplicarTarifa(TarifaDepreciacionCambiada evento);

    void aplicarSucursal(SucursalEvento evento, Long versionAgregado);

    void aplicarBus(BusEvento evento, Long versionAgregado);

    void desactivarBus(UUID busId, Long versionAgregado);

    void aplicarChofer(ChoferEvento evento, Long versionAgregado);

    void desactivarChofer(UUID choferId, Long versionAgregado);

    void aplicarRuta(RutaEvento evento);

    void eliminarRuta(UUID rutaId);

    void aplicarViaje(ViajeEvento evento, Long versionAgregado);

    void aplicarViajeActualizado(ViajeEvento evento, Long versionAgregado);

    void eliminarViaje(UUID viajeId);

    void marcarViajeEnRuta(UUID viajeId, UUID busId, Long versionAgregado);

    void aplicarViajeCompletado(UUID eventoId, ViajeCompletado evento, Long versionAgregado,
                                LocalDate fechaNegocio);

    void aplicarBoletoVendido(UUID eventoId, BoletoVendido evento, LocalDate fechaNegocio);

    void aplicarVentaCancelada(UUID eventoId, VentaCancelada evento, LocalDate fechaNegocio);

    void aplicarAlquilerPagado(UUID eventoId, AlquilerPagado evento, LocalDate fechaNegocio);

    void aplicarGastoTaller(UUID eventoId, GastoTallerRegistrado evento, LocalDate fechaNegocio);

    void aplicarCategoriaActualizada(UUID categoriaId, String nombre);

    /** Chofer del viaje EN_RUTA del bus o, si no hay, del próximo PROGRAMADO (NULL si ninguno). */
    void recalcularChoferActual(UUID busId);
}
