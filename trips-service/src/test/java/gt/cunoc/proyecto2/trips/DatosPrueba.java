package gt.cunoc.proyecto2.trips;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.CanalAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.ClienteAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.ConfiguracionAlquilerEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.RecorridoAlquiler;
import gt.cunoc.proyecto2.trips.modelos.buses.VistaBusEntidad;
import gt.cunoc.proyecto2.trips.modelos.choferes.VistaChoferEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.LlegadaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.sucursales.VistaSucursalEntidad;
import gt.cunoc.proyecto2.trips.modelos.tarifas.VistaTarifaDepreciacionEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.AsignacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Datos de prueba reutilizables (entidades, usuarios y fechas).
 * Las entidades salen con version 0, como si ya estuvieran guardadas.
 */
public final class DatosPrueba {

    public static final UUID XELA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID GUATE = UUID.fromString("22222222-2222-2222-2222-222222222222");

    public static final LocalDate FECHA = LocalDate.of(2026, 10, 20);
    /** 25-oct 06:00 y 10:30 en Guatemala. */
    public static final OffsetDateTime SALIDA = OffsetDateTime.parse("2026-10-25T06:00:00-06:00");
    public static final OffsetDateTime LLEGADA = OffsetDateTime.parse("2026-10-25T10:30:00-06:00");

    public static final BigDecimal SALARIO = new BigDecimal("350.00");
    public static final BigDecimal PRECIO_BOLETO = new BigDecimal("75.00");

    private DatosPrueba() {
    }

    // ----- Usuarios (claims del JWT) -----

    public static UsuarioActual adminSistema() {
        return new UsuarioActual(UUID.randomUUID(), "admin@sistema.gt", Set.of(Roles.ADMIN_SISTEMA), null, null);
    }

    public static UsuarioActual adminSucursal(UUID sucursalId) {
        return new UsuarioActual(UUID.randomUUID(), "admin@sucursal.gt", Set.of(Roles.ADMIN_SUCURSAL),
                sucursalId, null);
    }

    public static UsuarioActual cajero(UUID sucursalId) {
        return new UsuarioActual(UUID.randomUUID(), "cajero@sucursal.gt", Set.of(Roles.CAJERO), sucursalId, null);
    }

    public static UsuarioActual chofer(UUID choferId) {
        return new UsuarioActual(UUID.randomUUID(), "chofer@trips.gt", Set.of(Roles.CHOFER), null, choferId);
    }

    public static UsuarioActual cliente() {
        return new UsuarioActual(UUID.randomUUID(), "cliente@correo.com", Set.of(Roles.CLIENTE), null, null);
    }

    public static UsuarioActual servicio() {
        return new UsuarioActual(UUID.randomUUID(), "fleet-service", Set.of(Roles.SERVICIO), null, null);
    }

    // ----- Rutas y viajes -----

    /** Ruta activa Xela → Guate (205 km, Q75), version 0. */
    public static RutaEntidad ruta() {
        return ruta(XELA, GUATE);
    }

    public static RutaEntidad ruta(UUID origen, UUID destino) {
        RutaEntidad ruta = RutaEntidad.registrar(origen, destino, new BigDecimal("205.00"), PRECIO_BOLETO,
                UUID.randomUUID());
        return conVersion(ruta, 0);
    }

    /** Bus de 45 asientos y chofer con salario 350, del 25-oct 06:00 a 10:30. */
    public static AsignacionViaje asignacion() {
        return asignacion(UUID.randomUUID(), UUID.randomUUID(), 45);
    }

    public static AsignacionViaje asignacion(UUID busId, UUID choferId, int capacidad) {
        return new AsignacionViaje(busId, capacidad, choferId, SALARIO, SALIDA, LLEGADA);
    }

    /** Viaje REGULAR PROGRAMADO de la ruta, version 0. */
    public static ViajeEntidad viajeRegular(RutaEntidad ruta, AsignacionViaje asignacion) {
        return conVersion(ViajeEntidad.programarRegular(ruta, asignacion, UUID.randomUUID()), 0);
    }

    public static ViajeEntidad viajeRegular() {
        return viajeRegular(ruta(), asignacion());
    }

    /** Viaje PRIVADO PROGRAMADO del alquiler, version 0. */
    public static ViajeEntidad viajePrivado(AlquilerPrivadoEntidad alquiler, AsignacionViaje asignacion) {
        return conVersion(ViajeEntidad.programarPrivado(alquiler, asignacion, UUID.randomUUID()), 0);
    }

    /** Salida a las 06:05 con 1000 km. */
    public static SalidaViajeEntidad salida(ViajeEntidad viaje) {
        return SalidaViajeEntidad.registrar(viaje, SALIDA.plusMinutes(5), new BigDecimal("1000.00"),
                UUID.randomUUID());
    }

    /** Llegada a las 10:40 con 1207.50 km, combustible 950 y tarifa 1.25. */
    public static LlegadaViajeEntidad llegada(ViajeEntidad viaje, SalidaViajeEntidad salida) {
        return LlegadaViajeEntidad.registrar(viaje, salida, LLEGADA.plusMinutes(10), new BigDecimal("1207.50"),
                new BigDecimal("950.00"), new BigDecimal("1.2500"), UUID.randomUUID());
    }

