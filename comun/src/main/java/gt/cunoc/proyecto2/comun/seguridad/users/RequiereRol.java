package gt.cunoc.proyecto2.comun.seguridad.users;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige usuario autenticado y, si se indican, alguno de los roles. En el método o en toda la clase.
 *
 * <pre>
 * &#64;RequiereRol({Roles.ADMIN_SUCURSAL, Roles.ADMIN_SISTEMA})
 * &#64;PostMapping("/fleet/buses") ...
 * </pre>
 * Sin usuario → 401. Usuario sin ninguno de los roles → 403.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiereRol {

    /** Roles permitidos; vacío = cualquier usuario autenticado. */
    String[] value() default {};
}
