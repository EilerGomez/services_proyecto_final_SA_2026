package gt.cunoc.proyecto2.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del servicio de reportes.
 *
 * <p>Levanta la aplicación y escanea también el paquete compartido {@code comun}, del que toma la
 * seguridad, el manejo de errores y la mensajería con la que se alimenta el modelo de lectura.
 */
@SpringBootApplication(scanBasePackages = {
        "gt.cunoc.proyecto2.reporting",
        "gt.cunoc.proyecto2.comun"
})
public class ReportingServiceApplication {

    // Arranca Spring Boot: base de datos de reportes, consumidores de eventos y servidor HTTP.
    public static void main(String[] args) {
        SpringApplication.run(ReportingServiceApplication.class, args);
    }
}
