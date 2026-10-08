package gt.cunoc.proyecto2.fleet.modelos.buses;

/** Estados de un bus (columna buses.estado). */
public enum EstadoBus {

    ACTIVO,
    EN_RUTA,
    EN_TALLER,
    INACTIVO;

    public boolean seAsignaManualmente() {
        return this != EN_RUTA;
    }

    public boolean disponibleParaViajes() {
        return this == ACTIVO || this == EN_RUTA;
    }
}