package com.example.demo.dtoEstatisticas;

public record RankingUsuarioDTO(
    Long id,
    String nome,
    String tipoUsuario,
    String cpf,
    long pedidosFinalizados,
    long pedidosCancelados,
    long pedidosAbandono,
    double taxaAbandono
) {}
