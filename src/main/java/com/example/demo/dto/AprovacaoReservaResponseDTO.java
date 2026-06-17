package com.example.demo.dto;

import com.example.demo.enums.StatusAprovacao;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AprovacaoReservaResponseDTO {
    private Long id;
    private PedidoReservaResponseDTO pedido;
    private StatusAprovacao status;
    private LocalDateTime solicitadaEm;
    private LocalDateTime decididaEm;
    private UsuarioResponseDTO decididaPorUsuario;
    private String motivo;
}