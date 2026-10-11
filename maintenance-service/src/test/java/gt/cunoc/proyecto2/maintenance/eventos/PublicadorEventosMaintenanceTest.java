package gt.cunoc.proyecto2.maintenance.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void gastoRegistradoPublicaConFechaServicio() {
        ServicioTallerEntidad servicio = new ServicioTallerEntidad();
        servicio.setId(UUID.randomUUID());
        servicio.setBusId(UUID.randomUUID());
        servicio.setSucursalId(UUID.randomUUID());
        servicio.setFechaServicio(LocalDate.of(2026, 10, 22));
        CategoriaEntidad categoria = new CategoriaEntidad();
        categoria.setId(UUID.randomUUID());
        categoria.setNombre("Llantas");
        DetalleServicioEntidad linea = new DetalleServicioEntidad();
        linea.setId(UUID.randomUUID());
        linea.setCategoriaId(categoria.getId());
        linea.setCategoria(categoria);
        linea.setDescripcion("4 llantas");
        linea.setTipo("REPUESTO");
        linea.setMonto(new BigDecimal("6800.00"));
        servicio.agregarLinea(linea);

        eventos.gastoRegistrado(servicio);

        var captor = ArgumentCaptor.forClass(GastoTallerRegistrado.class);
        verify(publicador).publicar(eq("ServicioTaller"), eq(servicio.getId()),
                eq("GastoTallerRegistrado"), captor.capture(), eq(LocalDate.of(2026, 10, 22)));
        assertThat(captor.getValue().lineas()).hasSize(1);
        assertThat(captor.getValue().lineas().get(0).categoriaNombre()).isEqualTo("Llantas");
        assertThat(TipoEvento.GASTO_TALLER_REGISTRADO.nombre()).isEqualTo("GastoTallerRegistrado");
        assertThat(TipoAgregado.SERVICIO_TALLER.nombre()).isEqualTo("ServicioTaller");
    }

    @Test
    void categoriaActualizadaPublicaConHoy() {
        CategoriaEntidad categoria = new CategoriaEntidad();
        categoria.setId(UUID.randomUUID());
        categoria.setNombre("Motor");
        categoria.setActiva(false);

        eventos.categoriaActualizada(categoria);

        var captor = ArgumentCaptor.forClass(CategoriaGastoActualizada.class);
        verify(publicador).publicar(eq("CategoriaGasto"), eq(categoria.getId()),
                eq("CategoriaGastoActualizada"), captor.capture(), eq(LocalDate.now()));
        assertThat(captor.getValue().nombre()).isEqualTo("Motor");
        assertThat(TipoEvento.CATEGORIA_GASTO_ACTUALIZADA.nombre())
                .isEqualTo("CategoriaGastoActualizada");
        assertThat(TipoAgregado.CATEGORIA_GASTO.nombre()).isEqualTo("CategoriaGasto");
        assertThat(TipoEvento.BUS_CREADO.nombre()).isEqualTo("BusCreado");
        assertThat(TipoEvento.BUS_DESACTIVADO.nombre()).isEqualTo("BusDesactivado");
        assertThat(captor.getValue()).isEqualTo(new CategoriaGastoActualizada(
                categoria.getId(), "Motor", false));
    }
}
