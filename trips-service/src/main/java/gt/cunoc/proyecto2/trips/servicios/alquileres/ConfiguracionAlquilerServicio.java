package gt.cunoc.proyecto2.trips.servicios.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.alquileres.NuevaConfiguracionAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.modelos.alquileres.ConfiguracionAlquilerEntidad;
import java.time.LocalDate;
import java.util.List;

public interface ConfiguracionAlquilerServicio {

    List<ConfiguracionAlquilerEntidad> listar();

    ConfiguracionAlquilerEntidad registrar(NuevaConfiguracionAlquilerSolicitud solicitud, UsuarioActual usuario);

    ConfiguracionAlquilerEntidad vigenteEn(LocalDate fecha);
}