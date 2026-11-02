package api_gateway.constants;

/**
 * Paths y segmentos compartidos por las rutas, los endpoints públicos y los filtros del gateway.
 * Las rutas de negocio se declaran en application.yml, estos valores evitan repetir prefijos
 * cuando otra clase necesita reconocer recursos de infraestructura o bloquear APIs internas.
 */
public final class ApiPaths {
    // Prefijo público que enruta al servicio de identidad.
    public static final String IDENTITY = "/identity";
    // Prefijo público que enruta al servicio de flota.
    public static final String FLEET = "/fleet";
    // Prefijo público que enruta al servicio de viajes.
    public static final String TRIPS = "/trips";
    // Prefijo público que enruta al servicio de ventas.
    public static final String SALES = "/sales";
    // Prefijo público que enruta al servicio de mantenimiento.
    public static final String MAINTENANCE = "/maintenance";
    // Prefijo público que enruta al servicio de reportes.
    public static final String REPORTS = "/reports";
    // Prefijo público que enruta al servicio de notificaciones.
    public static final String NOTIFICATIONS = "/notifications";
    // Prefijo público que enruta al servicio de cartera.
    public static final String WALLET = "/wallet";
    // Prefijos de los 8 microservicios (rutas del gateway en application.yml).
    public static final String[] PREFIJOS = {
        IDENTITY, FLEET, TRIPS, SALES, MAINTENANCE, REPORTS, NOTIFICATIONS, WALLET,
    };
    public static final String HEALTH = "/actuator/health";
    public static final String HEALTH_ALL = HEALTH + "/**";
    public static final String ACTUATOR = "/actuator/**";
    public static final String SWAGGER_HTML = "/swagger-ui.html";
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String WEBJARS = "/webjars/**";
    public static final String API_DOCS = "/v3/api-docs/**";
    public static final String INTERNO = "interno";

    // Evita crear instancias de esta clase.
    private ApiPaths() { }
}
