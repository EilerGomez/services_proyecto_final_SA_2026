package gt.cunoc.proyecto2.reporting.reportes;

/** Contrato de cada generador: PDF binario y datos para JSON. */
public interface GeneradorReporte {

    TipoReporte tipo();

    byte[] generar(ParametrosReporte params) throws Exception;

    Object datos(ParametrosReporte params);
}
