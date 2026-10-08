package gt.cunoc.proyecto2.fleet.eventos;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.fleet.eventos.publicados.buses.BusDesactivado;
import gt.cunoc.proyecto2.fleet.eventos.publicados.buses.BusEvento;
import gt.cunoc.proyecto2.fleet.eventos.publicados.choferes.ChoferDesactivado;
import gt.cunoc.proyecto2.fleet.eventos.publicados.choferes.ChoferEvento;
import gt.cunoc.proyecto2.fleet.eventos.publicados.choferes.LicenciaChoferActualizada;
import gt.cunoc.proyecto2.fleet.eventos.publicados.personal.PersonalSucursalAsignado;
import gt.cunoc.proyecto2.fleet.eventos.publicados.personal.PersonalSucursalDesactivado;
import gt.cunoc.proyecto2.fleet.eventos.publicados.sucursales.SucursalEvento;
import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PublicadorEventosFleet {

    private final PublicadorEventos publicador;

    public PublicadorEventosFleet(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    // ----- Sucursales -----

    public void sucursalCreada(SucursalEntidad sucursal, LocalDate fecha) {
        publicar(TipoAgregado.SUCURSAL, sucursal.getId(), sucursal.getVersion(),
                TipoEvento.SUCURSAL_CREADA, SucursalEvento.desde(sucursal), fecha);
    }

    public void sucursalActualizada(SucursalEntidad sucursal, LocalDate fecha) {
        publicar(TipoAgregado.SUCURSAL, sucursal.getId(), sucursal.getVersion(),
                TipoEvento.SUCURSAL_ACTUALIZADA, SucursalEvento.desde(sucursal), fecha);
    }

    // ----- Personal de sucursal -----

    public void personalAsignado(PersonalSucursalEntidad personal, LocalDate fecha) {
        publicar(TipoAgregado.PERSONAL_SUCURSAL, personal.getId(), personal.getVersion(),
                TipoEvento.PERSONAL_SUCURSAL_ASIGNADO, PersonalSucursalAsignado.desde(personal), fecha);
    }

    public void personalDesactivado(PersonalSucursalEntidad personal, LocalDate fecha) {
        publicar(TipoAgregado.PERSONAL_SUCURSAL, personal.getId(), personal.getVersion(),
                TipoEvento.PERSONAL_SUCURSAL_DESACTIVADO, PersonalSucursalDesactivado.desde(personal), fecha);
    }

    // ----- Buses -----

    public void busCreado(BusEntidad bus, LocalDate fecha) {
        publicar(TipoAgregado.BUS, bus.getId(), bus.getVersion(), TipoEvento.BUS_CREADO, BusEvento.desde(bus), fecha);
    }

    public void busActualizado(BusEntidad bus, LocalDate fecha) {
        publicar(TipoAgregado.BUS, bus.getId(), bus.getVersion(), TipoEvento.BUS_ACTUALIZADO, BusEvento.desde(bus), fecha);
    }

    public void busDesactivado(BusEntidad bus, LocalDate fecha) {
        publicar(TipoAgregado.BUS, bus.getId(), bus.getVersion(),
                TipoEvento.BUS_DESACTIVADO, BusDesactivado.desde(bus), fecha);
    }

    // ----- Choferes -----

    public void choferCreado(ChoferEntidad chofer, LocalDate fecha) {
        publicar(TipoAgregado.CHOFER, chofer.getId(), chofer.getVersion(),
                TipoEvento.CHOFER_CREADO, ChoferEvento.desde(chofer), fecha);
    }

    public void choferActualizado(ChoferEntidad chofer, LocalDate fecha) {
        publicar(TipoAgregado.CHOFER, chofer.getId(), chofer.getVersion(),
                TipoEvento.CHOFER_ACTUALIZADO, ChoferEvento.desde(chofer), fecha);
    }

    public void choferDesactivado(ChoferEntidad chofer, LocalDate fecha) {
        publicar(TipoAgregado.CHOFER, chofer.getId(), chofer.getVersion(),
                TipoEvento.CHOFER_DESACTIVADO, ChoferDesactivado.desde(chofer), fecha);
    }

    public void licenciaChoferActualizada(ChoferEntidad chofer, LocalDate fecha) {
        publicar(TipoAgregado.CHOFER, chofer.getId(), chofer.getVersion(),
                TipoEvento.LICENCIA_CHOFER_ACTUALIZADA, LicenciaChoferActualizada.desde(chofer), fecha);
    }

    private void publicar(TipoAgregado agregado, UUID agregadoId, Integer version, TipoEvento evento,
                          Object payload, LocalDate fecha) {
        publicador.publicar(agregado.nombre(), agregadoId, version == null ? null : version.longValue(),
                evento.nombre(), payload, fecha);
    }
}