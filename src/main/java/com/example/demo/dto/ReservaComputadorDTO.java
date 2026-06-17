package com.example.demo.dto;

import com.example.demo.enums.StatusReserva;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservaComputadorDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull(message = "O ID do computador deve ser informado.")
    private Long computadorId;

    private Long usuarioId;

    @NotNull(message = "O início da reserva deve ser informado.")
    private LocalDateTime inicioPrevisto;

    private LocalDateTime fimPrevisto;

    @Min(value = 1, message = "Mínimo de 1 pessoa.")
    @Max(value = 2, message = "Máximo de 2 pessoas por computador.")
    private int qtdePessoas;

    private String observacao;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private StatusReserva status;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime checkinEm;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime checkoutEm;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime canceladaEm;
}