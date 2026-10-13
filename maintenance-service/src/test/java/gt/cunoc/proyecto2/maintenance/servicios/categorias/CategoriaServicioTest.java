package gt.cunoc.proyecto2.maintenance.servicios.categorias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.ActualizarCategoriaSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.NuevaCategoriaSolicitud;
import gt.cunoc.proyecto2.maintenance.eventos.PublicadorEventosMaintenance;
import gt.cunoc.proyecto2.maintenance.modelos.CategoriaEntidad;
import gt.cunoc.proyecto2.maintenance.repositorios.CategoriaRepositorio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoriaServicioTest {

    @Mock
    CategoriaRepositorio categorias;
    @Mock
    PublicadorEventosMaintenance eventos;

    @InjectMocks
    CategoriaServicioImpl servicio;

    private CategoriaEntidad categoria(UUID id, String nombre, boolean activa) {
        CategoriaEntidad categoria = new CategoriaEntidad();
        categoria.setId(id);
        categoria.setNombre(nombre);
        categoria.setDescripcion("Desc");
        categoria.setActiva(activa);
        return categoria;
    }

    @Test
    void listarFiltraPorActiva() {
        when(categorias.findAllByOrderByNombreAsc()).thenReturn(
                List.of(categoria(UUID.randomUUID(), "A", true),
                        categoria(UUID.randomUUID(), "B", false)));

        assertThat(servicio.listar(null)).hasSize(2);
        assertThat(servicio.listar(true)).hasSize(1);
        assertThat(servicio.listar(false)).hasSize(1);
    }

    @Test
    void obtenerDevuelveLaCategoria() {
        UUID id = UUID.randomUUID();
        when(categorias.findById(id)).thenReturn(Optional.of(categoria(id, "Motor", true)));

        assertThat(servicio.obtener(id).nombre()).isEqualTo("Motor");
    }

    @Test
    void obtenerInexistenteEs404() {
        UUID id = UUID.randomUUID();
        when(categorias.findById(id)).thenReturn(Optional.empty());

        var lanzada = catchThrowable(() -> servicio.obtener(id));
        assertThat(lanzada).isInstanceOf(NoEncontradoException.class);
        assertThat(((gt.cunoc.proyecto2.comun.errores.NegocioException) lanzada).getCodigo())
                .isEqualTo("CATEGORIA_NO_EXISTE");
    }

    @Test
    void crearGuardaYPublica() {
        when(categorias.existsByNombreIgnoreCase("Frenos")).thenReturn(false);

        var respuesta = servicio.crear(new NuevaCategoriaSolicitud("Frenos", null));

        assertThat(respuesta.nombre()).isEqualTo("Frenos");
        assertThat(respuesta.activa()).isTrue();
        verify(categorias).save(any(CategoriaEntidad.class));
        verify(eventos).categoriaActualizada(any(CategoriaEntidad.class));
    }

    @Test
    void crearDuplicadaEs409() {
        when(categorias.existsByNombreIgnoreCase("Motor")).thenReturn(true);

        var lanzada = catchThrowable(() -> servicio.crear(new NuevaCategoriaSolicitud("Motor", null)));
        assertThat(lanzada).isInstanceOf(ConflictoException.class);
        assertThat(((gt.cunoc.proyecto2.comun.errores.NegocioException) lanzada).getCodigo())
                .isEqualTo("CATEGORIA_DUPLICADA");
    }

    @Test
    void actualizarRenombraYPublica() {
        UUID id = UUID.randomUUID();
        when(categorias.findById(id)).thenReturn(Optional.of(categoria(id, "Vieja", true)));

        var respuesta = servicio.actualizar(id,
                new ActualizarCategoriaSolicitud("Nueva", "D", false));

        assertThat(respuesta.nombre()).isEqualTo("Nueva");
        assertThat(respuesta.activa()).isFalse();
        verify(eventos).categoriaActualizada(any(CategoriaEntidad.class));
    }

    @Test
    void actualizarConMismoNombreNoRevisaDuplicado() {
        UUID id = UUID.randomUUID();
        when(categorias.findById(id)).thenReturn(Optional.of(categoria(id, "Motor", true)));

        var respuesta = servicio.actualizar(id,
                new ActualizarCategoriaSolicitud("Motor", null, null));

        assertThat(respuesta.activa()).isTrue();
    }

    @Test
    void actualizarADuplicadoEs409() {
        UUID id = UUID.randomUUID();
        when(categorias.findById(id)).thenReturn(Optional.of(categoria(id, "Vieja", true)));
        when(categorias.existsByNombreIgnoreCase("Motor")).thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizar(id,
                new ActualizarCategoriaSolicitud("Motor", null, null)))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void actualizarInexistenteEs404() {
        UUID id = UUID.randomUUID();
        when(categorias.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(id,
                new ActualizarCategoriaSolicitud("X", null, null)))
                .isInstanceOf(NoEncontradoException.class);
    }
}
