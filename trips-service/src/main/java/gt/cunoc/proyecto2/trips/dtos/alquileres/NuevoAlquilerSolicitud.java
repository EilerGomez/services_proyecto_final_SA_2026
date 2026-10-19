package gt.cunoc.proyecto2.trips.dtos.alquileres;

import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.CanalAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.ClienteAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.RecorridoAlquiler;
import gt.cunoc.proyecto2.trips.utilidades.Normalizador;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cuerpo de POST /trips/alquileres.
 * CLIENTE (EN_LINEA): el alquiler queda a nombre de su cuenta; sin correo, se usa el del token.
 * CAJERO (PRESENCIAL): sucursalId debe ser la suya; clienteNit vacío = "CF".
 * fechaRetorno null = solo ida. fechaSolicitud: fecha elegida por el usuario (fija el precio por km).
 */
public record NuevoAlquilerSolicitud(
        @NotNull UUID sucursalId,
        @Size(max = 20) String clienteNit,
        @NotBlank @Size(max = 150) String clienteNombre,
        @Email @Size(max = 150) String clienteCorreo,
        @NotBlank @Size(max = 255) String origen,
        @NotBlank @Size(max = 255) String destino,
        @NotNull OffsetDateTime fechaSalida,
        OffsetDateTime fechaRetorno,
        @NotNull @Min(1) @Max(120) Integer cantidadPasajeros,
        @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2) BigDecimal kmEstimados,
        @NotNull LocalDate fechaSolicitud,
        // Coordenadas opcionales que el cliente marca en el mapa (origen y destino).
        @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 6) BigDecimal origenLatitud,
        @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 6) BigDecimal origenLongitud,
        @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 6) BigDecimal destinoLatitud,
        @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 6) BigDecimal destinoLongitud) {

    public NuevoAlquilerSolicitud {
        clienteNit = Normalizador.codigo(Normalizador.textoOpcional(clienteNit));
        clienteNombre = Normalizador.texto(clienteNombre);
        clienteCorreo = Normalizador.correo(Normalizador.textoOpcional(clienteCorreo));
        origen = Normalizador.texto(origen);
        destino = Normalizador.texto(destino);
    }

    /** 400 RETORNO_ANTES_DE_SALIDA si el retorno es anterior a la salida. */
    public RecorridoAlquiler recorrido() {
        return new RecorridoAlquiler(origen, destino, fechaSalida, fechaRetorno, cantidadPasajeros, kmEstimados,
                origenLatitud, origenLongitud, destinoLatitud, destinoLongitud);
    }

    /**
     * clienteUsuarioId: el del token si es EN_LINEA, null si es PRESENCIAL.
     * correoDelToken: se usa si el cliente EN_LINEA no escribió otro correo.
     */
    public AlquilerPrivadoEntidad crearEntidad(CanalAlquiler canal, UUID clienteUsuarioId, String correoDelToken,
                                               BigDecimal precioEstimado, UUID solicitadoPor) {
        String correo = clienteCorreo == null && canal == CanalAlquiler.EN_LINEA ? correoDelToken : clienteCorreo;
        ClienteAlquiler cliente = new ClienteAlquiler(clienteUsuarioId, clienteNit, clienteNombre, correo);
        return AlquilerPrivadoEntidad.solicitar(sucursalId, canal, cliente, recorrido(), precioEstimado,
                fechaSolicitud, solicitadoPor);
    }
}