package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComputadorDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank(message = "O código do computador deve ser preenchido.")
    private String codigo;

    @NotNull(message = "A capacidade deve ser informada.")
    @Min(value = 1, message = "Capacidade mínima é 1.")
    @Max(value = 2, message = "Capacidade máxima de um computador é 2.")
    private Integer capacidadePessoas = 2;

    private String observacao;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean ativo;
}