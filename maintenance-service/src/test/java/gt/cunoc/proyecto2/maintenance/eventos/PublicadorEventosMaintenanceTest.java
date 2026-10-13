package gt.cunoc.proyecto2.maintenance.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.maintenance.eventos.publicados.CategoriaGastoActualizada;
import gt.cunoc.proyecto2.maintenance.eventos.publicados.GastoTallerRegistrado;
import gt.cunoc.proyecto2.maintenance.modelos.CategoriaEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.DetalleServicioEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.ServicioTallerEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicadorEventosMaintenanceTest {

    @Mock
    PublicadorEventos publicador;

    @InjectMocks
    PublicadorEventosMaintenance eventos;

    private static LocalDate hoyEnGuatemala() {
        return LocalDate.now(ZoneId.of("America/Guatemala"));
    }

    /** Servicio como queda tras saveAndFlush + refresh: con los totales que puso el trigger. */
    private static ServicioTallerEntidad servicioRefrescado() {
        ServicioTallerEntidad servicio = new ServicioTallerEntidad();
        servicio.setId(UUID.randomUUID());
        servicio.setBusId(UUID.randomUUID());
        servicio.setSucursalId(UUID.randomUUID());
        servicio.setFechaServicio(LocalDate.of(2026, 10, 22));
        servicio.setTaller("Taller Hermanos Pérez");
        servicio.setTotalManoObra(new BigDecimal("400.00"));
        servicio.setTotalRepuestos(new BigDecimal("6800.00"));
        servicio.setTotal(new BigDecimal("7200.00"));
        CategoriaEntidad categoria = new CategoriaEntidad();
        categoria.setId(UUID.randomUUID());
        categoria.setNombre("Llantas");
        servicio.agregarLinea(linea(categoria, "4 llantas", "REPUESTO", "6800.00"));
        servicio.agregarLinea(linea(categoria, "Montaje", "MANO_OBRA", "400.00"));
        return servicio;
    }

    private static DetalleServicioEntidad linea(CategoriaEntidad categoria, String descripcion, String tipo,
                                                String monto) {
        DetalleServicioEntidad linea = new DetalleServicioEntidad();
        linea.setId(UUID.randomUUID());
        linea.setCategoriaId(categoria.getId());
        linea.setCategoria(categoria);
        linea.setDescripcion(descripcion);
        linea.setTipo(tipo);
        linea.setMonto(new BigDecimal(monto));
        return linea;
    }

    @Test
    void gastoRegistradoPublicaConFechaServicio() {
        ServicioTallerEntidad servicio = servicioRefrescado();

        eventos.gastoRegistrado(servicio);

        var captor = ArgumentCaptor.forClass(GastoTallerRegistrado.class);
        verify(publicador).publicar(eq("ServicioTaller"), eq(servicio.getId()),
                eq("GastoTallerRegistrado"), captor.capture(), eq(LocalDate.of(2026, 10, 22)));
        assertThat(captor.getValue().lineas()).hasSize(2);
        assertThat(captor.getValue().lineas().get(0).categoriaNombre()).isEqualTo("Llantas");
        assertThat(TipoEvento.GASTO_TALLER_REGISTRADO.nombre()).isEqualTo("GastoTallerRegistrado");
        assertThat(TipoAgregado.SERVICIO_TALLER.nombre()).isEqualTo("ServicioTaller");
    }

    @Test
    void gastoRegistradoLlevaTallerYTotalesDelCatalogo() {
        ServicioTallerEntidad servicio = servicioRefrescado();

        eventos.gastoRegistrado(servicio);

        var captor = ArgumentCaptor.forClass(GastoTallerRegistrado.class);
        verify(publicador).publicar(eq("ServicioTaller"), eq(servicio.getId()),
                eq("GastoTallerRegistrado"), captor.capture(), eq(servicio.getFechaServicio()));
        GastoTallerRegistrado payload = captor.getValue();
        assertThat(payload.servicioId()).isEqualTo(servicio.getId());
        assertThat(payload.busId()).isEqualTo(servicio.getBusId());
        assertThat(payload.sucursalId()).isEqualTo(servicio.getSucursalId());
        assertThat(payload.fechaServicio()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(payload.taller()).isEqualTo("Taller Hermanos Pérez");
        assertThat(payload.totalManoObra()).isEqualByComparingTo("400.00");
        assertThat(payload.totalRepuestos()).isEqualByComparingTo("6800.00");
        assertThat(payload.total()).isEqualByComparingTo("7200.00");
        assertThat(payload.lineas()).extracting(GastoTallerRegistrado.Linea::tipo)
                .containsExactly("REPUESTO", "MANO_OBRA");
    }

    @Test
    void gastoRegistradoSinTallerLoMandaNull() {
        ServicioTallerEntidad servicio = servicioRefrescado();
        servicio.setTaller(null);

        assertThat(GastoTallerRegistrado.desde(servicio).taller()).isNull();
        assertThat(GastoTallerRegistrado.desde(servicio).total()).isEqualByComparingTo("7200.00");
    }

    @Test
    void categoriaActualizadaPublicaConHoyDeGuatemala() {
        CategoriaEntidad categoria = new CategoriaEntidad();
        categoria.setId(UUID.randomUUID());
        categoria.setNombre("Motor");
        categoria.setActiva(false);

        eventos.categoriaActualizada(categoria);

        var captor = ArgumentCaptor.forClass(CategoriaGastoActualizada.class);
        verify(publicador).publicar(eq("CategoriaGasto"), eq(categoria.getId()),
                eq("CategoriaGastoActualizada"), captor.capture(), eq(hoyEnGuatemala()));
        assertThat(captor.getValue().nombre()).isEqualTo("Motor");
        assertThat(PublicadorEventosMaintenance.ZONA_GUATEMALA).isEqualTo(ZoneId.of("America/Guatemala"));
        assertThat(TipoEvento.CATEGORIA_GASTO_ACTUALIZADA.nombre())
                .isEqualTo("CategoriaGastoActualizada");
        assertThat(TipoAgregado.CATEGORIA_GASTO.nombre()).isEqualTo("CategoriaGasto");
        assertThat(TipoEvento.BUS_CREADO.nombre()).isEqualTo("BusCreado");
        assertThat(TipoEvento.BUS_DESACTIVADO.nombre()).isEqualTo("BusDesactivado");
        assertThat(captor.getValue()).isEqualTo(new CategoriaGastoActualizada(
                categoria.getId(), "Motor", false));
    }
}
