package gt.cunoc.proyecto2.identity.services.tarifa;


import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.identity.dtos.tarifa.CreateDepreciationRateRequest;
import gt.cunoc.proyecto2.identity.dtos.tarifa.DepreciationRateResponse;
import gt.cunoc.proyecto2.identity.evento.payload.TarifaDepreciacionPayload;
import gt.cunoc.proyecto2.identity.models.TarifaDepreciacion;
import gt.cunoc.proyecto2.identity.repositories.TarifaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;


/**
 * Altas y consulta de vigencia de la tarifa de depreciación.
 */
@Service
public class TarifaServiceImpl implements TarifaService {

    private final TarifaRepository tarifas;

    private final PublicadorEventos publicador;

    public TarifaServiceImpl(TarifaRepository tarifas, PublicadorEventos publicador) {
        this.tarifas = tarifas;
        this.publicador = publicador;
    }

    @Override
    @Transactional
    public DepreciationRateResponse crear(CreateDepreciationRateRequest solicitud, UUID creadoPor) {
        TarifaDepreciacion tarifa = new TarifaDepreciacion();
        tarifa.setMontoPorKm(solicitud.montoPorKm());
        tarifa.setVigenteDesde(solicitud.vigenteDesde());
        tarifa.setCreadoPor(creadoPor);
        tarifas.save(tarifa);
        publicador.publicar(
                "TarifaDepreciacion",
                tarifa.getId(),
                "TarifaDepreciacionCambiada",
                new TarifaDepreciacionPayload(tarifa.getId(), tarifa.getMontoPorKm(), tarifa.getVigenteDesde()),
                solicitud.vigenteDesde());
        return aRespuesta(tarifa);
    }

    @Override
    @Transactional(readOnly = true)
    public DepreciationRateResponse vigente(LocalDate fecha) {
        LocalDate dia = fecha == null ? LocalDate.now() : fecha;
        TarifaDepreciacion tarifa = tarifas.findTopByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(dia)
                .orElseThrow(() -> new NoEncontradoException("TARIFA_NO_EXISTE", "No hay tarifa vigente"));
        return aRespuesta(tarifa);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepreciationRateResponse> listar(Pageable paginado) {
        return tarifas.findAll(paginado).map(this::aRespuesta);
    }

    private DepreciationRateResponse aRespuesta(TarifaDepreciacion tarifa) {
        return new DepreciationRateResponse(tarifa.getId(), tarifa.getMontoPorKm(), tarifa.getVigenteDesde());
    }
}