    // ----- Alquileres -----

    /** Recorrido del 5-nov, ida y vuelta, 30 pasajeros, 200 km. */
    public static RecorridoAlquiler recorrido() {
        return new RecorridoAlquiler("Quetzaltenango", "Antigua Guatemala",
                OffsetDateTime.parse("2026-11-05T07:00:00-06:00"), OffsetDateTime.parse("2026-11-05T19:00:00-06:00"),
                30, new BigDecimal("200.00"),
                new BigDecimal("14.834700"), new BigDecimal("-91.518400"),
                new BigDecimal("14.558600"), new BigDecimal("-90.729500"));
    }

    /** Alquiler SOLICITADO EN_LINEA de un cliente con cuenta, en Xela, precio estimado 2750, version 0. */
    public static AlquilerPrivadoEntidad alquiler(UUID clienteUsuarioId) {
        ClienteAlquiler cliente = new ClienteAlquiler(clienteUsuarioId, "1234567-8", "Ana López", "cliente@correo.com");
        AlquilerPrivadoEntidad alquiler = AlquilerPrivadoEntidad.solicitar(XELA, CanalAlquiler.EN_LINEA, cliente,
                recorrido(), new BigDecimal("2750.00"), FECHA, clienteUsuarioId);
        return conVersion(alquiler, 0);
    }

    public static AlquilerPrivadoEntidad alquiler() {
        return alquiler(UUID.randomUUID());
    }

    /** Alquiler ya PAGADO (SOLICITADO → COTIZADO 2600 → PAGADO). */
    public static AlquilerPrivadoEntidad alquilerPagado() {
        AlquilerPrivadoEntidad alquiler = alquiler();
        alquiler.cotizar(new BigDecimal("2600.00"), UUID.randomUUID());
        alquiler.marcarPagado();
        return alquiler;
    }

    public static ConfiguracionAlquilerEntidad configuracion(String precioPorKm, LocalDate vigenteDesde) {
        ConfiguracionAlquilerEntidad configuracion = ConfiguracionAlquilerEntidad.registrar(
                new BigDecimal(precioPorKm), vigenteDesde, UUID.randomUUID());
        ReflectionTestUtils.setField(configuracion, "id", UUID.randomUUID());
        return configuracion;
    }

    // Vistas (copias de Fleet e Identity): sin fábrica, se llenan por reflexión

    public static VistaSucursalEntidad vistaSucursal(UUID id, String nombre, boolean activa) {
        VistaSucursalEntidad sucursal = BeanUtils.instantiateClass(VistaSucursalEntidad.class);
        ReflectionTestUtils.setField(sucursal, "id", id);
        ReflectionTestUtils.setField(sucursal, "nombre", nombre);
        ReflectionTestUtils.setField(sucursal, "latitud", new BigDecimal("14.834999"));
        ReflectionTestUtils.setField(sucursal, "longitud", new BigDecimal("-91.518639"));
        ReflectionTestUtils.setField(sucursal, "activa", activa);
        return sucursal;
    }

    public static VistaBusEntidad vistaBus(UUID id, String placa) {
        VistaBusEntidad bus = BeanUtils.instantiateClass(VistaBusEntidad.class);
        ReflectionTestUtils.setField(bus, "id", id);
        ReflectionTestUtils.setField(bus, "sucursalId", XELA);
        ReflectionTestUtils.setField(bus, "placa", placa);
        ReflectionTestUtils.setField(bus, "capacidadPasajeros", 45);
        ReflectionTestUtils.setField(bus, "estado", "ACTIVO");
        return bus;
    }

    public static VistaChoferEntidad vistaChofer(UUID id, String nombre) {
        VistaChoferEntidad chofer = BeanUtils.instantiateClass(VistaChoferEntidad.class);
        ReflectionTestUtils.setField(chofer, "id", id);
        ReflectionTestUtils.setField(chofer, "sucursalId", XELA);
        ReflectionTestUtils.setField(chofer, "nombreCompleto", nombre);
        ReflectionTestUtils.setField(chofer, "salarioBaseViaje", SALARIO);
        ReflectionTestUtils.setField(chofer, "estado", "ACTIVO");
        return chofer;
    }

    public static VistaTarifaDepreciacionEntidad vistaTarifa(String montoPorKm, LocalDate vigenteDesde) {
        VistaTarifaDepreciacionEntidad tarifa = BeanUtils.instantiateClass(VistaTarifaDepreciacionEntidad.class);
        ReflectionTestUtils.setField(tarifa, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(tarifa, "montoPorKm", new BigDecimal(montoPorKm));
        ReflectionTestUtils.setField(tarifa, "vigenteDesde", vigenteDesde);
        return tarifa;
    }

    /** Pone la version que tendría la fila en la BD (las entidades no tienen setter de version). */
    public static <T> T conVersion(T entidad, int version) {
        ReflectionTestUtils.setField(entidad, "version", version);
        return entidad;
    }
}
