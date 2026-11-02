package api_gateway.filter;

import api_gateway.constants.ApiPaths;
import api_gateway.exception.ErrorResponseWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filtro encargado de impedir el acceso externo a las rutas internas
 *
 * Identifica y bloquea las solicitudes dirigidas a estos endpoints,
 * respondiendo con HTTP 404 antes de que continúen hacia los filtros
 *
 * La detección de rutas considera la normalización de sus segmentos,
 * incluyendo la decodificación de caracteres, la comparación sin distinción de mayúsculas
 * y la resolución de segmentos relativos (".", "..") y barras duplicadas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class BloqueoInternoFilter implements WebFilter {

    private final ErrorResponseWriter writer;

    // Guarda el componente que construye las respuestas HTTP uniformes.
    public BloqueoInternoFilter(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    // Responde 404 a rutas internas y deja continuar cualquier otra solicitud.
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (esRutaInterna(exchange.getRequest().getPath().pathWithinApplication())) {
            return writer.write(exchange, HttpStatus.NOT_FOUND);
        }
        return chain.filter(exchange);
    }

    // Convierte la ruta en segmentos comparables y detecta si es ruta interna
    static boolean esRutaInterna(PathContainer ruta) {
        List<String> segmentos = new ArrayList<>();
        for (PathContainer.Element elemento : ruta.elements()) {
            if (elemento instanceof PathContainer.PathSegment segmento) {
                for (String parte : segmento.valueToMatch().split("/")) {
                    agregar(segmentos, parte.toLowerCase(Locale.ROOT));
                }
            }
        }
        return segmentos.size() > 1 && ApiPaths.INTERNO.equals(segmentos.get(1));
    }

    // Ignora segmentos vacíos y puntos, y aplica la navegación segura de "..".
    private static void agregar(List<String> segmentos, String segmento) {
        if (segmento.isEmpty() || ".".equals(segmento)) {
            return;
        }
        if ("..".equals(segmento)) {
            if (!segmentos.isEmpty()) {
                segmentos.remove(segmentos.size() - 1);
            }
            return;
        }
        segmentos.add(segmento);
    }
}
