package gt.cunoc.proyecto2.sales.repositorios.tarifas;

import gt.cunoc.proyecto2.sales.modelos.tarifas.EstadoAlquilerTarifa;
import gt.cunoc.proyecto2.sales.modelos.tarifas.TarifaReembolsoEntidad;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TarifaReembolsoRepositorio extends JpaRepository<TarifaReembolsoEntidad, UUID> {

    /** Pantalla de administración: todas, incluidas las desactivadas. */
    List<TarifaReembolsoEntidad> findAllByOrderByEstadoAlquilerAscHorasMinimasAntesDesc();

    /**
     * Candidatas a cubrir una cancelación: el estado del alquiler o el comodín. La fila ganadora se
     * elige en el servicio (son pocas filas y la regla de desempate se lee mejor en Java que en SQL).
     */
    List<TarifaReembolsoEntidad> findByActivaTrueAndEstadoAlquilerIn(List<EstadoAlquilerTarifa> estados);

    /** El UNIQUE de la tabla: no puede haber dos filas para el mismo estado y el mismo tramo. */
    Optional<TarifaReembolsoEntidad> findByEstadoAlquilerAndHorasMinimasAntes(EstadoAlquilerTarifa estado,
                                                                             int horasMinimasAntes);
}
