package gt.cunoc.proyecto2.sales.repositorios.ventas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.dtos.ventas.FiltroVentas;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

/**
 * Sin base de datos: la Criteria API se simula con mocks y se verifica qué condiciones se agregan.
 * Que la consulta funcione de verdad lo cubren las pruebas de integración.
 */
@SuppressWarnings("unchecked")
class EspecificacionesVentaTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 30);

    private final Root<VentaEntidad> raiz = mock(Root.class);
    private final CriteriaQuery<?> consulta = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    private final Path<LocalDate> fechaPago = mock(Path.class);

    @BeforeEach
    void preparar() {
        when(raiz.<LocalDate>get("fechaPago")).thenReturn(fechaPago);
    }

    private Path<Object> campo(String nombre) {
        Path<Object> camino = mock(Path.class);
        when(raiz.<Object>get(nombre)).thenReturn(camino);
        return camino;
    }

    @Test
    void sinFiltrosNoAgregaCondiciones() {
        Specification<VentaEntidad> especificacion = EspecificacionesVenta.filtrar(
                new FiltroVentas(null, null, null, null, null, null));

        assertThat(especificacion).isNotNull();
        especificacion.toPredicate(raiz, consulta, cb);

        verify(cb).conjunction();
        verify(cb, never()).equal(any(Expression.class), any(Object.class));
        verify(raiz, never()).get("fechaPago");
    }

    @Test
    void conTodosLosFiltrosAgregaUnaCondicionPorCada() {
        UUID viajeId = UUID.randomUUID();
        Path<Object> viaje = campo("viajeId");
        Path<Object> sucursal = campo("sucursalId");
        Path<Object> estado = campo("estado");
        Path<Object> canal = campo("canal");

        EspecificacionesVenta.filtrar(new FiltroVentas(viajeId, DatosPrueba.XELA, EstadoVenta.CONFIRMADA,
                CanalVenta.PRESENCIAL, DESDE, HASTA)).toPredicate(raiz, consulta, cb);

        verify(cb).conjunction();
        verify(cb).equal(viaje, viajeId);
        verify(cb).equal(sucursal, DatosPrueba.XELA);
        verify(cb).equal(estado, EstadoVenta.CONFIRMADA);
        verify(cb).equal(canal, CanalVenta.PRESENCIAL);
        verify(cb).greaterThanOrEqualTo(fechaPago, DESDE);
        verify(cb).lessThanOrEqualTo(fechaPago, HASTA);
    }

    @Test
    void soloAgregaLosFiltrosQueVienen() {
        Path<Object> estado = campo("estado");

        EspecificacionesVenta.filtrar(new FiltroVentas(null, null, EstadoVenta.PENDIENTE_PAGO, null, null, null))
                .toPredicate(raiz, consulta, cb);

        verify(cb).equal(estado, EstadoVenta.PENDIENTE_PAGO);
        verify(cb, times(1)).equal(any(Expression.class), any(Object.class));
        verify(raiz, never()).get("viajeId");
        verify(raiz, never()).get("sucursalId");
        verify(raiz, never()).get("canal");
        verify(raiz, never()).get("fechaPago");
    }

    @Test
    void soloDesdeAgregaElLimiteInferior() {
        EspecificacionesVenta.filtrar(new FiltroVentas(null, null, null, null, DESDE, null))
                .toPredicate(raiz, consulta, cb);

        verify(cb).greaterThanOrEqualTo(fechaPago, DESDE);
        verify(raiz, times(1)).get("fechaPago");
        verify(cb, never()).equal(any(Expression.class), any(Object.class));
    }

    @Test
    void soloHastaAgregaElLimiteSuperior() {
        EspecificacionesVenta.filtrar(new FiltroVentas(null, null, null, null, null, HASTA))
                .toPredicate(raiz, consulta, cb);

        verify(cb).lessThanOrEqualTo(fechaPago, HASTA);
        verify(raiz, times(1)).get("fechaPago");
        verify(cb, never()).equal(any(Expression.class), any(Object.class));
    }
}
