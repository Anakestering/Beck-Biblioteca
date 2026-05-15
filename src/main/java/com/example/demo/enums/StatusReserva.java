package com.example.demo.enums;

public enum StatusReserva {
    PENDENTE_APROVACAO,  // reserva > 3h aguardando admin
    APROVADA,            // confirmada, aguardando check-in
    CANCELADA,           // cancelada pelo usuário (até 1h antes)
    ATRASADO,             // não fez check-in em 15min após início
    EM_ANDAMENTO,        // check-in realizado
    FINALIZADA,          // check-out manual ou automático
    REJEITADA            // reprovada pelo admin
}