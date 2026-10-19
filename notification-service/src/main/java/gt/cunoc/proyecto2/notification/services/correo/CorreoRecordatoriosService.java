package gt.cunoc.proyecto2.notification.services.correo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/*
 * Arma el correo de recordatorio de vencimiento de la licencia de un chofer (30, 15 y 3 días antes).
 * Lo envía con {@link EnvioCorreoService#enviarPlantilla}, que evita duplicados por evento + destinatario + plantilla.
 */
@Service
public class CorreoRecordatoriosService {

    public static final String LICENCIA_POR_VENCER = "LICENCIA_POR_VENCER";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SIN_DATO = "-";

    private final EnvioCorreoService correos;
    private final JsonMapper json;

    public CorreoRecordatoriosService(EnvioCorreoService correos, JsonMapper json) {
        this.correos = correos;
        this.json = json;
    }

    /*
     * @param recordatorioId id de la fila de recordatorios_licencia: sirve de clave de idempotencia, así
     *                       repetir la corrida del job no vuelve a mandar el mismo aviso.
     */
    public void licenciaPorVencer(UUID recordatorioId, String correo, String nombre, LocalDate fechaVencimiento,
                                  int diasAntes) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("recordatorioId", recordatorioId);
        datos.put("fechaVencimiento", fechaVencimiento == null ? null : fechaVencimiento.toString());
        datos.put("diasAntes", diasAntes);
        String cuerpo = """
                Hola %s,

                Tu licencia de conducir vence el %s (en %d días).

                Renueva la licencia a tiempo para seguir conduciendo.""".formatted(
                nombre, fechaVencimiento == null ? SIN_DATO : FECHA.format(fechaVencimiento), diasAntes);
        correos.enviarPlantilla(recordatorioId, correo, LICENCIA_POR_VENCER,
                "Tu licencia de conducir está por vencer", json.writeValueAsString(datos), cuerpo);
    }
}
