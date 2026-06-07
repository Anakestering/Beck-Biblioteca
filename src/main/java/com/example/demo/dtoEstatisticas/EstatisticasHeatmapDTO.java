package com.example.demo.dtoEstatisticas;


public record EstatisticasHeatmapDTO(
    int diaSemana,
    int hora,
    long totalPrimeiraMetade,
    long totalSegundaMetade,
    long valorParaCor,
    long media
) {}