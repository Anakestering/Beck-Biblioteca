package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuario_outro_info")
@EqualsAndHashCode(callSuper = false)
public class UsuarioOutroInfo extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "onde_conheceu", length = 200)
    private String ondeConheceu;

    @Column(name = "trabalha", nullable = false)
    private boolean trabalha = false;

    @Column(name = "onde_trabalha", length = 200)
    private String ondeTrabalha;
}