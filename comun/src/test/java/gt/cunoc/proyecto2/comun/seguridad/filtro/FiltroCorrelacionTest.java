package gt.cunoc.proyecto2.comun.seguridad.filtro;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class FiltroCorrelacionTest {

    private final FiltroCorrelacion filtro = new FiltroCorrelacion();
    private final AtomicReference<Optional<UUID>> durante = new AtomicReference<>();
    private final FilterChain cadena = (req, res) -> durante.set(ContextoCorrelacion.actual());

    @Test
    void usaElIdQueTraeLaPeticion() throws Exception {
        UUID id = UUID.randomUUID();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(ContextoCorrelacion.HEADER, id.toString());
        MockHttpServletResponse res = new MockHttpServletResponse();

        filtro.doFilter(req, res, cadena);

        assertThat(durante.get()).contains(id);
        assertThat(res.getHeader(ContextoCorrelacion.HEADER)).isEqualTo(id.toString());
        assertThat(ContextoCorrelacion.actual()).isEmpty();
    }

    @Test
    void sinHeaderGeneraUnId() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest(), res, cadena);

        assertThat(durante.get()).isPresent();
        assertThat(res.getHeader(ContextoCorrelacion.HEADER)).isEqualTo(durante.get().get().toString());
    }

    @Test
    void conHeaderInvalidoGeneraUnIdNuevo() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(ContextoCorrelacion.HEADER, "no-es-un-uuid");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filtro.doFilter(req, res, cadena);

        assertThat(durante.get()).isPresent();
        assertThat(res.getHeader(ContextoCorrelacion.HEADER)).isNotEqualTo("no-es-un-uuid");
    }
}
