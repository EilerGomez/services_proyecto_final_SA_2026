/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.comun.cliente;

/**
 *
 * @author eiler
 */

import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClientesInternos {

    public static final Duration TIEMPO_ESPERA = Duration.ofSeconds(3);

    private final ServicioJwt jwt;
    private final String nombreServicio;

    @Autowired
    public ClientesInternos(ServicioJwt jwt,
                            @Value("${spring.application.name:servicio-desconocido}") String nombreServicio) {
        this.jwt = jwt;
        this.nombreServicio = nombreServicio;
    }

    /** Cliente con la URL base del otro servicio (p. ej. {@code http://trips-service:8083}) y espera de 3 s. */
    public RestClient crear(String urlBase) {
        return crear(urlBase, TIEMPO_ESPERA);
    }

    public RestClient crear(String urlBase, Duration tiempoEspera) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(tiempoEspera)
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(tiempoEspera);
        return RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabrica)
                .requestInterceptor(this::agregarCabeceras)
                .build();
    }

    ClientHttpResponse agregarCabeceras(HttpRequest peticion, byte[] cuerpo, ClientHttpRequestExecution ejecucion)
            throws IOException {
        peticion.getHeaders().setBearerAuth(jwt.generarServicio(nombreServicio));
        peticion.getHeaders().set(ContextoCorrelacion.HEADER, ContextoCorrelacion.actualOGenerar().toString());
        return ejecucion.execute(peticion, cuerpo);
    }
}