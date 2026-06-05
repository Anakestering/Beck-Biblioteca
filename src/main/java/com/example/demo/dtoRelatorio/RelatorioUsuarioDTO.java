package com.example.demo.dtoRelatorio;

public record RelatorioUsuarioDTO(
    Long id,
    String nome,
    String email,
    long totalReservas
) {}