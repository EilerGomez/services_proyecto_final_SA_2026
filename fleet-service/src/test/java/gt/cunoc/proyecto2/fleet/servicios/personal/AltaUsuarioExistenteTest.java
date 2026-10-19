package gt.cunoc.proyecto2.fleet.servicios.personal;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.clientes.identity.ClienteIdentity;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.UsuarioExistenteSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.identity.CuentaPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.identity.EstadoCuentaRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.identity.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.fleet.dtos.identity.VinculoCuentaPersonal;
import gt.cunoc.proyecto2.fleet.dtos.personal.NuevoPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.PersonalSucursalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.personal.ReintegrarPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.fleet.errores.personal.ErrorPersonal;
import gt.cunoc.proyecto2.fleet.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.fleet.eventos.PublicadorEventosFleet;
import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.personal.PersonalSucursalRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.sucursales.SucursalRepositorio;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Alta de personal con una cuenta que ya existe (R5b), en F7 y para el admin inicial de F3. */
@ExtendWith(MockitoExtension.class)
class AltaUsuarioExistenteTest {

    @Mock private PersonalSucursalRepositorio repositorio;
    @Mock private SucursalRepositorio sucursales;
    @Mock private ClienteIdentity identity;
    @Mock private PublicadorEventosFleet eventos;

    private PersonalSucursalServicioImpl servicio;
    private SucursalEntidad xela;
    private UsuarioActual adminXela;

    @BeforeEach
    void preparar() {
        servicio = new PersonalSucursalServicioImpl(repositorio, sucursales, identity, eventos);
        xela = DatosPrueba.sucursal(true);
        adminXela = DatosPrueba.adminSucursal(xela.getId());
    }

    @Test
    void agregarCajeroExistenteVinculaLaCuentaSinCrearOtra() {
        UUID usuarioId = UUID.randomUUID();
        UUID solicitudId = UUID.randomUUID();
        NuevoPersonalSolicitud solicitud = new NuevoPersonalSolicitud(RolPersonal.CAJERO, FECHA, null,
                new UsuarioExistenteSolicitud(solicitudId, usuarioId));
        when(sucursales.findById(xela.getId())).thenReturn(Optional.of(xela));
        when(identity.vincularCuentaExistente(any(VinculoCuentaPersonal.class)))
                .thenReturn(new CuentaPersonalRespuesta(usuarioId, "ana@fleet.gt", "Ana Lopez", false));
        when(repositorio.saveAndFlush(any(PersonalSucursalEntidad.class))).thenAnswer(i -> i.getArgument(0));

        PersonalSucursalRespuesta respuesta = servicio.agregar(xela.getId(), solicitud, adminXela);

        ArgumentCaptor<VinculoCuentaPersonal> r5b = ArgumentCaptor.forClass(VinculoCuentaPersonal.class);
        verify(identity).vincularCuentaExistente(r5b.capture());
        assertThat(r5b.getValue()).isEqualTo(new VinculoCuentaPersonal(solicitudId, usuarioId, "CAJERO",
                xela.getId(), "Xela Central"));
        assertThat(respuesta.personalId()).isEqualTo(solicitudId);
        assertThat(respuesta.usuarioId()).isEqualTo(usuarioId);
        assertThat(respuesta.nombreCompleto()).isEqualTo("Ana Lopez");
        assertThat(respuesta.correo()).isEqualTo("ana@fleet.gt");
        verify(identity, never()).crearCuentaPersonal(any(SolicitudCuentaPersonal.class));
        verify(eventos).personalAsignado(any(PersonalSucursalEntidad.class), eq("Xela Central"), eq(FECHA));
    }

    @Test
    void sinSolicitudIdSeGeneraUnoNuevo() {
        UUID usuarioId = UUID.randomUUID();
        when(identity.vincularCuentaExistente(any(VinculoCuentaPersonal.class)))
                .thenReturn(new CuentaPersonalRespuesta(usuarioId, "ana@fleet.gt", "Ana Lopez", false));
        when(repositorio.saveAndFlush(any(PersonalSucursalEntidad.class))).thenAnswer(i -> i.getArgument(0));

        PersonalSucursalEntidad admin = servicio.registrarAltaExistente(xela, RolPersonal.ADMIN_SUCURSAL, FECHA,
                new UsuarioExistenteSolicitud(null, usuarioId));

        assertThat(admin.getId()).isNotNull();
        assertThat(admin.getRol()).isEqualTo(RolPersonal.ADMIN_SUCURSAL);
        assertThat(admin.getUsuarioId()).isEqualTo(usuarioId);
    }

    @Test
    void sinCuentaNiUsuarioExistenteEsSolicitudInvalida() {
        NuevoPersonalSolicitud solicitud = new NuevoPersonalSolicitud(RolPersonal.CAJERO, FECHA, null, null);
        when(sucursales.findById(xela.getId())).thenReturn(Optional.of(xela));

        VerificarError.lanza(() -> servicio.agregar(xela.getId(), solicitud, adminXela),
                ErrorPersonal.CUENTA_REQUERIDA);
        verifyNoInteractions(identity, eventos);
    }

