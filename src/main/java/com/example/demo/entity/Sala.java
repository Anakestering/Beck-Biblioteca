package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sala")
@EqualsAndHashCode(callSuper = false)
public class Sala extends BaseEntity {

    @Column(name = "nome", nullable = false, unique = true)
    private String nome;

    @Column(name = "capacidade_pessoas", nullable = false)
    private int capacidadePessoas;
}