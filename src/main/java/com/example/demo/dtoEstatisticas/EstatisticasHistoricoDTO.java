package com.example.demo.dtoEstatisticas;

import java.util.List;

/**
 * Resposta do endpoint GET /estatisticas/historico.
 * Retorna os pontos do gráfico linear já com a média móvel calculada,
 * mais a tendência do período (comparação primeira vs segunda metade).
 */
public record EstatisticasHistoricoDTO(
    List<Ponto> pontos,
    Tendencia tendencia,      // null se período tem menos de 4 pontos
    double mediaPessoasDia    // média de pessoas por dia útil no período
) {
    public record Ponto(
        String data,   // "yyyy-MM-dd"
        long total,
        Double mm      // média móvel — null nos primeiros (janela - 1) pontos
    ) {}

    public record Tendencia(
        double pct,
        boolean subindo
    ) {}
}