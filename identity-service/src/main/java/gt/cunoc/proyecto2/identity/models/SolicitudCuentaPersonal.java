package gt.cunoc.proyecto2.identity.models;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


/**
 * Idempotencia de R5: Fleet genera el id antes de llamar y lo envía como solicitudId.
 */
@Getter
@Setter
@Entity
@Table(name = "solicitudes_cuenta_personal")
public class SolicitudCuentaPersonal {

    @Id
    @Column(name = "solicitud_id")
    private UUID solicitudId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "rol_codigo", length = 30, nullable = false)
    private String rolCodigo;

    @Column(name = "sucursal_id", nullable = false)
    private UUID sucursalId;
}
