/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.repositorios.sucursales;

import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SucursalRepositorio extends JpaRepository<SucursalEntidad, UUID> {

    List<SucursalEntidad> findAllByOrderByNombreAsc();

    List<SucursalEntidad> findByActivaOrderByNombreAsc(boolean activa);

    boolean existsByCodigo(String codigo);

    boolean existsByNombre(String nombre);

    /** Para editar: otro registro (distinto de id) ya usa ese nombre. */
    boolean existsByNombreAndIdNot(String nombre, UUID id);
}
