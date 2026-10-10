package gt.cunoc.proyecto2.reporting.reportes.generadores;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Formato común de los reportes (títulos, fechas y montos del PDF). */
final class Formatos {

    private Formatos() {
    }

    static String texto(Object valor) {
        return valor != null ? valor.toString() : "todos";
    }

    static String fecha(LocalDate fecha) {
        return fecha != null ? fecha.toString() : "-";
    }

    static String ruta(String origen, String destino) {
        return (origen != null ? origen : "-") + " → " + (destino != null ? destino : "-");
    }

    static String quetzales(BigDecimal monto) {
        return "Q " + (monto != null ? monto.toPlainString() : "0.00");
    }
}
