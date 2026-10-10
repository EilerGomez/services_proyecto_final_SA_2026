package gt.cunoc.proyecto2.trips.repositorios.alquileres;

import static gt.cunoc.proyecto2.trips.DatosPrueba.FECHA;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.trips.dtos.alquileres.FiltroAlquileres;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

/** Sin base de datos: la Criteria API se simula con mocks y se verifica qué condiciones se agregan. */
@SuppressWarnings("unchecked")
class EspecificacionesAlquilerTest {

    private final Root<AlquilerPrivadoEntidad> raiz = mock(Root.class);
    private final CriteriaQuery<?> consulta = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    private final Path<OffsetDateTime> salida = mock(Path.class);

    @BeforeEach
    void preparar() {
        when(raiz.<OffsetDateTime>get("fechaSalida")).thenReturn(salida);
    }

    private Path<Object> campo(String nombre) {
        Path<Object> camino = mock(Path.class);
        when(raiz.<Object>get(nombre)).thenReturn(camino);
        return camino;
    }

    @Test
    void sinFiltrosSoloLimitaElRangoAmplioDeFechas() {
        Specification<AlquilerPrivadoEntidad> especificacion = EspecificacionesAlquiler.filtrar(
                new FiltroAlquileres(null, null, null, null));

        assertThat(especificacion).isNotNull();
        especificacion.toPredicate(raiz, consulta, cb);

        verify(cb).greaterThanOrEqualTo(salida, OffsetDateTime.parse("2000-01-01T00:00:00-06:00"));
        verify(cb).lessThan(salida, OffsetDateTime.parse("2101-01-01T00:00:00-06:00"));
        verify(cb, never()).equal(any(Expression.class), any(Object.class));
    }

    @Test
    void conSucursalEstadoYFechas() {
        Path<Object> sucursal = campo("sucursalId");
        Path<Object> estado = campo("estado");

        EspecificacionesAlquiler.filtrar(new FiltroAlquileres(XELA, EstadoAlquiler.PAGADO, FECHA, FECHA))
                .toPredicate(raiz, consulta, cb);

        verify(cb).greaterThanOrEqualTo(salida, OffsetDateTime.parse("2026-10-20T00:00:00-06:00"));
        verify(cb).lessThan(salida, OffsetDateTime.parse("2026-10-21T00:00:00-06:00"));
        verify(cb).equal(sucursal, XELA);
        verify(cb).equal(estado, EstadoAlquiler.PAGADO);
    }

    @Test
    void soloElEstado() {
        Path<Object> estado = campo("estado");

        EspecificacionesAlquiler.filtrar(new FiltroAlquileres(null, EstadoAlquiler.SOLICITADO, null, null))
                .toPredicate(raiz, consulta, cb);

        verify(cb).equal(estado, EstadoAlquiler.SOLICITADO);
        verify(cb, times(1)).equal(any(Expression.class), any(Object.class));
        verify(raiz, never()).get("sucursalId");
    }
}
