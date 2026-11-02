package gt.cunoc.proyecto2.reporting.reportes;

/**
 * Tipos de reporte que puede producir el servicio.
 *
 * <p>Los primeros son globales y los siguientes son por sucursal; el último es la vista agrupada de
 * gastos de taller por bus y categoría.
 */
public enum TipoReporte {

    INGRESOS,
    GASTOS,
    GANANCIAS,
    RUTAS_DEMANDADAS,
    TALLER_POR_CATEGORIA,
    MAPA_RUTAS,
    BUSES,
    CHOFERES,
    INGRESOS_BOLETOS,
    INGRESOS_ALQUILER,
    DEPRECIACION,
    TALLER_POR_BUS,
    TALLER_BUS_CATEGORIA
}
