package com.example.demo.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioOutroInfoDTO {
    private String ondeConheceu;
    private boolean trabalha;
    private String ondeTrabalha;
}