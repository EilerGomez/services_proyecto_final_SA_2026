package gt.cunoc.proyecto2.reporting.reportes;

/**
 * Los reportes del spec: RG1–RG6 globales, RS1–RS6 por sucursal, más la vista agrupada de RS6
 * (los mismos gastos de taller sumados por categoría de cada bus).
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
    /** RS6 agrupado: por cada bus, el subtotal de cada categoría y el total del bus. */
    TALLER_BUS_CATEGORIA
}
