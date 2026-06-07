package com.example.demo.dtoEstatisticas;

public record EstatisticasRecursoDTO(
    Long id,
    String nome,
    long totalMinutosUsados,
    long totalReservasFinalizadas
) {}