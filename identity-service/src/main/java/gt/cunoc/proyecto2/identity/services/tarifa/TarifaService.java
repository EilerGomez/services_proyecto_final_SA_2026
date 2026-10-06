package gt.cunoc.proyecto2.identity.services.tarifa;


import gt.cunoc.proyecto2.identity.dtos.tarifa.CreateDepreciationRateRequest;
import gt.cunoc.proyecto2.identity.dtos.tarifa.DepreciationRateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;


/**
 * Tarifa de depreciación por km (solo altas, nunca se edita).
 */
public interface TarifaService {


    DepreciationRateResponse crear(CreateDepreciationRateRequest request, UUID creadoPor);


    DepreciationRateResponse vigente(LocalDate fecha);


    Page<DepreciationRateResponse> listar(Pageable paginado);

}
