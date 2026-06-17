package com.example.demo.dto;

public record UsuarioStatsDTO(
    long total,
    long ativos,
    long cadastradosNaSemana
) {}