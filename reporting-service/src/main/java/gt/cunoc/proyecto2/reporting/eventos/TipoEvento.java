package gt.cunoc.proyecto2.reporting.eventos;

/**
 * Nombres de los eventos que alimentan el modelo de lectura de reportes.
 *
 * El valor de cada constante es la routing key con la que viaja el mensaje y debe coincidir con
 * las suscripciones declaradas en el módulo común, si no coincide, el evento nunca llega.
 */
public enum TipoEvento {

    // Identity
    TARIFA_DEPRECIACION_CAMBIADA("TarifaDepreciacionCambiada"),

    // Fleet
    SUCURSAL_CREADA("SucursalCreada"),
    SUCURSAL_ACTUALIZADA("SucursalActualizada"),
    BUS_CREADO("BusCreado"),
    BUS_ACTUALIZADO("BusActualizado"),
    BUS_DESACTIVADO("BusDesactivado"),
    CHOFER_CREADO("ChoferCreado"),
    CHOFER_ACTUALIZADO("ChoferActualizado"),
    CHOFER_DESACTIVADO("ChoferDesactivado"),

    // Trips
    RUTA_CREADA("RutaCreada"),
    RUTA_ACTUALIZADA("RutaActualizada"),
    RUTA_ELIMINADA("RutaEliminada"),
    VIAJE_PROGRAMADO("ViajeProgramado"),
    VIAJE_ACTUALIZADO("ViajeActualizado"),
    VIAJE_ELIMINADO("ViajeEliminado"),
    VIAJE_INICIADO("ViajeIniciado"),
    VIAJE_COMPLETADO("ViajeCompletado"),

    // Sales
    BOLETO_VENDIDO("BoletoVendido"),
    VENTA_CANCELADA("VentaCancelada"),
    ALQUILER_PAGADO("AlquilerPagado"),

    // Maintenance
    GASTO_TALLER_REGISTRADO("GastoTallerRegistrado"),
    CATEGORIA_GASTO_ACTUALIZADA("CategoriaGastoActualizada");

    private final String nombre;

    TipoEvento(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
