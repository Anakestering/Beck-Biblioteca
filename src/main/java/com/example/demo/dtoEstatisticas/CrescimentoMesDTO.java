package com.example.demo.dtoEstatisticas;

public record CrescimentoMesDTO(
    String mes,
    long novosCadastros,
    long primeiroUso
) {}
