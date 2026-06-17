package com.example.demo.dto;

import com.example.demo.enums.StatusReserva;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservaComputadorResponseDTO {
    private Long id;
    private ComputadorResponseDTO computador;
    private StatusReserva status;
    private LocalDateTime inicioPrevisto;
    private LocalDateTime fimPrevisto;
    private int qtdePessoas;
    private String observacao;
    private LocalDateTime checkinEm;
    private LocalDateTime checkoutEm;
    private LocalDateTime canceladaEm;
    private LocalDateTime atrasadoEm;
    private boolean checkoutAutomatico;
}