    @Test
    void conCuentaNuevaYUsuarioExistenteALaVezEsSolicitudInvalida() {
        NuevoPersonalSolicitud solicitud = new NuevoPersonalSolicitud(RolPersonal.CAJERO, FECHA,
                DatosPrueba.cuenta(null, null), new UsuarioExistenteSolicitud(null, UUID.randomUUID()));
        when(sucursales.findById(xela.getId())).thenReturn(Optional.of(xela));

        VerificarError.lanza(() -> servicio.agregar(xela.getId(), solicitud, adminXela),
                ErrorPersonal.CUENTA_REQUERIDA);
        verifyNoInteractions(identity, eventos);
    }

    @Test
    void quienYaTrabajaEnOtraSucursalNoSeAgrega() {
        UUID usuarioId = UUID.randomUUID();
        when(repositorio.existsByUsuarioIdAndActivoTrue(usuarioId)).thenReturn(true);

        VerificarError.lanza(() -> servicio.registrarAltaExistente(xela, RolPersonal.CAJERO, FECHA,
                new UsuarioExistenteSolicitud(null, usuarioId)), ErrorPersonal.PERSONAL_EN_OTRA_SUCURSAL);
        verifyNoInteractions(identity, eventos);
    }

    @Test
    void noSeDaDeAltaEnUnaSucursalInactiva() {
        SucursalEntidad inactiva = DatosPrueba.sucursal(false);

        VerificarError.lanza(() -> servicio.registrarAltaExistente(inactiva, RolPersonal.CAJERO, FECHA,
                new UsuarioExistenteSolicitud(null, UUID.randomUUID())), ErrorSucursal.SUCURSAL_INACTIVA);
        verifyNoInteractions(identity);
    }

    @Test
    void unReintentoDevuelveLaMismaFilaSinLlamarAIdentity() {
        UUID solicitudId = UUID.randomUUID();
        PersonalSucursalEntidad existente = DatosPrueba.personal(solicitudId, xela.getId(), RolPersonal.CAJERO);
        when(repositorio.findById(solicitudId)).thenReturn(Optional.of(existente));

        PersonalSucursalEntidad resultado = servicio.registrarAltaExistente(xela, RolPersonal.CAJERO, FECHA,
                new UsuarioExistenteSolicitud(solicitudId, existente.getUsuarioId()));

        assertThat(resultado).isSameAs(existente);
        verifyNoInteractions(identity, eventos);
    }

    @Test
    void unSolicitudIdUsadoEnOtraSucursal() {
        UUID solicitudId = UUID.randomUUID();
        PersonalSucursalEntidad deOtra = DatosPrueba.personal(solicitudId, UUID.randomUUID(), RolPersonal.CAJERO);
        when(repositorio.findById(solicitudId)).thenReturn(Optional.of(deOtra));

        VerificarError.lanza(() -> servicio.registrarAltaExistente(xela, RolPersonal.CAJERO, FECHA,
                new UsuarioExistenteSolicitud(solicitudId, UUID.randomUUID())), ErrorGeneral.SOLICITUD_YA_USADA);
    }

    @Test
    void unReintentoConElMismoSolicitudIdPeroOtraCuentaSeRechaza() {
        UUID solicitudId = UUID.randomUUID();
        PersonalSucursalEntidad existente = DatosPrueba.personal(solicitudId, xela.getId(), RolPersonal.CAJERO);
        when(repositorio.findById(solicitudId)).thenReturn(Optional.of(existente));

        VerificarError.lanza(() -> servicio.registrarAltaExistente(xela, RolPersonal.CAJERO, FECHA,
                new UsuarioExistenteSolicitud(solicitudId, UUID.randomUUID())), ErrorGeneral.SOLICITUD_YA_USADA);
        verifyNoInteractions(identity, eventos);
    }

    @Test
    void noSeReintegraAQuienSeVolvioAdminDelSistema() {
        PersonalSucursalEntidad cajero = DatosPrueba.personal(xela.getId(), RolPersonal.CAJERO);
        cajero.desactivar(FECHA);
        when(repositorio.findById(cajero.getId())).thenReturn(Optional.of(cajero));
        when(sucursales.findById(xela.getId())).thenReturn(Optional.of(xela));
        when(identity.estadoCuenta(cajero.getUsuarioId())).thenReturn(new EstadoCuentaRespuesta(
                cajero.getUsuarioId(), "ACTIVO", null,
                "La cuenta es de un administrador del sistema; no puede ser personal de sucursal"));

        assertThatThrownBy(() -> servicio.reintegrar(cajero.getId(),
                new ReintegrarPersonalSolicitud(xela.getId(), RolPersonal.CAJERO, FECHA.plusDays(1)),
                DatosPrueba.adminSistema()))
                .hasMessage("La cuenta es de un administrador del sistema; no puede ser personal de sucursal");
        verify(repositorio, never()).saveAndFlush(any(PersonalSucursalEntidad.class));
        verifyNoInteractions(eventos);
    }
}
