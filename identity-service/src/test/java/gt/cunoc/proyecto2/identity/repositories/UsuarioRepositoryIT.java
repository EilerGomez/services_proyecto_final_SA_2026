package gt.cunoc.proyecto2.identity.repositories;

import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Usuario;
import jakarta.persistence.EntityManagerFactory;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regresion de filtros opcionales contra PostgreSQL con el esquema de identity existente.
 * Activar con IDENTITY_TEST_DB_URL, IDENTITY_TEST_DB_USER e IDENTITY_TEST_DB_PASSWORD.
 * La conexion impone transacciones de solo lectura; no crea ni modifica datos o tablas.
 */
@EnabledIfEnvironmentVariable(named = "IDENTITY_TEST_DB_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UsuarioRepositoryIT {

    private LocalContainerEntityManagerFactoryBean fabrica;
    private UsuarioRepository usuarios;
    private PerfilRepository perfiles;
    private TransactionTemplate lectura;

    @BeforeAll
    void conectar() {
        DriverManagerDataSource fuente = new DriverManagerDataSource();
        fuente.setUrl(System.getenv("IDENTITY_TEST_DB_URL"));
        fuente.setUsername(System.getenv("IDENTITY_TEST_DB_USER"));
        fuente.setPassword(System.getenv("IDENTITY_TEST_DB_PASSWORD"));
        Properties propiedades = new Properties();
        propiedades.setProperty("options", "-c default_transaction_read_only=on");
        fuente.setConnectionProperties(propiedades);

        fabrica = new LocalContainerEntityManagerFactoryBean();
        fabrica.setDataSource(fuente);
        fabrica.setPackagesToScan("gt.cunoc.proyecto2.identity.models");
        fabrica.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        fabrica.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none"));
        fabrica.afterPropertiesSet();
        EntityManagerFactory emf = fabrica.getObject();
        JpaRepositoryFactory repositorios = new JpaRepositoryFactory(
                SharedEntityManagerCreator.createSharedEntityManager(emf));
        usuarios = repositorios.getRepository(UsuarioRepository.class);
        perfiles = repositorios.getRepository(PerfilRepository.class);
        lectura = new TransactionTemplate(new JpaTransactionManager(emf));
        lectura.setReadOnly(true);
    }

    @AfterAll
    void cerrar() {
        if (fabrica != null) fabrica.destroy();
    }

    static Stream<Arguments> filtros() {
        return Stream.of(null, "CLIENTE").flatMap(rol ->
                Stream.of(null, "ACTIVO").flatMap(estado ->
                        Stream.of(null, "a").map(q -> Arguments.of(rol, estado, q))));
    }

    @ParameterizedTest(name = "rol={0}, estado={1}, q={2}")
    @MethodSource("filtros")
    void filtrosOpcionalesConservanResultadosYConteo(String rol, String estado, String q) {
        lectura.executeWithoutResult(tx -> {
            Map<UUID, Perfil> perfilesPorId = perfiles.findAll().stream()
                    .collect(Collectors.toMap(Perfil::getUsuarioId, Function.identity()));
            List<UUID> esperados = usuarios.findAll().stream()
                    .filter(u -> rol == null || u.getRoles().stream().anyMatch(r -> rol.equals(r.getCodigo())))
                    .filter(u -> estado == null || estado.equals(u.getEstado()))
                    .filter(u -> coincide(u, perfilesPorId.get(u.getId()), q))
                    .map(Usuario::getId)
                    .sorted(Comparator.comparing(UUID::toString))
                    .toList();

            var pagina = usuarios.buscar(rol, estado, q, PageRequest.of(0, 1, Sort.by("id")));
            assertThat(pagina.getTotalElements()).isEqualTo(esperados.size());
            assertThat(pagina.getContent()).extracting(Usuario::getId)
                    .containsExactlyElementsOf(esperados.stream().limit(1).toList());
            var segunda = usuarios.buscar(rol, estado, q, PageRequest.of(1, 1, Sort.by("id")));
            assertThat(segunda.getTotalElements()).isEqualTo(esperados.size());
            assertThat(segunda.getContent()).extracting(Usuario::getId)
                    .containsExactlyElementsOf(esperados.stream().skip(1).limit(1).toList());
        });
    }

    private boolean coincide(Usuario usuario, Perfil perfil, String q) {
        return q == null || contiene(usuario.getCorreo(), q)
                || perfil != null && (contiene(perfil.getNombres(), q) || contiene(perfil.getApellidos(), q));
    }

    private boolean contiene(String valor, String q) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT));
    }
}
