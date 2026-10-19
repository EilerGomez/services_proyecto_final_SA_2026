package gt.cunoc.proyecto2.fleet.servicios.personal;

import gt.cunoc.proyecto2.fleet.dtos.personal.ReintegrarPersonalSolicitud;
import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.errores.ProhibidoException;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.clientes.identity.ClienteIdentity;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.identity.CuentaPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.identity.EstadoCuentaRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.identity.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.fleet.dtos.personal.DesactivarPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.NuevoPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.PersonalSucursalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.personal.TrasladarPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.TrasladoPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.fleet.errores.personal.ErrorPersonal;
import gt.cunoc.proyecto2.fleet.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.fleet.eventos.PublicadorEventosFleet;
import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.personal.PersonalSucursalRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.sucursales.SucursalRepositorio;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonalSucursalServicioImplTest {

    @Mock
    private PersonalSucursalRepositorio repositorio;
    @Mock
    private SucursalRepositorio sucursales;
    @Mock
    private ClienteIdentity identity;
    @Mock
    private PublicadorEventosFleet eventos;

    private PersonalSucursalServicioImpl servicio;
    private SucursalEntidad xela;
    private UsuarioActual adminSistema;
    private UsuarioActual adminXela;

    @BeforeEach
    void preparar() {
        servicio = new PersonalSucursalServicioImpl(repositorio, sucursales, identity, eventos);
        xela = DatosPrueba.sucursal(true);
        adminSistema = DatosPrueba.adminSistema();
        adminXela = DatosPrueba.adminSucursal(xela.getId());
    }

    private void guardarDevuelveLaMisma() {
        when(repositorio.saveAndFlush(any(PersonalSucursalEntidad.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void identityCreaLaCuenta(UUID usuarioId) {
        when(identity.crearCuentaPersonal(any(SolicitudCuentaPersonal.class)))
                .thenReturn(new CuentaPersonalRespuesta(usuarioId, "ana@fleet.gt", "Ana Lopez", true));
    }

    // ----- F6 -----

    @Test
    void listarPersonalDeOtraSucursalEstaProhibido() {
        UUID otra = UUID.randomUUID();

        assertThatThrownBy(() -> servicio.listar(otra, null, null, adminXela)).isInstanceOf(ProhibidoException.class);
        verifyNoInteractions(repositorio);
    }

    @Test
    void listarDeUnaSucursalQueNoExiste() {
        VerificarError.lanza(() -> servicio.listar(xela.getId(), null, null, adminSistema),
                ErrorSucursal.SUCURSAL_NO_EXISTE);
    }

    @Test
    void listarConFiltros() {
        PersonalSucursalEntidad cajero = DatosPrueba.personal(xela.getId(), RolPersonal.CAJERO);
        when(sucursales.existsById(xela.getId())).thenReturn(true);
        when(repositorio.buscar(xela.getId(), RolPersonal.CAJERO, true)).thenReturn(List.of(cajero));

        assertThat(servicio.listar(xela.getId(), RolPersonal.CAJERO, true, adminXela)).containsExactly(cajero);
    }

    @Test
    void obtenerPersonalQueNoExiste() {
        UUID id = UUID.randomUUID();

        VerificarError.lanza(() -> servicio.obtenerPorId(id), ErrorPersonal.PERSONAL_NO_EXISTE);
    }

    // ----- F7 y alta (también F3) -----

    @Test
    void unAdminDeSucursalSoloAgregaCajeros() {
        NuevoPersonalSolicitud solicitud = new NuevoPersonalSolicitud(RolPersonal.ADMIN_SUCURSAL, FECHA,
                DatosPrueba.cuenta(null, null));

        VerificarError.lanza(() -> servicio.agregar(xela.getId(), solicitud, adminXela), ErrorPersonal.SOLO_CAJEROS);
        verifyNoInteractions(identity);
    }

    @Test
    void agregarEnUnaSucursalQueNoExiste() {
        NuevoPersonalSolicitud solicitud = new NuevoPersonalSolicitud(RolPersonal.CAJERO, FECHA,
                DatosPrueba.cuenta(null, null));

        VerificarError.lanza(() -> servicio.agregar(xela.getId(), solicitud, adminXela),
                ErrorSucursal.SUCURSAL_NO_EXISTE);
    }

    @Test
    void agregarCajeroCreaLaCuentaGuardaYPublica() {
        UUID usuarioId = UUID.randomUUID();
        NuevoPersonalSolicitud solicitud = new NuevoPersonalSolicitud(RolPersonal.CAJERO, FECHA,
                DatosPrueba.cuenta(null, null));
        when(sucursales.findById(xela.getId())).thenReturn(Optional.of(xela));
        identityCreaLaCuenta(usuarioId);
        guardarDevuelveLaMisma();

        PersonalSucursalRespuesta respuesta = servicio.agregar(xela.getId(), solicitud, adminXela);

        ArgumentCaptor<SolicitudCuentaPersonal> r5 = ArgumentCaptor.forClass(SolicitudCuentaPersonal.class);
        verify(identity).crearCuentaPersonal(r5.capture());
        assertThat(r5.getValue().rol()).isEqualTo("CAJERO");
        assertThat(r5.getValue().sucursalId()).isEqualTo(xela.getId());
        assertThat(respuesta.personalId()).isEqualTo(r5.getValue().solicitudId());
        assertThat(respuesta.usuarioId()).isEqualTo(usuarioId);
        assertThat(respuesta.nombreCompleto()).isEqualTo("Ana Lopez");
        assertThat(respuesta.rol()).isEqualTo(RolPersonal.CAJERO);
        assertThat(respuesta.activo()).isTrue();
        assertThat(respuesta.fechaAsignacion()).isEqualTo(FECHA);
        assertThat(r5.getValue().sucursalNombre()).isEqualTo("Xela Central");
        verify(eventos).personalAsignado(any(PersonalSucursalEntidad.class), eq("Xela Central"), eq(FECHA));
    }

    @Test
    void elAltaUsaElSolicitudIdComoIdDeLaFila() {
        UUID solicitudId = UUID.randomUUID();
        identityCreaLaCuenta(UUID.randomUUID());
        guardarDevuelveLaMisma();

        PersonalSucursalEntidad admin = servicio.registrarAlta(xela, RolPersonal.ADMIN_SUCURSAL, FECHA,
                DatosPrueba.cuenta(solicitudId, null));

        assertThat(admin.getId()).isEqualTo(solicitudId);
        assertThat(admin.getRol()).isEqualTo(RolPersonal.ADMIN_SUCURSAL);
    }

    @Test
    void noSeDaDeAltaEnUnaSucursalInactiva() {
        SucursalEntidad inactiva = DatosPrueba.sucursal(false);

        VerificarError.lanza(() -> servicio.registrarAlta(inactiva, RolPersonal.CAJERO, FECHA,
                DatosPrueba.cuenta(null, null)), ErrorSucursal.SUCURSAL_INACTIVA);
        verifyNoInteractions(identity);
    }

    @Test
    void unReintentoDeUnAltaCompletaDevuelveLaMismaFila() {
        UUID solicitudId = UUID.randomUUID();
        PersonalSucursalEntidad existente = DatosPrueba.personal(solicitudId, xela.getId(), RolPersonal.CAJERO);
        when(repositorio.findById(solicitudId)).thenReturn(Optional.of(existente));

        PersonalSucursalEntidad resultado = servicio.registrarAlta(xela, RolPersonal.CAJERO, FECHA,
                DatosPrueba.cuenta(solicitudId, null));

        assertThat(resultado).isSameAs(existente);
        verifyNoInteractions(identity, eventos);
    }

    @Test
    void unSolicitudIdUsadoEnOtraSucursal() {
        UUID solicitudId = UUID.randomUUID();
        PersonalSucursalEntidad deOtra = DatosPrueba.personal(solicitudId, UUID.randomUUID(), RolPersonal.CAJERO);
        when(repositorio.findById(solicitudId)).thenReturn(Optional.of(deOtra));

        VerificarError.lanza(() -> servicio.registrarAlta(xela, RolPersonal.CAJERO, FECHA,
                DatosPrueba.cuenta(solicitudId, null)), ErrorGeneral.SOLICITUD_YA_USADA);
    }

    // ----- F8 -----

    @Test
    void desactivarConVersionVieja() {
        PersonalSucursalEntidad cajero = DatosPrueba.personal(xela.getId(), RolPersonal.CAJERO);
        when(repositorio.findById(cajero.getId())).thenReturn(Optional.of(cajero));

        VerificarError.lanza(() -> servicio.desactivar(cajero.getId(), new DesactivarPersonalSolicitud(FECHA, 7),
                adminXela), ErrorGeneral.MODIFICACION_CONCURRENTE);
    }

    @Test
    void unAdminDeSucursalNoDesactivaAOtroAdmin() {
        PersonalSucursalEntidad admin = DatosPrueba.personal(xela.getId(), RolPersonal.ADMIN_SUCURSAL);
        when(repositorio.findById(admin.getId())).thenReturn(Optional.of(admin));

        VerificarError.lanza(() -> servicio.desactivar(admin.getId(), new DesactivarPersonalSolicitud(FECHA, 0),
                adminXela), ErrorPersonal.SOLO_CAJEROS);
    }

    @Test
    void desactivarAAlguienYaInactivo() {
        PersonalSucursalEntidad cajero = DatosPrueba.personal(xela.getId(), RolPersonal.CAJERO);
        cajero.desactivar(FECHA);
        when(repositorio.findById(cajero.getId())).thenReturn(Optional.of(cajero));

        VerificarError.lanza(() -> servicio.desactivar(cajero.getId(), new DesactivarPersonalSolicitud(FECHA, 0),
                adminXela), ErrorPersonal.PERSONAL_INACTIVO);
    }

    @Test
    void desactivarCajeroNoBloqueaLaSucursal() {
        PersonalSucursalEntidad cajero = DatosPrueba.personal(xela.getId(), RolPersonal.CAJERO);
        when(repositorio.findById(cajero.getId())).thenReturn(Optional.of(cajero));
        guardarDevuelveLaMisma();

        PersonalSucursalRespuesta respuesta = servicio.desactivar(cajero.getId(),
                new DesactivarPersonalSolicitud(FECHA, 0), adminXela);

        assertThat(respuesta.activo()).isFalse();
        assertThat(respuesta.fechaDesasignacion()).isEqualTo(FECHA);
        verify(sucursales, never()).bloquearPorId(any(UUID.class));
        verify(eventos).personalDesactivado(cajero, FECHA);
    }

    @Test
    void noSeDesactivaAlUltimoAdminDeUnaSucursalActiva() {
        PersonalSucursalEntidad admin = DatosPrueba.personal(xela.getId(), RolPersonal.ADMIN_SUCURSAL);
        when(repositorio.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(sucursales.bloquearPorId(xela.getId())).thenReturn(Optional.of(xela));
        when(repositorio.existsBySucursalIdAndRolAndActivoTrueAndIdNot(xela.getId(), RolPersonal.ADMIN_SUCURSAL,
                admin.getId())).thenReturn(false);

        VerificarError.lanza(() -> servicio.desactivar(admin.getId(), new DesactivarPersonalSolicitud(FECHA, 0),
                adminSistema), ErrorPersonal.SUCURSAL_SIN_ADMIN);
        assertThat(admin.isActivo()).isTrue();
    }

    @Test
    void seDesactivaUnAdminSiQuedaOtro() {
        PersonalSucursalEntidad admin = DatosPrueba.personal(xela.getId(), RolPersonal.ADMIN_SUCURSAL);
        when(repositorio.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(sucursales.bloquearPorId(xela.getId())).thenReturn(Optional.of(xela));
        when(repositorio.existsBySucursalIdAndRolAndActivoTrueAndIdNot(xela.getId(), RolPersonal.ADMIN_SUCURSAL,
                admin.getId())).thenReturn(true);
        guardarDevuelveLaMisma();

        PersonalSucursalRespuesta respuesta = servicio.desactivar(admin.getId(),
                new DesactivarPersonalSolicitud(FECHA, 0), adminSistema);

        assertThat(respuesta.activo()).isFalse();
        verify(eventos).personalDesactivado(admin, FECHA);
    }

    @Test
    void desactivarAdminDeUnaSucursalQueYaNoExiste() {
        PersonalSucursalEntidad admin = DatosPrueba.personal(xela.getId(), RolPersonal.ADMIN_SUCURSAL);
        when(repositorio.findById(admin.getId())).thenReturn(Optional.of(admin));

        VerificarError.lanza(() -> servicio.desactivar(admin.getId(), new DesactivarPersonalSolicitud(FECHA, 0),
                adminSistema), ErrorSucursal.SUCURSAL_NO_EXISTE);
    }

    // ----- F10 reintegro -----

    private PersonalSucursalEntidad retirado(RolPersonal rol) {
        PersonalSucursalEntidad p = DatosPrueba.personal(xela.getId(), rol);
        p.desactivar(FECHA);
        when(repositorio.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private void cuentaEnIdentity(PersonalSucursalEntidad p, String estado, Instant bloqueadoHasta) {
        when(identity.estadoCuenta(p.getUsuarioId()))
                .thenReturn(new EstadoCuentaRespuesta(p.getUsuarioId(), estado, bloqueadoHasta));
    }

    private SucursalEntidad sucursalActiva() {
        SucursalEntidad s = DatosPrueba.sucursal(true);
        when(sucursales.findById(s.getId())).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void noSeReintegraConLaCuentaDesactivada() {
        PersonalSucursalEntidad admin = retirado(RolPersonal.ADMIN_SUCURSAL);
        SucursalEntidad guate = sucursalActiva();
        cuentaEnIdentity(admin, "INACTIVO", null);

        VerificarError.lanza(() -> servicio.reintegrar(admin.getId(),
                new ReintegrarPersonalSolicitud(guate.getId(), RolPersonal.ADMIN_SUCURSAL, FECHA), adminSistema),
                ErrorPersonal.PERSONAL_CUENTA_INACTIVA);
        verify(repositorio, never()).saveAndFlush(any(PersonalSucursalEntidad.class));
        verifyNoInteractions(eventos);
    }

    @Test
    void noSeReintegraConLaCuentaBloqueada() {
        PersonalSucursalEntidad admin = retirado(RolPersonal.ADMIN_SUCURSAL);
        SucursalEntidad guate = sucursalActiva();
        cuentaEnIdentity(admin, "BLOQUEADO", Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> servicio.reintegrar(admin.getId(),
                new ReintegrarPersonalSolicitud(guate.getId(), RolPersonal.ADMIN_SUCURSAL, FECHA), adminSistema))
                .hasMessageContaining("bloqueada hasta el");
        verify(repositorio, never()).saveAndFlush(any(PersonalSucursalEntidad.class));
    }

    @Test
    void noSeReintegraConLaCuentaBloqueadaSinFecha() {
        PersonalSucursalEntidad cajero = retirado(RolPersonal.CAJERO);
        when(sucursales.findById(xela.getId())).thenReturn(Optional.of(xela));
        cuentaEnIdentity(cajero, "BLOQUEADO", null);

        VerificarError.lanza(() -> servicio.reintegrar(cajero.getId(),
                new ReintegrarPersonalSolicitud(xela.getId(), RolPersonal.CAJERO, FECHA), adminXela),
                ErrorPersonal.PERSONAL_CUENTA_BLOQUEADA);
    }

    @Test
    void unBloqueoVencidoNoImpideElReintegro() {
        PersonalSucursalEntidad admin = retirado(RolPersonal.ADMIN_SUCURSAL);
        SucursalEntidad guate = sucursalActiva();
        cuentaEnIdentity(admin, "BLOQUEADO", Instant.now().minusSeconds(60));
        guardarDevuelveLaMisma();

        PersonalSucursalRespuesta respuesta = servicio.reintegrar(admin.getId(),
                new ReintegrarPersonalSolicitud(guate.getId(), RolPersonal.ADMIN_SUCURSAL, FECHA), adminSistema);

        assertThat(respuesta.activo()).isTrue();
    }

    @Test
    void reintegrarAAlguienQueSigueActivo() {
        PersonalSucursalEntidad activo = DatosPrueba.personal(xela.getId(), RolPersonal.CAJERO);
        when(repositorio.findById(activo.getId())).thenReturn(Optional.of(activo));

        VerificarError.lanza(() -> servicio.reintegrar(activo.getId(),
                new ReintegrarPersonalSolicitud(xela.getId(), RolPersonal.CAJERO, FECHA), adminXela),
                ErrorPersonal.PERSONAL_ACTIVO);
        verify(repositorio, never()).saveAndFlush(any(PersonalSucursalEntidad.class));
    }

    @Test
    void noSeReintegraSiYaTrabajaEnOtraSucursal() {
        PersonalSucursalEntidad cajero = retirado(RolPersonal.CAJERO);
        when(repositorio.existsByUsuarioIdAndActivoTrue(cajero.getUsuarioId())).thenReturn(true);

        VerificarError.lanza(() -> servicio.reintegrar(cajero.getId(),
                new ReintegrarPersonalSolicitud(xela.getId(), RolPersonal.CAJERO, FECHA), adminSistema),
                ErrorPersonal.PERSONAL_EN_OTRA_SUCURSAL);
    }

    @Test
    void noSeReintegraEnUnaSucursalQueNoExiste() {
        PersonalSucursalEntidad cajero = retirado(RolPersonal.CAJERO);
        UUID otra = UUID.randomUUID();

        VerificarError.lanza(() -> servicio.reintegrar(cajero.getId(),
                new ReintegrarPersonalSolicitud(otra, RolPersonal.CAJERO, FECHA), adminSistema),
                ErrorSucursal.SUCURSAL_NO_EXISTE);
    }

    @Test
    void noSeReintegraEnUnaSucursalInactiva() {
        PersonalSucursalEntidad cajero = retirado(RolPersonal.CAJERO);
        SucursalEntidad cerrada = DatosPrueba.sucursal(false);
        when(sucursales.findById(cerrada.getId())).thenReturn(Optional.of(cerrada));

        VerificarError.lanza(() -> servicio.reintegrar(cajero.getId(),
                new ReintegrarPersonalSolicitud(cerrada.getId(), RolPersonal.CAJERO, FECHA), adminSistema),
                ErrorSucursal.SUCURSAL_INACTIVA);
    }

    @Test
    void unAdminDeSucursalSoloReintegraCajeros() {
        PersonalSucursalEntidad admin = retirado(RolPersonal.ADMIN_SUCURSAL);

        VerificarError.lanza(() -> servicio.reintegrar(admin.getId(),
                new ReintegrarPersonalSolicitud(xela.getId(), RolPersonal.ADMIN_SUCURSAL, FECHA), adminXela),
                ErrorPersonal.SOLO_CAJEROS);
    }

    @Test
    void unAdminDeSucursalNoReintegraEnOtraSucursal() {
        PersonalSucursalEntidad cajero = retirado(RolPersonal.CAJERO);
        UUID otra = UUID.randomUUID();

        assertThatThrownBy(() -> servicio.reintegrar(cajero.getId(),
                new ReintegrarPersonalSolicitud(otra, RolPersonal.CAJERO, FECHA), adminXela))
                .isInstanceOf(ProhibidoException.class);
    }

    @Test
    void reintegrarCreaOtraAsignacionYPublica() {
        PersonalSucursalEntidad cajero = retirado(RolPersonal.CAJERO);
        SucursalEntidad guate = DatosPrueba.sucursal(true);
        when(sucursales.findById(guate.getId())).thenReturn(Optional.of(guate));
        cuentaEnIdentity(cajero, "ACTIVO", null);
        guardarDevuelveLaMisma();

        PersonalSucursalRespuesta respuesta = servicio.reintegrar(cajero.getId(),
                new ReintegrarPersonalSolicitud(guate.getId(), RolPersonal.ADMIN_SUCURSAL, FECHA.plusDays(3)),
                adminSistema);

        assertThat(respuesta.activo()).isTrue();
        assertThat(respuesta.personalId()).isNotEqualTo(cajero.getId());
        assertThat(respuesta.sucursalId()).isEqualTo(guate.getId());
        assertThat(respuesta.usuarioId()).isEqualTo(cajero.getUsuarioId());
        assertThat(respuesta.rol()).isEqualTo(RolPersonal.ADMIN_SUCURSAL);
        assertThat(cajero.isActivo()).isFalse();
        verify(eventos).personalAsignado(any(PersonalSucursalEntidad.class), eq(guate.getNombre()),
                eq(FECHA.plusDays(3)));
        verify(identity, never()).crearCuentaPersonal(any(SolicitudCuentaPersonal.class));   // sin R5
    }
}
