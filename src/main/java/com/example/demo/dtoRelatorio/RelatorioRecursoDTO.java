package com.example.demo.dtoRelatorio;

public record RelatorioRecursoDTO(
    Long id,
    String nome,
    long totalMinutosUsados,
    long totalReservasFinalizadas
) {}