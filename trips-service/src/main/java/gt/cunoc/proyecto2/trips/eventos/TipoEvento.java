package gt.cunoc.proyecto2.trips.eventos;

/**
 * Nombres de los eventos (son la routing key en RabbitMQ)
 */
public enum TipoEvento {

    // ----- Publica Trips (exchange trips.eventos) -----
    RUTA_CREADA("RutaCreada"),
    RUTA_ACTUALIZADA("RutaActualizada"),
    RUTA_ELIMINADA("RutaEliminada"),
    VIAJE_PROGRAMADO("ViajeProgramado"),
    VIAJE_ACTUALIZADO("ViajeActualizado"),
    VIAJE_ELIMINADO("ViajeEliminado"),
    VIAJE_INICIADO("ViajeIniciado"),
    VIAJE_COMPLETADO("ViajeCompletado"),
    COTIZACION_ALQUILER_CONFIRMADA("CotizacionAlquilerConfirmada"),

    // ----- Consume Trips (cola trips.q) -----
    SUCURSAL_CREADA("SucursalCreada"),
    SUCURSAL_ACTUALIZADA("SucursalActualizada"),
    BUS_CREADO("BusCreado"),
    BUS_ACTUALIZADO("BusActualizado"),
    BUS_DESACTIVADO("BusDesactivado"),
    CHOFER_CREADO("ChoferCreado"),
    CHOFER_ACTUALIZADO("ChoferActualizado"),
    CHOFER_DESACTIVADO("ChoferDesactivado"),
    TARIFA_DEPRECIACION_CAMBIADA("TarifaDepreciacionCambiada"),
    ALQUILER_PAGADO("AlquilerPagado");

    private final String nombre;

    TipoEvento(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}