package gt.cunoc.proyecto2.fleet.eventos;

import static gt.cunoc.proyecto2.fleet.DatosPrueba.FECHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.fleet.DatosPrueba;
import gt.cunoc.proyecto2.fleet.eventos.publicados.buses.BusDesactivado;
import gt.cunoc.proyecto2.fleet.eventos.publicados.buses.BusEvento;
import gt.cunoc.proyecto2.fleet.eventos.publicados.choferes.ChoferDesactivado;
import gt.cunoc.proyecto2.fleet.eventos.publicados.choferes.ChoferEvento;
import gt.cunoc.proyecto2.fleet.eventos.publicados.choferes.LicenciaChoferActualizada;
import gt.cunoc.proyecto2.fleet.eventos.publicados.personal.PersonalSucursalAsignado;
import gt.cunoc.proyecto2.fleet.eventos.publicados.personal.PersonalSucursalDesactivado;
import gt.cunoc.proyecto2.fleet.eventos.publicados.sucursales.SucursalEvento;
import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PublicadorEventosFleetTest {

    private final PublicadorEventos publicador = mock(PublicadorEventos.class);
    private final PublicadorEventosFleet eventos = new PublicadorEventosFleet(publicador);
    private final UUID sucursalId = UUID.randomUUID();

    /** Verifica tipo de agregado, id, version, nombre del evento y fecha; devuelve el payload. */
    private Object publicado(String agregado, UUID id, Long version, String evento) {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(publicador).publicar(eq(agregado), eq(id), eq(version), eq(evento), payload.capture(), eq(FECHA));
        return payload.getValue();
    }

    @Test
    void eventosDeSucursal() {
        SucursalEntidad sucursal = DatosPrueba.sucursal(true);

        eventos.sucursalCreada(sucursal, FECHA);
        Object creada = publicado("Sucursal", sucursal.getId(), 0L, "SucursalCreada");

        sucursal.setVersion(1);
        eventos.sucursalActualizada(sucursal, FECHA);
        Object actualizada = publicado("Sucursal", sucursal.getId(), 1L, "SucursalActualizada");

        assertThat(creada).isEqualTo(SucursalEvento.desde(sucursal));
        assertThat(actualizada).isInstanceOfSatisfying(SucursalEvento.class, e -> {
            assertThat(e.codigo()).isEqualTo("XELA");
            assertThat(e.activa()).isTrue();
        });
    }

    @Test
    void eventosDePersonal() {
        PersonalSucursalEntidad personal = DatosPrueba.personal(sucursalId, RolPersonal.CAJERO);

        eventos.personalAsignado(personal, FECHA);
        Object asignado = publicado("PersonalSucursal", personal.getId(), 0L, "PersonalSucursalAsignado");

        personal.desactivar(FECHA);
        eventos.personalDesactivado(personal, FECHA);
        Object desactivado = publicado("PersonalSucursal", personal.getId(), 0L, "PersonalSucursalDesactivado");

        assertThat(asignado).isInstanceOfSatisfying(PersonalSucursalAsignado.class, e -> {
            assertThat(e.usuarioId()).isEqualTo(personal.getUsuarioId());
            assertThat(e.rolCodigo()).isEqualTo(RolPersonal.CAJERO);
        });
        assertThat(desactivado).isInstanceOfSatisfying(PersonalSucursalDesactivado.class,
                e -> assertThat(e.fechaDesasignacion()).isEqualTo(FECHA));
    }

    @Test
    void eventosDeBus() {
        BusEntidad bus = DatosPrueba.bus(sucursalId);

        eventos.busCreado(bus, FECHA);
        Object creado = publicado("Bus", bus.getId(), 0L, "BusCreado");

        eventos.busActualizado(bus, FECHA);
        Object actualizado = publicado("Bus", bus.getId(), 0L, "BusActualizado");

        bus.cambiarEstado(EstadoBus.INACTIVO);
        eventos.busDesactivado(bus, FECHA);
        Object desactivado = publicado("Bus", bus.getId(), 0L, "BusDesactivado");

        assertThat(creado).isInstanceOfSatisfying(BusEvento.class,
                e -> assertThat(e.placa()).isEqualTo("P-123ABC"));
        assertThat(actualizado).isInstanceOf(BusEvento.class);
        assertThat(desactivado).isEqualTo(new BusDesactivado(bus.getId(), sucursalId, "P-123ABC"));
    }

    @Test
    void eventosDeChofer() {
        ChoferEntidad chofer = DatosPrueba.chofer(sucursalId);

        eventos.choferCreado(chofer, FECHA);
        Object creado = publicado("Chofer", chofer.getId(), 0L, "ChoferCreado");

        eventos.choferActualizado(chofer, FECHA);
        Object actualizado = publicado("Chofer", chofer.getId(), 0L, "ChoferActualizado");

        eventos.licenciaChoferActualizada(chofer, FECHA);
        Object licencia = publicado("Chofer", chofer.getId(), 0L, "LicenciaChoferActualizada");

        eventos.choferDesactivado(chofer, FECHA);
        Object desactivado = publicado("Chofer", chofer.getId(), 0L, "ChoferDesactivado");

        assertThat(creado).isInstanceOfSatisfying(ChoferEvento.class,
                e -> assertThat(e.nombreCompleto()).isEqualTo("Juan Perez"));
        assertThat(actualizado).isEqualTo(ChoferEvento.desde(chofer));
        assertThat(licencia).isInstanceOfSatisfying(LicenciaChoferActualizada.class,
                e -> assertThat(e.fechaVencimientoLicencia()).isEqualTo(chofer.getFechaVencimientoLicencia()));
        assertThat(desactivado).isEqualTo(ChoferDesactivado.desde(chofer));
    }

    @Test
    void unaEntidadNuevaSinVersionSePublicaConVersionNula() {
        BusEntidad bus = BusEntidad.registrar(sucursalId, "P-999XYZ", "Volvo", "B7", 2015, 40,
                BigDecimal.ZERO, DatosPrueba.FOTO_BUS);

        eventos.busCreado(bus, FECHA);

        publicado("Bus", bus.getId(), null, "BusCreado");
    }
}
