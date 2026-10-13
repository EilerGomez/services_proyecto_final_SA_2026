package api_gateway.constants;

public final class ApiPaths {
    public static final String IDENTITY = "/identity";
    public static final String FLEET = "/fleet";
    public static final String TRIPS = "/trips";
    public static final String SALES = "/sales";
    public static final String MAINTENANCE = "/maintenance";
    public static final String REPORTS = "/reports";
    public static final String NOTIFICATIONS = "/notifications";
    /** Prefijos de los 7 microservicios (rutas del gateway en application.yml). */
    public static final String[] PREFIJOS = {
        IDENTITY, FLEET, TRIPS, SALES, MAINTENANCE, REPORTS, NOTIFICATIONS,
    };
    public static final String HEALTH = "/actuator/health";
    /** Health y sus grupos (/actuator/health/liveness, etc.). También coincide con /actuator/health. */
    public static final String HEALTH_ALL = HEALTH + "/**";
    public static final String ACTUATOR = "/actuator/**";
    public static final String SWAGGER_HTML = "/swagger-ui.html";
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String WEBJARS = "/webjars/**";
    /** También coincide con /v3/api-docs (el ** acepta cero segmentos). */
    public static final String API_DOCS = "/v3/api-docs/**";
    /** Segmento de las rutas de servicio a servicio: /&lt;prefijo&gt;/interno/** (R1–R5). */
    public static final String INTERNO = "interno";

    private ApiPaths() { }
}
