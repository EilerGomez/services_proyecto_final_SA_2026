package gt.cunoc.proyecto2.notification.configuracion;

import gt.cunoc.proyecto2.comun.cliente.ClientesInternos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/*
 * Cliente REST hacia Identity (R4): administradores de una sucursal y contacto de un usuario.
 * El token de servicio, la correlación y la espera de 3 s los agrega ClientesInternos.
 */
@Configuration
public class ConfiguracionClientes {

    public static final String CLIENTE_IDENTITY = "clienteRestIdentity";

    @Bean(CLIENTE_IDENTITY)
    public RestClient clienteRestIdentity(ClientesInternos clientes,
                                          @Value("${app.servicios.identity-url:http://localhost:8081}") String url) {
        return clientes.crear(url);
    }
}
