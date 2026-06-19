package com.example.demo.dtoEstatisticas;

public record DistribuicaoTipoDTO(
    String tipo,
    long usuariosFinalizados,
    long pedidosFinalizados,
    double mediaVisitas,
    long usuariosAbandonos,
    long totalAbandonos,
    long usuariosCancelamentos,
    long totalCancelamentos
) {}
