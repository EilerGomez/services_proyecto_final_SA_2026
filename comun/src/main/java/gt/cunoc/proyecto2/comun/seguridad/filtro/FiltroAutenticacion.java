package gt.cunoc.proyecto2.comun.seguridad.filtro;

import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.errores.EscritorErrores;
import gt.cunoc.proyecto2.comun.errores.NoAutenticadoException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/*
 * Si la petición trae "Authorization: Bearer, valida el JWT y deja el {@link UsuarioActual}
 * como atributo de la petición. Token inválido manda un 401 inmediato.
 * Sin token la petición sigue como anónima: los endpoints protegidos lo rechazan con
 * {RequiereRol} o al pedir un parámetro {UsuarioActual}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FiltroAutenticacion extends OncePerRequestFilter {

    public static final String ATRIBUTO_USUARIO = "comun.usuarioActual";
    private static final String PREFIJO = "Bearer ";

    private final ServicioJwt jwt;
    private final EscritorErrores errores;

    public FiltroAutenticacion(ServicioJwt jwt, EscritorErrores errores) {
        this.jwt = jwt;
        this.errores = errores;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String autorizacion = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (autorizacion != null && autorizacion.startsWith(PREFIJO)) {
            try {
                request.setAttribute(ATRIBUTO_USUARIO, jwt.validar(autorizacion.substring(PREFIJO.length()).trim()));
            } catch (NoAutenticadoException e) {
                errores.escribir(request, response, e.getStatus(), e.getCodigo(), e.getMessage());
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
