package gt.cunoc.proyecto2.trips.servicios.rutas;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.rutas.ActualizarRutaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.rutas.NuevaRutaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.rutas.RutaRespuesta;
import gt.cunoc.proyecto2.trips.errores.rutas.ErrorRuta;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.sucursales.VistaSucursalEntidad;
import gt.cunoc.proyecto2.trips.repositorios.rutas.RutaRepositorio;
import gt.cunoc.proyecto2.trips.servicios.sucursales.VistaSucursalServicio;
import gt.cunoc.proyecto2.trips.utilidades.VerificadorVersion;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RutaServicioImpl implements RutaServicio {

    private static final Comparator<RutaRespuesta> POR_ORIGEN_Y_DESTINO = Comparator
            .comparing(RutaRespuesta::origenNombre, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(RutaRespuesta::destinoNombre, Comparator.nullsLast(Comparator.naturalOrder()));

    private final RutaRepositorio repositorio;
    private final VistaSucursalServicio sucursales;

    public RutaServicioImpl(RutaRepositorio repositorio, VistaSucursalServicio sucursales) {
        this.repositorio = repositorio;
        this.sucursales = sucursales;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RutaRespuesta> listar(UUID sucursalOrigenId, Boolean activa) {
        List<RutaEntidad> rutas = repositorio.buscar(sucursalOrigenId, activa);
        Map<UUID, String> nombres = sucursales.nombres(rutas.stream()
                .flatMap(r -> Stream.of(r.getSucursalOrigenId(), r.getSucursalDestinoId()))
                .distinct()
                .toList());
        return rutas.stream()
                .map(ruta -> new RutaRespuesta(ruta, nombres))
                .sorted(POR_ORIGEN_Y_DESTINO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RutaRespuesta obtener(UUID id) {
        return respuesta(obtenerPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public RutaEntidad obtenerPorId(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ErrorRuta.RUTA_NO_EXISTE.excepcion());
    }

    @Override
    @Transactional
    public RutaRespuesta crear(NuevaRutaSolicitud solicitud, UsuarioActual usuario) {
        usuario.verificarAccesoSucursal(solicitud.sucursalOrigenId());
        RutaEntidad nueva = solicitud.crearEntidad(usuario.usuarioId());          // 422 MISMA_SUCURSAL
        VistaSucursalEntidad origen = sucursales.verificarActiva(solicitud.sucursalOrigenId());
        VistaSucursalEntidad destino = sucursales.verificarActiva(solicitud.sucursalDestinoId());
        if (repositorio.existsBySucursalOrigenIdAndSucursalDestinoId(origen.getId(), destino.getId())) {
            throw ErrorRuta.RUTA_DUPLICADA.excepcion(origen.getNombre(), destino.getNombre());
        }
        RutaEntidad ruta = repositorio.saveAndFlush(nueva);
        // Paso 6: aquí se publica el evento RutaCreada (businessDate = hoy)
        return respuesta(ruta);
    }

    @Override
    @Transactional
    public RutaRespuesta actualizar(UUID id, ActualizarRutaSolicitud solicitud, UsuarioActual usuario) {
        RutaEntidad ruta = obtenerPorId(id);
        usuario.verificarAccesoSucursal(ruta.getSucursalOrigenId());
        VerificadorVersion.verificar(ruta.getVersion(), solicitud.version());
        if (ruta.seReactiva(solicitud.activa())) {
            sucursales.verificarActiva(ruta.getSucursalOrigenId());
            sucursales.verificarActiva(ruta.getSucursalDestinoId());
        }
        solicitud.aplicarA(ruta);
        ruta = repositorio.saveAndFlush(ruta);
        // Paso 6: aquí se publica el evento RutaActualizada (businessDate = hoy)
        return respuesta(ruta);
    }

    @Override
    @Transactional
    public void eliminar(UUID id, UsuarioActual usuario) {
        RutaEntidad ruta = obtenerPorId(id);
        usuario.verificarAccesoSucursal(ruta.getSucursalOrigenId());
        // Paso 3 (viajes): si la ruta tiene viajes → throw ErrorRuta.RUTA_CON_VIAJES.excepcion()
        repositorio.delete(ruta);
        repositorio.flush();
        // Paso 6: aquí se publica el evento RutaEliminada (businessDate = hoy)
    }

    private RutaRespuesta respuesta(RutaEntidad ruta) {
        return new RutaRespuesta(ruta,
                sucursales.nombres(List.of(ruta.getSucursalOrigenId(), ruta.getSucursalDestinoId())));
    }
}