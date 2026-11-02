package gt.cunoc.proyecto2.reporting.reportes.generadores;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utilidades de formato compartidas por los generadores de reportes.
 *
 * <p>Convierte fechas, horas y montos a los textos que se muestran en el PDF y en el JSON. Las horas
 * llegan en UTC y aquí se pasan a la zona horaria de Guatemala.
 */
final class Formatos {

    // Formato de fecha y hora usado en los reportes.
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ZONA_GUATEMALA = ZoneId.of("America/Guatemala");

    // Clase de utilidades: no se instancia.
    private Formatos() {
    }

    // Texto de un filtro; si viene vacío se muestra "todos".
    static String texto(Object valor) {
        return valor != null ? valor.toString() : "todos";
    }

    // Fecha en formato ISO; un valor nulo se muestra como guion.
    static String fecha(LocalDate fecha) {
        return fecha != null ? fecha.toString() : "-";
    }

    /** Fecha y hora en la zona de Guatemala (el instante del evento llega normalizado a UTC). */
    static String fechaHora(OffsetDateTime fecha) {
        return fecha != null
                ? FECHA_HORA.format(fecha.atZoneSameInstant(ZONA_GUATEMALA))
                : null;
    }

    // Une origen y destino con una flecha; los valores nulos se muestran como guion.
    static String ruta(String origen, String destino) {
        return (origen != null ? origen : "-") + " → " + (destino != null ? destino : "-");
    }

    // Monto con símbolo de quetzal y sin notación científica.
    static String quetzales(BigDecimal monto) {
        return "Q " + (monto != null ? monto.toPlainString() : "0.00");
    }
}
