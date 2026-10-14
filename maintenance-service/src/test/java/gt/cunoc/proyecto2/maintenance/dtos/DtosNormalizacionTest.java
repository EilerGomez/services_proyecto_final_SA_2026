package gt.cunoc.proyecto2.maintenance.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.maintenance.dtos.buses.BusDisponibleRespuesta;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.ActualizarCategoriaSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.CategoriaRespuesta;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.NuevaCategoriaSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.LineaServicioRespuesta;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.LineaServicioSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.NuevoServicioSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.ServicioRespuesta;
import gt.cunoc.proyecto2.maintenance.modelos.CategoriaEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.DetalleServicioEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.ServicioTallerEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.VistaBusEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Ramas de normalización de los DTOs (null / blanco / con valor). */
class DtosNormalizacionTest {

    @Test
    void nuevaCategoriaNormaliza() {
        assertThat(new NuevaCategoriaSolicitud("  Frenos  ", "  Desc  ").nombre()).isEqualTo("Frenos");
        assertThat(new NuevaCategoriaSolicitud("X", null).descripcion()).isNull();
        assertThat(new NuevaCategoriaSolicitud("X", "   ").descripcion()).isNull();
        assertThat(new NuevaCategoriaSolicitud(null, null).nombre()).isNull();
        var creada = new NuevaCategoriaSolicitud("Motor", "D").createEntity();
        assertThat(creada.getNombre()).isEqualTo("Motor");
        assertThat(creada.isActiva()).isTrue();
    }

    @Test
    void actualizarCategoriaNormaliza() {
        assertThat(new ActualizarCategoriaSolicitud(" Y ", "", true).nombre()).isEqualTo("Y");
        assertThat(new ActualizarCategoriaSolicitud("Y", "", true).descripcion()).isNull();
        assertThat(new ActualizarCategoriaSolicitud(null, null, null).nombre()).isNull();
    }

    @Test
    void nuevoServicioNormaliza() {
        var linea = new LineaServicioSolicitud(UUID.randomUUID(), null, "REPUESTO",
                new BigDecimal("10.00"));
        assertThat(linea.descripcion()).isNull();
        assertThat(new LineaServicioSolicitud(UUID.randomUUID(), "  X  ", "MANO_OBRA",
                new BigDecimal("10.00")).descripcion()).isEqualTo("X");

        var solicitud = new NuevoServicioSolicitud(UUID.randomUUID(), LocalDate.now(),
                "  Taller  ", "   ", List.of(linea));
        assertThat(solicitud.taller()).isEqualTo("Taller");
        assertThat(solicitud.observaciones()).isNull();
        var vacia = new NuevoServicioSolicitud(UUID.randomUUID(), LocalDate.now(),
                null, null, List.of(linea));
        assertThat(vacia.taller()).isNull();
        assertThat(vacia.observaciones()).isNull();
    }

    @Test
    void respuestasMapeanNulos() {
        CategoriaEntidad categoria = new CategoriaEntidad();
        categoria.setId(UUID.randomUUID());
        categoria.setNombre("Llantas");
        assertThat(new CategoriaRespuesta(categoria).descripcion()).isNull();

        DetalleServicioEntidad sinCategoria = new DetalleServicioEntidad();
        sinCategoria.setId(UUID.randomUUID());
        sinCategoria.setDescripcion("X");
        sinCategoria.setTipo("REPUESTO");
        sinCategoria.setMonto(BigDecimal.ONE);
        assertThat(new LineaServicioRespuesta(sinCategoria).categoriaNombre()).isNull();

        DetalleServicioEntidad conCategoria = new DetalleServicioEntidad();
        conCategoria.setId(UUID.randomUUID());
        conCategoria.setCategoria(categoria);
        conCategoria.setDescripcion("Y");
        conCategoria.setTipo("MANO_OBRA");
        conCategoria.setMonto(BigDecimal.TEN);
        assertThat(new LineaServicioRespuesta(conCategoria).categoriaNombre()).isEqualTo("Llantas");

        ServicioTallerEntidad servicio = new ServicioTallerEntidad();
        servicio.setId(UUID.randomUUID());
        assertThat(new ServicioRespuesta(servicio, "-", List.of()).lineas()).isEmpty();

        VistaBusEntidad bus = new VistaBusEntidad();
        bus.setBusId(UUID.randomUUID());
        bus.setPlaca("C1");
        bus.setSucursalId(UUID.randomUUID());
        assertThat(new BusDisponibleRespuesta(bus).activo()).isTrue();
    }
}
