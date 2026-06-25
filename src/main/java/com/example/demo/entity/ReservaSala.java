package com.example.demo.entity;

import com.example.demo.enums.StatusReserva;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reserva_sala", indexes = {
    @Index(name = "idx_rs_ativo_status_checkin",  columnList = "ativo, status, checkin_em, checkout_em"),
    @Index(name = "idx_rs_ativo_status_inicio",   columnList = "ativo, status, inicio_previsto"),
    @Index(name = "idx_rs_heatmap",               columnList = "usuario_id, sala_id, checkin_em, inicio_previsto, status, ativo"),
})
@EqualsAndHashCode(callSuper = false)
public class ReservaSala extends BaseEntity {

    @JsonIgnoreProperties({ "reservasComputador", "reservasSala", "usuario", "criadaPorUsuario" })
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private PedidoReserva pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criada_por_usuario_id", nullable = false)
    private Usuario criadaPorUsuario;

    @Column(name = "inicio_previsto", nullable = false)
    private LocalDateTime inicioPrevisto;

    @Column(name = "fim_previsto", nullable = false)
    private LocalDateTime fimPrevisto;

    @Column(name = "qtde_pessoas", nullable = false)
    private int qtdePessoas;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusReserva status = StatusReserva.APROVADA;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "cancelada_em")
    private LocalDateTime canceladaEm;

    @Column(name = "atrasado_em")
    private LocalDateTime atrasadoEm;

    @Column(name = "checkin_em")
    private LocalDateTime checkinEm;

    @Column(name = "checkout_em")
    private LocalDateTime checkoutEm;

    @Column(name = "checkout_automatico", nullable = false)
    private boolean checkoutAutomatico = false;
}