
package com.example.demo.dtoEstatisticas;

public record EstatisticasResumoDTO(
    long totalPedidos,          // pedidos de reserva finalizados no período (métrica principal)
    long totalReservas,         // recursos individuais (salas + PCs) efetivamente utilizados
    double taxaOcupacaoMedia,   // % média de ocupação dos recursos no período
    double taxaNoShow,          // % de pedidos que viraram ATRASADO (sem checkin)
    String recursoMaisUsado,    // nome do PC ou sala com mais minutos usados
    String tipoRecursoMaisUsado // "PC" ou "SALA"
) {}