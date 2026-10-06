package api_gateway.constants;

public final class PublicEndpoints {
    public static final String[] PATHS = {
        ApiPaths.HEALTH, ApiPaths.SWAGGER_HTML, ApiPaths.SWAGGER_UI,
        ApiPaths.WEBJARS, ApiPaths.API_DOCS,
        // Auth de identity (I1–I6 públicos)
        "/identity/auth/registro", "/identity/auth/login", "/identity/auth/refrescar",
        "/identity/auth/recuperar", "/identity/auth/restablecer",
    };

    private PublicEndpoints() { }
}
