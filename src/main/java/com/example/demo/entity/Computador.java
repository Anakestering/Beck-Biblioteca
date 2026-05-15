package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "computador")
@EqualsAndHashCode(callSuper = false)
public class Computador extends BaseEntity {

    @Column(name = "codigo", nullable = false, unique = true)
    private String codigo;

    @Column(name = "capacidade_pessoas", nullable = false)
    private int capacidadePessoas = 2;
}