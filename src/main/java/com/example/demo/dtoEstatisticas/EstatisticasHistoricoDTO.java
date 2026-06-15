package com.example.demo.dtoEstatisticas;

import java.util.List;

/**
 * Resposta do endpoint GET /estatisticas/historico.
 * Retorna os pontos do gráfico linear já com a média móvel calculada,
 * mais a tendência do período e os dados de abandono (ATRASADO).
 */
public record EstatisticasHistoricoDTO(
    List<Ponto> pontos,
    List<PontoAbandono> abandonos,   // pontos de ATRASADO agrupados por data
    Tendencia tendencia,             // null se período tem menos de 4 pontos
    Tendencia tendenciaAbandono,     // null se menos de 4 pontos de abandono
    double mediaPessoasDia,
    double taxaAbandono              // % de ATRASADO sobre (FINALIZADA + ATRASADO) no período
) {
    public record Ponto(
        String data,          // "yyyy-MM-dd"
        long total,           // pedidos finalizados (linha do gráfico)
        Double mm,            // média móvel — null nos primeiros (janela - 1) pontos
        long totalReservas    // recursos individuais utilizados (salas + PCs) — contexto tooltip
    ) {}

    public record PontoAbandono(
        String data,
        long total,
        Double mm
    ) {}

    public record Tendencia(
        double pct,
        boolean subindo
    ) {}
}