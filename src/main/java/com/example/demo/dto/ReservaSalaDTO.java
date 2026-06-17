package com.example.demo.dto;

import com.example.demo.enums.StatusReserva;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservaSalaDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull(message = "O ID da sala deve ser informado.")
    private Long salaId;

    private Long usuarioId;

    @NotNull(message = "O início da reserva deve ser informado.")
    private LocalDateTime inicioPrevisto;

    private LocalDateTime fimPrevisto;

    @Min(value = 1, message = "Mínimo de 1 pessoa.")
    @Max(value = 5, message = "Máximo de 5 pessoas para sala.")
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