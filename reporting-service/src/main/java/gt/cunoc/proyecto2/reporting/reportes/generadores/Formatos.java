package gt.cunoc.proyecto2.reporting.reportes.generadores;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Formato común de los reportes (títulos, fechas y montos del PDF). */
final class Formatos {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ZONA_GUATEMALA = ZoneId.of("America/Guatemala");

    private Formatos() {
    }

    static String texto(Object valor) {
        return valor != null ? valor.toString() : "todos";
    }

    static String fecha(LocalDate fecha) {
        return fecha != null ? fecha.toString() : "-";
    }

    /** Fecha y hora en la zona de Guatemala (el instante del evento llega normalizado a UTC). */
    static String fechaHora(OffsetDateTime fecha) {
        return fecha != null
                ? FECHA_HORA.format(fecha.atZoneSameInstant(ZONA_GUATEMALA))
                : null;
    }

    static String ruta(String origen, String destino) {
        return (origen != null ? origen : "-") + " → " + (destino != null ? destino : "-");
    }

    static String quetzales(BigDecimal monto) {
        return "Q " + (monto != null ? monto.toPlainString() : "0.00");
    }
}
