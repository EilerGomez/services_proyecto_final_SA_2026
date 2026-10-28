package gt.cunoc.proyecto2.trips.servicios.proyecciones;

import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.ChoferEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.fleet.SucursalEvento;
import gt.cunoc.proyecto2.trips.eventos.recibidos.identity.TarifaDepreciacionCambiada;
import gt.cunoc.proyecto2.trips.eventos.recibidos.sales.TarifaReembolsoCambiada;
import java.util.UUID;

/**
 * Copias locales (vista_*) de los datos de Fleet e Identity. Solo las llaman los manejadores de eventos.
 * version = aggregateVersion del evento: un evento viejo (version menor a la guardada) no cambia nada.
 */
public interface ProyeccionServicio {

    void guardarSucursal(SucursalEvento sucursal, Long version);

    void guardarBus(BusEvento bus, Long version);

    void desactivarBus(UUID busId, Long version);

    void guardarChofer(ChoferEvento chofer, Long version);

    void desactivarChofer(UUID choferId, Long version);

    void guardarTarifa(TarifaDepreciacionCambiada tarifa);

    /**
     * TarifaReembolsoCambiada: copia de la política de cancelación de Sales. A diferencia de las
     * tarifas de depreciación, estas sí se editan, así que es un UPSERT por id.
     */
    void guardarTarifaReembolso(TarifaReembolsoCambiada tarifa);

    /**
     * ClienteRegistrado / PerfilActualizado: copia del cliente con cuenta. Sirve para ligar a su
     * cuenta un alquiler que la ventanilla pide con su NIT. Identity no manda version: gana el último.
     */
    void guardarCliente(UUID usuarioId, String nit, String nombre, String correo);
}