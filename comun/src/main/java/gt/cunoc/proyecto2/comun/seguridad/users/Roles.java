package gt.cunoc.proyecto2.comun.seguridad.users;

public final class Roles {

    public static final String ADMIN_SISTEMA = "ADMIN_SISTEMA";
    public static final String ADMIN_SUCURSAL = "ADMIN_SUCURSAL";
    public static final String CAJERO = "CAJERO";
    public static final String CHOFER = "CHOFER";
    public static final String CLIENTE = "CLIENTE";

    /**
     * Rol del token de servicio (llamadas internas R1–R5). Ninguna persona lo tiene: solo lo genera
     * {@code ServicioJwt.generarServicio(...)}. Los endpoints {@code /<prefijo>/interno/**} usan
     * {@code @RequiereRol(Roles.SERVICIO)}.
     */
    public static final String SERVICIO = "SERVICIO";

    private Roles() {
    }
}