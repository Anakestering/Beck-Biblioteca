package com.example.demo.dtoEstatisticas;

public record EstatisticasOcupacaoDiaDTO (
    int diaSemana,       // 1=Seg, 2=Ter, 3=Qua, 4=Qui, 5=Sex
    String nome,         // "Seg", "Ter", ...
    double taxaOcupacao
){}