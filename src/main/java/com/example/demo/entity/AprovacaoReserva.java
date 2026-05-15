package com.example.demo.entity;

import com.example.demo.enums.StatusAprovacao;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "aprovacao_reserva")
@EqualsAndHashCode(callSuper = false)
public class AprovacaoReserva extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_sala_id", unique = true)
    private ReservaSala reservaSala;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_computador_id", unique = true)   // era reserva_pc_id
    private ReservaComputador reservaComputador;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusAprovacao status = StatusAprovacao.PENDENTE;

    @Column(name = "solicitada_em", nullable = false)
    private LocalDateTime solicitadaEm;

    @Column(name = "decidida_em")
    private LocalDateTime decididaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decidida_por_usuario_id")
    private Usuario decididaPorUsuario;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;
}