package com.example.demo.dtoRelatorio;

public record RelatorioHeatmapDTO(
    int diaSemana,
    int hora,
    long total,
    long media
) {}