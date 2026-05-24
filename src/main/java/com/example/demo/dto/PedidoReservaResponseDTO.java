package com.example.demo.dto;

import com.example.demo.enums.StatusReserva;
import com.example.demo.enums.TipoPedido;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PedidoReservaResponseDTO {
    private Long id;
    private UsuarioResponseDTO usuario;
    private UsuarioResponseDTO criadaPorUsuario;
    private TipoPedido tipo;
    private StatusReserva status;
    private LocalDateTime inicioPrevisto;
    private LocalDateTime fimPrevisto;
    private int qtdePessoas;
    private String observacao;
    private List<ReservaComputadorResponseDTO> reservasComputador;
    private List<ReservaSalaResponseDTO> reservasSala;
}