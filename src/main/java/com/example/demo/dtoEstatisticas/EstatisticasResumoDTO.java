
package com.example.demo.dtoEstatisticas;

public record EstatisticasResumoDTO(
    long totalReservas,
    double taxaOcupacaoMedia,   // % média de ocupação dos recursos no período
    double taxaNoShow,          // % de reservas que viraram ATRASADO (sem checkin)
    String recursoMaisUsado,    // nome do PC ou sala com mais minutos usados
    String tipoRecursoMaisUsado // "PC" ou "SALA"
) {}