package gt.cunoc.proyecto2.trips.repositorios.viajes;

import static gt.cunoc.proyecto2.trips.DatosPrueba.FECHA;
import static gt.cunoc.proyecto2.trips.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.trips.dtos.viajes.FiltroViajes;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

/**
 * Sin base de datos: la Criteria API se simula con mocks y se verifica qué condiciones se agregan.
 * Que la consulta funcione de verdad lo cubren las pruebas de integración.
 */
@SuppressWarnings("unchecked")
class EspecificacionesViajeTest {

    private final Root<ViajeEntidad> raiz = mock(Root.class);
    private final CriteriaQuery<?> consulta = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    private final Path<OffsetDateTime> salida = mock(Path.class);

    @BeforeEach
    void preparar() {
        when(raiz.<OffsetDateTime>get("fechaSalidaProgramada")).thenReturn(salida);
    }

    private Path<Object> campo(String nombre) {
        Path<Object> camino = mock(Path.class);
        when(raiz.<Object>get(nombre)).thenReturn(camino);
        return camino;
    }

    @Test
    void sinFiltrosSoloLimitaElRangoAmplioDeFechas() {
        Specification<ViajeEntidad> especificacion = EspecificacionesViaje.filtrar(
                new FiltroViajes(null, null, null, null, null, null, null));

        assertThat(especificacion).isNotNull();
        especificacion.toPredicate(raiz, consulta, cb);

        verify(cb).greaterThanOrEqualTo(salida, OffsetDateTime.parse("2000-01-01T00:00:00-06:00"));
        verify(cb).lessThan(salida, OffsetDateTime.parse("2101-01-01T00:00:00-06:00"));
        verify(cb, never()).equal(any(Expression.class), any(Object.class));
    }

    @Test
    void conTodosLosFiltrosAgregaUnaCondicionPorCada() {
        UUID rutaId = UUID.randomUUID();
        UUID choferId = UUID.randomUUID();
        Path<Object> ruta = campo("rutaId");
        Path<Object> sucursal = campo("sucursalId");
        Path<Object> chofer = campo("choferId");
        Path<Object> estado = campo("estado");
        Path<Object> tipo = campo("tipo");

        EspecificacionesViaje.filtrar(new FiltroViajes(rutaId, XELA, choferId, EstadoViaje.PROGRAMADO,
                TipoViaje.REGULAR, FECHA, FECHA.plusDays(2))).toPredicate(raiz, consulta, cb);

        verify(cb).greaterThanOrEqualTo(salida, OffsetDateTime.parse("2026-10-20T00:00:00-06:00"));
        verify(cb).lessThan(salida, OffsetDateTime.parse("2026-10-23T00:00:00-06:00"));
        verify(cb).equal(ruta, rutaId);
        verify(cb).equal(sucursal, XELA);
        verify(cb).equal(chofer, choferId);
        verify(cb).equal(estado, EstadoViaje.PROGRAMADO);
        verify(cb).equal(tipo, TipoViaje.REGULAR);
    }

    @Test
    void soloAgregaLosFiltrosQueVienen() {
        UUID choferId = UUID.randomUUID();
        Path<Object> chofer = campo("choferId");

        EspecificacionesViaje.filtrar(FiltroViajes.delChofer(choferId, null, FECHA, null))
                .toPredicate(raiz, consulta, cb);

        verify(cb).greaterThanOrEqualTo(salida, OffsetDateTime.parse("2026-10-20T00:00:00-06:00"));
        verify(cb).lessThan(salida, OffsetDateTime.parse("2101-01-01T00:00:00-06:00"));
        verify(cb).equal(chofer, choferId);
        verify(cb, times(1)).equal(any(Expression.class), any(Object.class));
        verify(raiz, never()).get("rutaId");
        verify(raiz, never()).get("estado");
    }
}
