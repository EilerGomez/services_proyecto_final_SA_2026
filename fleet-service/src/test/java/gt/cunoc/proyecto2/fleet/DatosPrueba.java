package gt.cunoc.proyecto2.fleet;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.TipoLicencia;
import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

/** Datos de prueba reutilizables (entidades, usuarios y solicitudes válidas). */
public final class DatosPrueba {

    public static final LocalDate FECHA = LocalDate.of(2026, 10, 20);
    public static final String BUCKET = "proyecto-final-sa-fotos";
    public static final String URL_FOTOS = "https://storage.googleapis.com/" + BUCKET + "/";
    public static final String FOTO_BUS = URL_FOTOS + "dev/buses/bus.jpg";
    public static final String FOTO_CHOFER = URL_FOTOS + "dev/choferes/chofer.jpg";

    private DatosPrueba() {
    }

    // ----- Usuarios (claims del JWT) -----

    public static UsuarioActual adminSistema() {
        return new UsuarioActual(UUID.randomUUID(), "admin@fleet.gt", Set.of(Roles.ADMIN_SISTEMA), null, null);
    }

    public static UsuarioActual adminSucursal(UUID sucursalId) {
        return new UsuarioActual(UUID.randomUUID(), "admin.xela@fleet.gt", Set.of(Roles.ADMIN_SUCURSAL),
                sucursalId, null);
    }

    public static UsuarioActual usuarioChofer(UUID choferId) {
        return new UsuarioActual(UUID.randomUUID(), "chofer@fleet.gt", Set.of(Roles.CHOFER), null, choferId);
    }

    // ----- Entidades (con version 0, como si ya estuvieran guardadas) -----

    public static SucursalEntidad sucursal(boolean activa) {
        SucursalEntidad sucursal = new SucursalEntidad();
        sucursal.setId(UUID.randomUUID());
        sucursal.setCodigo("XELA");
        sucursal.setNombre("Xela Central");
        sucursal.setDepartamento("Quetzaltenango");
        sucursal.setMunicipio("Quetzaltenango");
        sucursal.setDireccion("4a calle 10-20 zona 1");
        sucursal.setTelefono("77651234");
        sucursal.setLatitud(new BigDecimal("14.834500"));
        sucursal.setLongitud(new BigDecimal("-91.518000"));
        sucursal.setActiva(activa);
        sucursal.setVersion(0);
        return sucursal;
    }

    public static BusEntidad bus(UUID sucursalId) {
        BusEntidad bus = BusEntidad.registrar(sucursalId, "P-123ABC", "Volvo", "B12", 2018, 50,
                new BigDecimal("1000.00"), FOTO_BUS);
        return conVersion(bus, 0);
    }

    public static ChoferEntidad chofer(UUID sucursalId) {
        return chofer(UUID.randomUUID(), sucursalId);
    }

    public static ChoferEntidad chofer(UUID choferId, UUID sucursalId) {
        ChoferEntidad chofer = ChoferEntidad.registrar(choferId, sucursalId, UUID.randomUUID(), "Juan Perez",
                "LIC-1234", TipoLicencia.A, FECHA.plusYears(2), new BigDecimal("350.00"));
        return conVersion(chofer, 0);
    }

    public static PersonalSucursalEntidad personal(UUID sucursalId, RolPersonal rol) {
        return personal(UUID.randomUUID(), sucursalId, rol);
    }

    public static PersonalSucursalEntidad personal(UUID personalId, UUID sucursalId, RolPersonal rol) {
        PersonalSucursalEntidad personal = PersonalSucursalEntidad.asignar(personalId, sucursalId,
                UUID.randomUUID(), "Ana Lopez", "ana@fleet.gt", rol, FECHA.minusMonths(1));
        return conVersion(personal, 0);
    }

    /** Pone la version que tendría la fila en la BD (las entidades no tienen setter de version). */
    public static <T> T conVersion(T entidad, int version) {
        ReflectionTestUtils.setField(entidad, "version", version);
        return entidad;
    }

    // ----- Solicitudes -----

    public static DatosCuentaSolicitud cuenta(UUID solicitudId, String fotoUrl) {
        return new DatosCuentaSolicitud(solicitudId, "ana@fleet.gt", "Ana", "Lopez", "1234567-8",
                "1234567890101", "55551234", "Zona 1, Quetzaltenango", fotoUrl);
    }
}
