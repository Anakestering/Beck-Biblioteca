package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComputadorResponseDTO {
    private Long id;
    private String codigo;
    private int capacidadePessoas;
    private String observacao;
    private boolean ativo;
}