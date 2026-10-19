package api_gateway.constants;

public final class ErrorCodes {
    public static final String UNAUTHORIZED = "TOKEN_INVALIDO";
    /** El JWT se emitió antes de que retiraran o desactivaran a la persona: debe iniciar sesión de nuevo. */
    public static final String SESION_REVOCADA = "SESION_REVOCADA";
    public static final String FORBIDDEN = "PROHIBIDO";
    public static final String NOT_FOUND = "RUTA_INEXISTENTE";
    public static final String SERVICE_UNAVAILABLE = "SERVICIO_NO_DISPONIBLE";
    public static final String GATEWAY_TIMEOUT = "TIEMPO_AGOTADO";
    public static final String INTERNAL_ERROR = "ERROR_INTERNO";

    private ErrorCodes() { }
}
