package gt.cunoc.proyecto2.trips.utilidades;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class Fechas {

    public static final ZoneId ZONA_GUATEMALA = ZoneId.of("America/Guatemala");

    private static final LocalDate PRIMER_DIA = LocalDate.of(2000, 1, 1);
    private static final LocalDate ULTIMO_DIA = LocalDate.of(2100, 12, 31);

    private Fechas() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA_GUATEMALA);
    }

    public static OffsetDateTime ahora() {
        return OffsetDateTime.now(ZONA_GUATEMALA);
    }

    public static OffsetDateTime inicioDelDia(LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : PRIMER_DIA;
        return dia.atStartOfDay(ZONA_GUATEMALA).toOffsetDateTime();
    }

    public static OffsetDateTime finDelDia(LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : ULTIMO_DIA;
        return dia.plusDays(1).atStartOfDay(ZONA_GUATEMALA).toOffsetDateTime();
    }

    public static LocalDate diaEnGuatemala(OffsetDateTime instante) {
        return instante.atZoneSameInstant(ZONA_GUATEMALA).toLocalDate();
    }

    public static OffsetDateTime enGuatemala(OffsetDateTime instante) {
        return instante == null ? null : instante.atZoneSameInstant(ZONA_GUATEMALA).toOffsetDateTime();
    }
}