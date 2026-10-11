package gt.cunoc.proyecto2.sales.repositorios.alquileres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.dtos.alquileres.FiltroOrdenes;
import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

/** Sin base de datos: la Criteria API se simula con mocks y se verifica qué condiciones se agregan. */
@SuppressWarnings("unchecked")
class EspecificacionesOrdenTest {

    private final Root<OrdenAlquilerEntidad> raiz = mock(Root.class);
    private final CriteriaQuery<?> consulta = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);

    private Path<Object> campo(String nombre) {
        Path<Object> camino = mock(Path.class);
        when(raiz.<Object>get(nombre)).thenReturn(camino);
        return camino;
    }

    @Test
    void sinFiltrosNoAgregaCondiciones() {
        Specification<OrdenAlquilerEntidad> especificacion = EspecificacionesOrden.filtrar(
                new FiltroOrdenes(null, null));

        assertThat(especificacion).isNotNull();
        especificacion.toPredicate(raiz, consulta, cb);

        verify(cb).conjunction();
        verify(cb, never()).equal(any(Expression.class), any(Object.class));
    }

    @Test
    void conSucursalYEstado() {
        Path<Object> sucursal = campo("sucursalId");
        Path<Object> estado = campo("estado");

        EspecificacionesOrden.filtrar(new FiltroOrdenes(DatosPrueba.XELA, EstadoOrdenAlquiler.RECHAZADA))
                .toPredicate(raiz, consulta, cb);

        verify(cb).conjunction();
        verify(cb).equal(sucursal, DatosPrueba.XELA);
        verify(cb).equal(estado, EstadoOrdenAlquiler.RECHAZADA);
    }

    @Test
    void soloLaSucursal() {
        Path<Object> sucursal = campo("sucursalId");

        EspecificacionesOrden.filtrar(new FiltroOrdenes(DatosPrueba.GUATE, null)).toPredicate(raiz, consulta, cb);

        verify(cb).equal(sucursal, DatosPrueba.GUATE);
        verify(cb, times(1)).equal(any(Expression.class), any(Object.class));
        verify(raiz, never()).get("estado");
    }

    @Test
    void soloElEstado() {
        Path<Object> estado = campo("estado");

        EspecificacionesOrden.filtrar(new FiltroOrdenes(null, EstadoOrdenAlquiler.PAGADA))
                .toPredicate(raiz, consulta, cb);

        verify(cb).equal(estado, EstadoOrdenAlquiler.PAGADA);
        verify(cb, times(1)).equal(any(Expression.class), any(Object.class));
        verify(raiz, never()).get("sucursalId");
    }
}
