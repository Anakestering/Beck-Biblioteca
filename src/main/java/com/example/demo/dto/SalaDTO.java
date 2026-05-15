package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

// ─── SalaDTO ─────────────────────────────────────────────────────────────────

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalaDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank(message = "O nome da sala deve ser preenchido.")
    private String nome;

    @Min(value = 1, message = "Capacidade mínima é 1.")
    @Max(value = 5, message = "Capacidade máxima de uma sala é 5.")
    private int capacidadePessoas = 5;
}