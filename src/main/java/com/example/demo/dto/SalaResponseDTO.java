package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalaResponseDTO {
    private Long id;
    private String nome;
    private int capacidadePessoas;
    private boolean ativo;
}