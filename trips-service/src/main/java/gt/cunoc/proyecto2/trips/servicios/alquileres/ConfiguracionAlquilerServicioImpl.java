package gt.cunoc.proyecto2.trips.servicios.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.alquileres.NuevaConfiguracionAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.errores.alquileres.ErrorConfiguracionAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.ConfiguracionAlquilerEntidad;
import gt.cunoc.proyecto2.trips.repositorios.alquileres.ConfiguracionAlquilerRepositorio;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracionAlquilerServicioImpl implements ConfiguracionAlquilerServicio {

    private final ConfiguracionAlquilerRepositorio repositorio;

    public ConfiguracionAlquilerServicioImpl(ConfiguracionAlquilerRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionAlquilerEntidad> listar() {
        return repositorio.findAllByOrderByVigenteDesdeDesc();
    }

    @Override
    @Transactional
    public ConfiguracionAlquilerEntidad registrar(NuevaConfiguracionAlquilerSolicitud solicitud,
                                                  UsuarioActual usuario) {
        // Dos peticiones al mismo tiempo con la misma fecha: el UNIQUE de la BD responde 409 REGISTRO_DUPLICADO
        if (repositorio.existsByVigenteDesde(solicitud.vigenteDesde())) {
            throw ErrorConfiguracionAlquiler.PRECIO_YA_EXISTE_PARA_FECHA.excepcion(solicitud.vigenteDesde());
        }
        return repositorio.saveAndFlush(solicitud.crearEntidad(usuario.usuarioId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionAlquilerEntidad vigenteEn(LocalDate fecha) {
        return repositorio.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(fecha)
                .orElseThrow(() -> ErrorConfiguracionAlquiler.SIN_PRECIO_ALQUILER.excepcion(fecha));
    }
}