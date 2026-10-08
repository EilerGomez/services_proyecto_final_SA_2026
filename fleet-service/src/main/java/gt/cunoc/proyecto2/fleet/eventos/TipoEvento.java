package gt.cunoc.proyecto2.fleet.eventos;

public enum TipoEvento {

    // ----- Publica Fleet (exchange fleet.eventos) -----
    SUCURSAL_CREADA("SucursalCreada"),
    SUCURSAL_ACTUALIZADA("SucursalActualizada"),
    PERSONAL_SUCURSAL_ASIGNADO("PersonalSucursalAsignado"),
    PERSONAL_SUCURSAL_DESACTIVADO("PersonalSucursalDesactivado"),
    BUS_CREADO("BusCreado"),
    BUS_ACTUALIZADO("BusActualizado"),
    BUS_DESACTIVADO("BusDesactivado"),
    CHOFER_CREADO("ChoferCreado"),
    CHOFER_ACTUALIZADO("ChoferActualizado"),
    CHOFER_DESACTIVADO("ChoferDesactivado"),
    LICENCIA_CHOFER_ACTUALIZADA("LicenciaChoferActualizada"),

    // ----- Consume Fleet (cola fleet.q) -----
    PERFIL_ACTUALIZADO("PerfilActualizado"),
    VIAJE_INICIADO("ViajeIniciado"),
    VIAJE_COMPLETADO("ViajeCompletado");

    private final String nombre;

    TipoEvento(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}