package gt.cunoc.proyecto2.fleet.servicios.bitacora;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.modelos.bitacora.BitacoraKilometrajeEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BitacoraKilometrajeServicio {

    List<BitacoraKilometrajeEntidad> ultimosViajes(UUID busId);

    List<BitacoraKilometrajeEntidad> listar(UUID busId, LocalDate desde, LocalDate hasta, UsuarioActual usuario);

    boolean registrarViajeCompletado(UUID busId, UUID viajeId, BigDecimal kmInicial, BigDecimal kmFinal,
                                     OffsetDateTime fechaLlegada);
}