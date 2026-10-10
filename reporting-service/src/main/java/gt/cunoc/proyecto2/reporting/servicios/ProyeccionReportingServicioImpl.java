package gt.cunoc.proyecto2.reporting.servicios;

import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.ChoferEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.SucursalEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.identity.TarifaDepreciacionCambiada;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.maintenance.GastoTallerRegistrado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.AlquilerPagado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.BoletoVendido;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.VentaCancelada;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.RutaEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeCompletado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeEvento;
import gt.cunoc.proyecto2.reporting.modelos.BusEntidad;
import gt.cunoc.proyecto2.reporting.modelos.ChoferEntidad;
import gt.cunoc.proyecto2.reporting.modelos.GastoEntidad;
import gt.cunoc.proyecto2.reporting.modelos.IngresoEntidad;
import gt.cunoc.proyecto2.reporting.modelos.RutaEntidad;
import gt.cunoc.proyecto2.reporting.modelos.SucursalEntidad;
import gt.cunoc.proyecto2.reporting.modelos.TarifaDepreciacionEntidad;
import gt.cunoc.proyecto2.reporting.modelos.ViajeEntidad;
import gt.cunoc.proyecto2.reporting.repositorios.BusRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.ChoferRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.GastoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.IngresoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.RutaRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.SucursalRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.TarifaDepreciacionRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.ViajeRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProyeccionReportingServicioImpl implements ProyeccionReportingServicio {

    private static final UUID SIN_DETALLE = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private static final Logger log = LoggerFactory.getLogger(ProyeccionReportingServicioImpl.class);

    private final SucursalRepositorio sucursales;
    private final TarifaDepreciacionRepositorio tarifas;
    private final RutaRepositorio rutas;
    private final BusRepositorio buses;
    private final ChoferRepositorio choferes;
    private final ViajeRepositorio viajes;
    private final IngresoRepositorio ingresos;
    private final GastoRepositorio gastos;

    public ProyeccionReportingServicioImpl(SucursalRepositorio sucursales,
                                          TarifaDepreciacionRepositorio tarifas,
                                          RutaRepositorio rutas,
                                          BusRepositorio buses,
                                          ChoferRepositorio choferes,
                                          ViajeRepositorio viajes,
                                          IngresoRepositorio ingresos,
                                          GastoRepositorio gastos) {
        this.sucursales = sucursales;
        this.tarifas = tarifas;
        this.rutas = rutas;
        this.buses = buses;
        this.choferes = choferes;
        this.viajes = viajes;
        this.ingresos = ingresos;
        this.gastos = gastos;
    }

    /** Versión vieja o igual ya aplicada: se aplica (>=); solo se descarta si es menor. */
    private boolean versionVigente(Integer guardada, Long recibida) {
        if (recibida == null) {
            return true; // sin versión: gana el último en llegar
        }
        int actual = guardada != null ? guardada : 0;
        return recibida >= actual;
    }

    @Override
    @Transactional
    public void aplicarTarifa(TarifaDepreciacionCambiada evento) {
        if (evento == null || evento.tarifaId() == null
                || evento.vigenteDesde() == null || evento.montoPorKm() == null) {
            log.warn("TarifaDepreciacionCambiada sin datos, se ignora");
            return;
        }
        if (tarifas.existsById(evento.tarifaId())) {
            return; // ON CONFLICT (id) DO NOTHING
        }
        TarifaDepreciacionEntidad entidad = new TarifaDepreciacionEntidad();
        entidad.setId(evento.tarifaId());
        entidad.setMontoPorKm(evento.montoPorKm());
        entidad.setVigenteDesde(evento.vigenteDesde());
        tarifas.save(entidad);
    }

    @Override
    @Transactional
    public void aplicarSucursal(SucursalEvento evento, Long versionAgregado) {
        SucursalEntidad entidad = sucursales.findById(evento.sucursalId())
                .orElseGet(SucursalEntidad::new);
        if (entidad.getId() == null) {
            entidad.setId(evento.sucursalId());
        } else if (!versionVigente(entidad.getVersion(), versionAgregado)) {
            return;
        }
        entidad.setNombre(evento.nombre());
        entidad.setLatitud(evento.latitud());
        entidad.setLongitud(evento.longitud());
        entidad.setActiva(evento.activa());
        sucursales.save(entidad);
    }

    @Override
    @Transactional
    public void aplicarBus(BusEvento evento, Long versionAgregado) {
        BusEntidad entidad = buses.findById(evento.busId()).orElseGet(BusEntidad::new);
        if (entidad.getId() == null) {
            entidad.setId(evento.busId());
        } else if (!versionVigente(entidad.getVersion(), versionAgregado)) {
            return;
        }
        entidad.setSucursalId(evento.sucursalId());
        entidad.setPlaca(evento.placa());
        entidad.setMarca(evento.marca() != null ? evento.marca() : "-");
        entidad.setModelo(evento.modelo() != null ? evento.modelo() : "-");
        entidad.setCapacidadPasajeros(evento.capacidadPasajeros() != null ? evento.capacidadPasajeros() : 0);
        entidad.setEstado(evento.estado() != null ? evento.estado() : "ACTIVO");
        if (evento.kilometrajeActual() != null) {
            entidad.setKilometrajeActual(evento.kilometrajeActual());
        }
        // No se tocan total_viajes ni chofer_actual_id: los llevan los eventos de viaje.
        buses.save(entidad);
    }

    @Override
    @Transactional
    public void desactivarBus(UUID busId, Long versionAgregado) {
        buses.findById(busId).ifPresent(bus -> {
            if (!versionVigente(bus.getVersion(), versionAgregado)) {
                return;
            }
            bus.setEstado("INACTIVO");
            buses.save(bus);
        });
    }

    @Override
    @Transactional
    public void aplicarChofer(ChoferEvento evento, Long versionAgregado) {
        ChoferEntidad entidad = choferes.findById(evento.choferId()).orElseGet(ChoferEntidad::new);
        if (entidad.getId() == null) {
            entidad.setId(evento.choferId());
        } else if (!versionVigente(entidad.getVersion(), versionAgregado)) {
            return;
        }
        entidad.setSucursalId(evento.sucursalId());
        entidad.setNombreCompleto(evento.nombreCompleto());
        entidad.setNumeroLicencia(evento.numeroLicencia());
        entidad.setTipoLicencia(evento.tipoLicencia() != null ? evento.tipoLicencia() : "-");
        entidad.setFechaVencimientoLicencia(evento.fechaVencimientoLicencia());
        entidad.setEstado(evento.estado() != null ? evento.estado() : "ACTIVO");
        // No se toca total_viajes: lo llevan los eventos de viaje.
        choferes.save(entidad);
    }

    @Override
    @Transactional
    public void desactivarChofer(UUID choferId, Long versionAgregado) {
        choferes.findById(choferId).ifPresent(chofer -> {
            if (!versionVigente(chofer.getVersion(), versionAgregado)) {
                return;
            }
            chofer.setEstado("INACTIVO");
            choferes.save(chofer);
        });
    }

    @Override
    @Transactional
    public void aplicarRuta(RutaEvento evento) {
        RutaEntidad entidad = rutas.findById(evento.rutaId()).orElseGet(RutaEntidad::new);
        if (entidad.getId() == null) {
            entidad.setId(evento.rutaId());
        }
        entidad.setSucursalOrigenId(evento.sucursalOrigenId());
        entidad.setSucursalDestinoId(evento.sucursalDestinoId());
        entidad.setOrigenNombre(evento.origenNombre() != null ? evento.origenNombre()
                : nombreSucursal(evento.sucursalOrigenId()));
        entidad.setDestinoNombre(evento.destinoNombre() != null ? evento.destinoNombre()
                : nombreSucursal(evento.sucursalDestinoId()));
        entidad.setDistanciaKm(evento.distanciaKm());
        entidad.setPrecioBoleto(evento.precioBoleto());
        entidad.setActiva(evento.activa() == null || evento.activa());
        rutas.save(entidad);
    }

    @Override
    @Transactional
    public void eliminarRuta(UUID rutaId) {
        rutas.deleteById(rutaId); // solo se eliminan rutas sin viajes
    }

    @Override
    @Transactional
    public void aplicarViaje(ViajeEvento evento, Long versionAgregado) {
        ViajeEntidad entidad = viajes.findById(evento.viajeId()).orElseGet(ViajeEntidad::new);
        if (entidad.getViajeId() == null) {
            entidad.setViajeId(evento.viajeId());
            entidad.setEstado("PROGRAMADO");
        } else {
            if (!"PROGRAMADO".equals(entidad.getEstado()) && !"EN_RUTA".equals(entidad.getEstado())) {
                return; // un evento tardío no revive un viaje completado
            }
            if (!versionVigente(entidad.getVersion(), versionAgregado)) {
                return;
            }
        }
        entidad.setTipo(evento.tipo() != null ? evento.tipo() : "REGULAR");
        entidad.setRutaId(evento.rutaId());
        entidad.setSucursalId(evento.sucursalId());
        entidad.setBusId(evento.busId());
        entidad.setChoferId(evento.choferId());
        if (evento.fechaSalida() != null) {
            entidad.setFechaSalida(evento.fechaSalida());
        }
        viajes.save(entidad);
        recalcularChoferActual(entidad.getBusId());
    }

    @Override
    @Transactional
    public void aplicarViajeActualizado(ViajeEvento evento, Long versionAgregado) {
        UUID busAnterior = viajes.findById(evento.viajeId()).map(ViajeEntidad::getBusId).orElse(null);
        aplicarViaje(evento, versionAgregado);
        if (busAnterior != null && !busAnterior.equals(evento.busId())) {
            recalcularChoferActual(busAnterior); // si cambió de bus
        }
    }

    @Override
    @Transactional
    public void eliminarViaje(UUID viajeId) {
        UUID busId = viajes.findById(viajeId).map(ViajeEntidad::getBusId).orElse(null);
        viajes.deleteById(viajeId);
        if (busId != null) {
            recalcularChoferActual(busId);
        }
    }

    @Override
    @Transactional
    public void marcarViajeEnRuta(UUID viajeId, UUID busId, Long versionAgregado) {
        ViajeEntidad viaje = viajes.findById(viajeId).orElse(null);
        if (viaje == null) {
            // El ViajeProgramado aún no llegó (desorden): reintentar
            throw new IllegalStateException("Viaje " + viajeId + " todavía no está en la proyección");
        }
        if (!versionVigente(viaje.getVersion(), versionAgregado)) {
            return; // versión vieja: se descarta sin reintento
        }
        if (!"PROGRAMADO".equals(viaje.getEstado())) {
            return;
        }
        viaje.setEstado("EN_RUTA");
        viajes.save(viaje);
        recalcularChoferActual(busId != null ? busId : viaje.getBusId());
    }

    @Override
    @Transactional
    public void aplicarViajeCompletado(UUID eventoId, ViajeCompletado evento, Long versionAgregado,
                                       LocalDate fechaNegocio) {
        ViajeEntidad viaje = viajes.findById(evento.viajeId()).orElse(null);
        if (viaje == null) {
            // Llegó antes que el ViajeProgramado (desorden): reintentar
            throw new IllegalStateException("Viaje " + evento.viajeId() + " todavía no está en la proyección");
        }
        if (!"COMPLETADO".equals(viaje.getEstado())) {
            if (!versionVigente(viaje.getVersion(), versionAgregado)) {
                return; // versión vieja: se descarta sin reintento
            }
            viaje.setEstado("COMPLETADO");
            viaje.setKmRecorridos(evento.kmRecorridos());
            viaje.setTarifaDepreciacion(evento.tarifaDepreciacion());
            viaje.setCostoDepreciacion(evento.costoDepreciacion());
            viajes.save(viaje);
        }
        LocalDate fecha = fechaNegocio != null ? fechaNegocio : LocalDate.now();
        guardarGastoSiFalta(eventoId, "COMBUSTIBLE", null, evento.sucursalId(), evento.busId(),
                evento.viajeId(), evento.costoCombustible(), fecha, "Combustible");
        guardarGastoSiFalta(eventoId, "DEPRECIACION", null, evento.sucursalId(), evento.busId(),
                evento.viajeId(), evento.costoDepreciacion(), fecha, "Depreciación");
        guardarGastoSiFalta(eventoId, "SALARIO_CHOFER", null, evento.sucursalId(), evento.busId(),
                evento.viajeId(), evento.salarioChofer(), fecha, "Salario del chofer");
        buses.findById(evento.busId()).ifPresent(bus -> {
            bus.setTotalViajes(bus.getTotalViajes() + 1);
            buses.save(bus);
        });
        if (evento.choferId() != null) {
            choferes.findById(evento.choferId()).ifPresent(chofer -> {
                chofer.setTotalViajes(chofer.getTotalViajes() + 1);
                choferes.save(chofer);
            });
        }
        recalcularChoferActual(evento.busId());
    }

    @Override
    @Transactional
    public void recalcularChoferActual(UUID busId) {
        if (busId == null) {
            return;
        }
        List<UUID> actual = viajes.choferActualDe(busId, Pageable.ofSize(1));
        buses.findById(busId).ifPresent(bus -> {
            bus.setChoferActualId(actual.isEmpty() ? null : actual.get(0));
            buses.save(bus);
        });
    }

    @Override
    @Transactional
    public void aplicarBoletoVendido(UUID eventoId, BoletoVendido evento, LocalDate fechaNegocio) {
        if (evento.sucursalId() == null || evento.montoTotal() == null) {
            log.warn("BoletoVendido {} sin sucursal o monto, se ignora", eventoId);
            return;
        }
        if (ingresos.existsByEventoOrigenId(eventoId)) {
            return;
        }
        IngresoEntidad ingreso = new IngresoEntidad();
        ingreso.setEventoOrigenId(eventoId);
        ingreso.setSucursalId(evento.sucursalId());
        ingreso.setTipo("BOLETO");
        ingreso.setReferenciaId(evento.ventaId());
        ingreso.setViajeId(evento.viajeId());
        ingreso.setRutaId(evento.rutaId());
        ingreso.setBusId(evento.busId());
        ingreso.setClienteNit(evento.clienteNit());
        ingreso.setClienteNombre(evento.clienteNombre());
        ingreso.setOrigen(evento.origenNombre());
        ingreso.setDestino(evento.destinoNombre());
        ingreso.setCantidadBoletos(evento.cantidadBoletos() != null ? evento.cantidadBoletos() : 0);
        ingreso.setMonto(evento.montoTotal());
        ingreso.setFechaNegocio(fechaNegocio != null ? fechaNegocio : LocalDate.now());
        ingresos.save(ingreso);
    }

    @Override
    @Transactional
    public void aplicarVentaCancelada(UUID eventoId, VentaCancelada evento, LocalDate fechaNegocio) {
        if (ingresos.existsByEventoOrigenId(eventoId)) {
            return;
        }
        if (evento.montoAnulado() == null) {
            throw new IllegalStateException("VentaCancelada " + evento.ventaId() + " sin monto");
        }
        // El payload no trae ruta, bus ni origen/destino: se copian de la fila positiva
        List<IngresoEntidad> positivas = ingresos.filasPositivasDe(evento.ventaId());
        if (positivas.isEmpty()) {
            // El BoletoVendido todavía no llegó (desorden): reintentar
            throw new IllegalStateException(
                    "BoletoVendido de " + evento.ventaId() + " todavía no llegó");
        }
        IngresoEntidad original = positivas.get(0);
        IngresoEntidad reembolso = new IngresoEntidad();
        reembolso.setEventoOrigenId(eventoId);
        reembolso.setSucursalId(evento.sucursalId() != null ? evento.sucursalId() : original.getSucursalId());
        reembolso.setTipo("BOLETO");
        reembolso.setReferenciaId(evento.ventaId());
        reembolso.setViajeId(evento.viajeId());
        reembolso.setRutaId(original.getRutaId());
        reembolso.setBusId(original.getBusId());
        reembolso.setClienteNit(evento.clienteNit());
        reembolso.setClienteNombre(evento.clienteNombre());
        reembolso.setOrigen(original.getOrigen());
        reembolso.setDestino(original.getDestino());
        reembolso.setCantidadBoletos(-(evento.cantidadBoletos() != null ? evento.cantidadBoletos() : 0));
        reembolso.setMonto(evento.montoAnulado().negate());
        reembolso.setFechaNegocio(fechaNegocio != null ? fechaNegocio : LocalDate.now());
        ingresos.save(reembolso);
    }

    @Override
    @Transactional
    public void aplicarAlquilerPagado(UUID eventoId, AlquilerPagado evento, LocalDate fechaNegocio) {
        BigDecimal monto = evento.monto() != null ? evento.monto() : evento.montoTotal();
        if (evento.sucursalId() == null || monto == null) {
            log.warn("AlquilerPagado {} sin sucursal o monto, se ignora", eventoId);
            return;
        }
        if (ingresos.existsByEventoOrigenId(eventoId)) {
            return;
        }
        IngresoEntidad ingreso = new IngresoEntidad();
        ingreso.setEventoOrigenId(eventoId);
        ingreso.setSucursalId(evento.sucursalId());
        ingreso.setTipo("ALQUILER");
        ingreso.setReferenciaId(evento.ordenId() != null ? evento.ordenId() : evento.alquilerId());
        ingreso.setClienteNit(evento.clienteNit());
        ingreso.setClienteNombre(evento.clienteNombre());
        ingreso.setOrigen(evento.origen());
        ingreso.setDestino(evento.destino());
        ingreso.setCantidadBoletos(0);
        ingreso.setMonto(monto);
        ingreso.setFechaNegocio(fechaNegocio != null ? fechaNegocio
                : evento.fechaPago() != null ? evento.fechaPago() : LocalDate.now());
        ingresos.save(ingreso);
    }

    @Override
    @Transactional
    public void aplicarGastoTaller(UUID eventoId, GastoTallerRegistrado evento, LocalDate fechaNegocio) {
        if (evento.busId() == null || evento.sucursalId() == null) {
            log.warn("GastoTallerRegistrado {} sin bus o sucursal, se ignora", eventoId);
            return;
        }
        if (evento.lineas() == null || evento.lineas().isEmpty()) {
            log.warn("GastoTallerRegistrado {} sin líneas, se ignora", eventoId);
            return;
        }
        LocalDate fecha = evento.fechaServicio() != null ? evento.fechaServicio()
                : fechaNegocio != null ? fechaNegocio : LocalDate.now();
        for (GastoTallerRegistrado.Linea linea : evento.lineas()) {
            if (linea == null || linea.monto() == null) {
                continue;
            }
            String tipo = linea.tipo() != null ? linea.tipo() : "REPUESTO";
            if (gastos.existe(eventoId, tipo, linea.lineaId(), SIN_DETALLE)) {
                continue;
            }
            GastoEntidad gasto = new GastoEntidad();
            gasto.setEventoOrigenId(eventoId);
            gasto.setSucursalId(evento.sucursalId());
            gasto.setTipo(tipo);
            gasto.setBusId(evento.busId());
            gasto.setServicioTallerId(evento.servicioId());
            gasto.setLineaDetalleId(linea.lineaId());
            gasto.setCategoriaId(linea.categoriaId());
            gasto.setCategoriaNombre(linea.categoriaNombre());
            gasto.setDescripcion(linea.descripcion());
            gasto.setMonto(linea.monto());
            gasto.setFechaNegocio(fecha);
            gastos.save(gasto);
        }
    }

    @Override
    @Transactional
    public void aplicarCategoriaActualizada(UUID categoriaId, String nombre) {
        if (categoriaId == null || nombre == null || nombre.isBlank()) {
            return;
        }
        List<GastoEntidad> afectados = gastos.findByCategoriaIdOrderByFechaNegocioDesc(categoriaId);
        for (GastoEntidad gasto : afectados) {
            gasto.setCategoriaNombre(nombre);
        }
        gastos.saveAll(afectados);
    }

    private String nombreSucursal(UUID sucursalId) {
        if (sucursalId == null) {
            return "-";
        }
        return sucursales.findById(sucursalId).map(SucursalEntidad::getNombre).orElse("-");
    }

    private void guardarGastoSiFalta(UUID eventoId, String tipo, UUID lineaId, UUID sucursalId, UUID busId,
                                     UUID viajeId, BigDecimal monto, LocalDate fecha, String descripcion) {
        if (monto == null || sucursalId == null || busId == null) {
            return;
        }
        if (gastos.existe(eventoId, tipo, lineaId, SIN_DETALLE)) {
            return;
        }
        GastoEntidad gasto = new GastoEntidad();
        gasto.setEventoOrigenId(eventoId);
        gasto.setSucursalId(sucursalId);
        gasto.setTipo(tipo);
        gasto.setBusId(busId);
        gasto.setViajeId(viajeId);
        gasto.setMonto(monto);
        gasto.setFechaNegocio(fecha);
        gasto.setDescripcion(descripcion);
        gastos.save(gasto);
    }
}
