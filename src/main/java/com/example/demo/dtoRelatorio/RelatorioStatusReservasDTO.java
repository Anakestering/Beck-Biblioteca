package com.example.demo.dtoRelatorio;

public record RelatorioStatusReservasDTO(
    long finalizadas,
    long canceladas,
    long atrasadas,
    long rejeitadas,
    long total
) {}