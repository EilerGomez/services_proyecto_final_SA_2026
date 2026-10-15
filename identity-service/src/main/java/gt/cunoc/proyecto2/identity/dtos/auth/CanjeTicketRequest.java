package gt.cunoc.proyecto2.identity.dtos.auth;

import jakarta.validation.constraints.NotBlank;

public record CanjeTicketRequest(
        @NotBlank
        String ticket) {
}
