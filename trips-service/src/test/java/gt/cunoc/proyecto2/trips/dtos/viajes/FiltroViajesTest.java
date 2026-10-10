package gt.cunoc.proyecto2.trips.dtos.viajes;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.DatosPrueba;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FiltroViajesTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 20);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 31);

    private final UUID rutaId = UUID.randomUUID();
    private final UUID choferId = UUID.randomUUID();

    private FiltroViajes filtro(UUID sucursalId) {
        return new FiltroViajes(rutaId, sucursalId, choferId, EstadoViaje.EN_RUTA, TipoViaje.PRIVADO, DESDE, HASTA);
    }

    @Test
    void desdePosteriorAHastaEsRangoInvalido() {
        VerificarError.lanza(() -> new FiltroViajes(null, null, null, null, null, HASTA, DESDE),
                ErrorViaje.RANGO_FECHAS_INVALIDO);
    }

    @Test
    void rangosValidosOAbiertos() {
        assertThat(new FiltroViajes(null, null, null, null, null, DESDE, DESDE).desde()).isEqualTo(DESDE);
        assertThat(new FiltroViajes(null, null, null, null, null, DESDE, null).hasta()).isNull();
        assertThat(new FiltroViajes(null, null, null, null, null, null, HASTA).desde()).isNull();
        assertThat(new FiltroViajes(null, null, null, null, null, null, null).estado()).isNull();
    }

    @Test
    void sinSesionSoloVeViajesRegularesProgramados() {
        FiltroViajes resultado = filtro(DatosPrueba.XELA).paraUsuario(null);

        assertThat(resultado.rutaId()).isEqualTo(rutaId);
        assertThat(resultado.sucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(resultado.choferId()).isNull();
        assertThat(resultado.estado()).isEqualTo(EstadoViaje.PROGRAMADO);
        assertThat(resultado.tipo()).isEqualTo(TipoViaje.REGULAR);
        assertThat(resultado.desde()).isEqualTo(DESDE);
        assertThat(resultado.hasta()).isEqualTo(HASTA);
    }

    @Test
    void unClienteOUnChoferSoloVenViajesRegularesProgramados() {
        FiltroViajes deCliente = filtro(null).paraUsuario(DatosPrueba.cliente());
        FiltroViajes deChofer = filtro(null).paraUsuario(DatosPrueba.chofer(UUID.randomUUID()));

        assertThat(deCliente.estado()).isEqualTo(EstadoViaje.PROGRAMADO);
        assertThat(deCliente.tipo()).isEqualTo(TipoViaje.REGULAR);
        assertThat(deCliente.choferId()).isNull();
        assertThat(deChofer.estado()).isEqualTo(EstadoViaje.PROGRAMADO);
        assertThat(deChofer.tipo()).isEqualTo(TipoViaje.REGULAR);
    }

    @Test
    void adminSistemaVeTodoSinCambios() {
        FiltroViajes original = filtro(DatosPrueba.GUATE);

        assertThat(original.paraUsuario(DatosPrueba.adminSistema())).isSameAs(original);
    }

    @Test
    void adminSucursalSinSucursalEnElFiltroUsaLaSuya() {
        FiltroViajes resultado = filtro(null).paraUsuario(DatosPrueba.adminSucursal(DatosPrueba.XELA));

        assertThat(resultado.sucursalId()).isEqualTo(DatosPrueba.XELA);
        assertThat(resultado.rutaId()).isEqualTo(rutaId);
        assertThat(resultado.choferId()).isEqualTo(choferId);
        assertThat(resultado.estado()).isEqualTo(EstadoViaje.EN_RUTA);
        assertThat(resultado.tipo()).isEqualTo(TipoViaje.PRIVADO);
        assertThat(resultado.desde()).isEqualTo(DESDE);
        assertThat(resultado.hasta()).isEqualTo(HASTA);
    }

    @Test
    void cajeroConSuPropiaSucursal() {
        FiltroViajes resultado = filtro(DatosPrueba.XELA).paraUsuario(DatosPrueba.cajero(DatosPrueba.XELA));

        assertThat(resultado.sucursalId()).isEqualTo(DatosPrueba.XELA);
    }

    @Test
    void otraSucursalEsSucursalAjena() {
        UsuarioActual cajero = DatosPrueba.cajero(DatosPrueba.XELA);

        VerificarError.lanza(() -> filtro(DatosPrueba.GUATE).paraUsuario(cajero), "SUCURSAL_AJENA");
    }

    @Test
    void adminSucursalSinSucursalAsignadaEsSucursalAjena() {
        UsuarioActual admin = DatosPrueba.adminSucursal(null);

        VerificarError.lanza(() -> filtro(null).paraUsuario(admin), "SUCURSAL_AJENA");
    }

    @Test
    void delChoferFiltraSoloPorSusViajes() {
        FiltroViajes resultado = FiltroViajes.delChofer(choferId, EstadoViaje.PROGRAMADO, DESDE, HASTA);

        assertThat(resultado.choferId()).isEqualTo(choferId);
        assertThat(resultado.rutaId()).isNull();
        assertThat(resultado.sucursalId()).isNull();
        assertThat(resultado.tipo()).isNull();
        assertThat(resultado.estado()).isEqualTo(EstadoViaje.PROGRAMADO);
        assertThat(resultado.desde()).isEqualTo(DESDE);
        assertThat(resultado.hasta()).isEqualTo(HASTA);
    }

    @Test
    void delChoferSinChoferAsociado() {
        VerificarError.lanza(() -> FiltroViajes.delChofer(null, null, null, null), ErrorViaje.SIN_CHOFER_ASOCIADO);
    }

    @Test
    void delChoferConRangoInvalido() {
        VerificarError.lanza(() -> FiltroViajes.delChofer(choferId, null, HASTA, DESDE),
                ErrorViaje.RANGO_FECHAS_INVALIDO);
    }
}
