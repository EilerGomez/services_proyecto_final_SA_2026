package gt.cunoc.proyecto2.comun.seguridad.filtro;

import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Toma el header X-Correlation-Id (o genera uno), lo deja en el contexto y en los logs, y lo devuelve. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroCorrelacion extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        UUID id = leer(request.getHeader(ContextoCorrelacion.HEADER));
        ContextoCorrelacion.establecer(id);
        response.setHeader(ContextoCorrelacion.HEADER, id.toString());
        try {
            chain.doFilter(request, response);
        } finally {
            ContextoCorrelacion.limpiar();
        }
    }

    private static UUID leer(String valor) {
        if (valor != null) {
            try {
                return UUID.fromString(valor.trim());
            } catch (IllegalArgumentException ignorado) {
                // valor inválido: se genera uno nuevo
            }
        }
        return UUID.randomUUID();
    }
}
