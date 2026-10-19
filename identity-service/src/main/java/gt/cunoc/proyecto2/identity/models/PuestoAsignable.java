package gt.cunoc.proyecto2.identity.models;

/** Puesto para el que se busca una cuenta existente (lista de cuentas asignables y búsqueda por correo). */
public enum PuestoAsignable {
    /** Administrador de sucursal o cajero. */
    PERSONAL,
    CHOFER,
    /** Solo lo consulta el administrador del sistema. */
    ADMIN_SISTEMA
}
