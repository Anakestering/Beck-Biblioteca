package com.example.demo.entity;

import com.example.demo.enums.StatusReserva;
import com.example.demo.enums.TipoPedido;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pedido_reserva")
@EqualsAndHashCode(callSuper = false)
public class PedidoReserva extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criada_por_usuario_id", nullable = false)
    private Usuario criadaPorUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoPedido tipo;

    @Column(name = "inicio_previsto", nullable = false)
    private LocalDateTime inicioPrevisto;

    @Column(name = "fim_previsto", nullable = false)
    private LocalDateTime fimPrevisto;

    @Column(name = "qtde_pessoas", nullable = false)
    private int qtdePessoas;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusReserva status = StatusReserva.APROVADA;

    @JsonIgnoreProperties({ "pedido" })
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReservaComputador> reservasComputador = new ArrayList<>();

    @JsonIgnoreProperties({ "pedido" })
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReservaSala> reservasSala = new ArrayList<>();
}