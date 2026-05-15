package com.example.demo.entity;

import com.example.demo.enums.StatusReserva;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reserva_sala")
@EqualsAndHashCode(callSuper = false)
public class ReservaSala extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    // Dono da reserva (para quem foi feita)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Quem criou (pode ser admin criando para outro)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criada_por_usuario_id", nullable = false)
    private Usuario criadaPorUsuario;

    @Column(name = "inicio_previsto", nullable = false)
    private LocalDateTime inicioPrevisto;

    // Sempre inicio + 45 min, calculado no service
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
    private LocalDateTime noShowEm;

    @Column(name = "checkin_em")
    private LocalDateTime checkinEm;

    @Column(name = "checkout_em")
    private LocalDateTime checkoutEm;

    @Column(name = "checkout_automatico", nullable = false)
    private boolean checkoutAutomatico = false;
}