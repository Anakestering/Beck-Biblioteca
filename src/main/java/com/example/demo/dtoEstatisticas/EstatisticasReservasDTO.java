package com.example.demo.dtoEstatisticas;

public record EstatisticasReservasDTO(
    long finalizadas,
    long canceladas,
    long atrasadas,
    long rejeitadas,
    long total
) {}