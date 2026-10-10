package gt.cunoc.proyecto2.sales.utilidades;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class Fechas {

    public static final ZoneId ZONA_GUATEMALA = ZoneId.of("America/Guatemala");

    private Fechas() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA_GUATEMALA);
    }

    public static OffsetDateTime ahora() {
        return OffsetDateTime.now(ZONA_GUATEMALA);
    }

    public static OffsetDateTime enGuatemala(OffsetDateTime instante) {
        return instante == null ? null : instante.atZoneSameInstant(ZONA_GUATEMALA).toOffsetDateTime();
    }
}