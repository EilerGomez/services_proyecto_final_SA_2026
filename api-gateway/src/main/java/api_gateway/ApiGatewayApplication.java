package api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de arranque del gateway reactivo.
 * Spring Boot configura el servidor HTTP, WebFlux, Spring Security y Spring Cloud Gateway;
 * las solicitudes se atienden mediante rutas declaradas y filtros, no con controladores propios.
 */
@SpringBootApplication
public class ApiGatewayApplication {

	// Inicia Spring Boot y pone en marcha el servidor reactivo del gateway.
	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
