package com.example.demo.dto;

import com.example.demo.enums.TipoPedido;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PedidoReservaDTO {

    @NotNull
    private TipoPedido tipo;

    @NotEmpty(message = "Selecione pelo menos um item.")
    private List<Long> itemIds;

    private Long usuarioId;

    @NotNull(message = "O início deve ser informado.")
    private LocalDateTime inicioPrevisto;

    @NotNull(message = "O fim deve ser informado.")
    private LocalDateTime fimPrevisto;

    @Min(1) @Max(20)
    private int qtdePessoas;

    private String observacao;
}