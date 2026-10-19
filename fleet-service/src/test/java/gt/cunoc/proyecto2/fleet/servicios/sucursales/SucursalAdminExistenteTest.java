package gt.cunoc.proyecto2.fleet.servicios.sucursales;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.UsuarioExistenteSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.NuevaSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.SucursalCreadaRespuesta;
import gt.cunoc.proyecto2.fleet.errores.personal.ErrorPersonal;
import gt.cunoc.proyecto2.fleet.eventos.PublicadorEventosFleet;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.buses.BusRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.sucursales.SucursalRepositorio;
import gt.cunoc.proyecto2.fleet.servicios.personal.PersonalSucursalServicio;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** F3 con un admin inicial que ya tiene cuenta (R5b). */
@ExtendWith(MockitoExtension.class)
class SucursalAdminExistenteTest {

    @Mock private SucursalRepositorio repositorio;
    @Mock private PersonalSucursalServicio personal;
    @Mock private BusRepositorio buses;
    @Mock private PublicadorEventosFleet eventos;

    private SucursalServicioImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new SucursalServicioImpl(repositorio, personal, buses, eventos);
    }

    @Test
    void crearConAdminExistenteLoAsignaSinCrearCuenta() {
        UsuarioExistenteSolicitud existente = new UsuarioExistenteSolicitud(UUID.randomUUID(), UUID.randomUUID());
        when(repositorio.saveAndFlush(any(SucursalEntidad.class))).thenAnswer(i -> i.getArgument(0));
        when(personal.registrarAltaExistente(any(SucursalEntidad.class), eq(RolPersonal.ADMIN_SUCURSAL), eq(FECHA),
                eq(existente)))
                .thenAnswer(i -> DatosPrueba.personal(((SucursalEntidad) i.getArgument(0)).getId(),
                        RolPersonal.ADMIN_SUCURSAL));

        SucursalCreadaRespuesta respuesta = servicio.crear(solicitud(null, existente));

        assertThat(respuesta.adminInicial().rol()).isEqualTo(RolPersonal.ADMIN_SUCURSAL);
        assertThat(respuesta.adminInicial().sucursalId()).isEqualTo(respuesta.sucursal().sucursalId());
        verify(personal, never()).registrarAlta(any(), any(), any(), any());
    }

    @Test
    void sinAdminInicialNoSeGuardaNada() {
        VerificarError.lanza(() -> servicio.crear(solicitud(null, null)), ErrorPersonal.CUENTA_REQUERIDA);
        verifyNoInteractions(repositorio, personal, eventos);
    }

    @Test
    void conLasDosFormasDeAdminNoSeGuardaNada() {
        VerificarError.lanza(() -> servicio.crear(solicitud(DatosPrueba.cuenta(null, null),
                new UsuarioExistenteSolicitud(null, UUID.randomUUID()))), ErrorPersonal.CUENTA_REQUERIDA);
        verifyNoInteractions(repositorio, personal, eventos);
    }

    private static NuevaSucursalSolicitud solicitud(DatosCuentaSolicitud nueva, UsuarioExistenteSolicitud existente) {
        return new NuevaSucursalSolicitud("XELA", "Xela Central", "Quetzaltenango", "Quetzaltenango",
                "4a calle", null, new BigDecimal("14.83"), new BigDecimal("-91.51"), FECHA, nueva, existente);
    }
}
