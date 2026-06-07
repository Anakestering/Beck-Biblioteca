package com.example.demo.dtoEstatisticas;

public record EstatisticasUsuarioDTO(
    Long id,
    String nome,
    String email,
    long totalReservas
) {}