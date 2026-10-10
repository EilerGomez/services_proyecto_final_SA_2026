package gt.cunoc.proyecto2.reporting.reportes;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Component;

/**
 * Compila las plantillas .jrxml una sola vez y las reutiliza.
 * Mismo patrón que el servicio de referencia (cinema-reports-service).
 */
@Component
public class CompiladorJasper {

    private final Map<String, JasperReport> cache = new ConcurrentHashMap<>();

    public JasperReport obtener(String nombre) throws JRException {
        return cache.computeIfAbsent(nombre, clave -> {
            try {
                InputStream jrxml = getClass().getResourceAsStream("/reports/" + clave + ".jrxml");
                if (jrxml == null) {
                    throw new IllegalArgumentException("Plantilla no encontrada: " + clave + ".jrxml");
                }
                return JasperCompileManager.compileReport(jrxml);
            } catch (JRException e) {
                throw new IllegalStateException("Error compilando reporte: " + clave, e);
            }
        });
    }
}
