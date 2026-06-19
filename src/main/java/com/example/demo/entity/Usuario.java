package com.example.demo.entity;

import java.time.LocalDateTime;

import com.example.demo.enums.NivelAcesso;
import com.example.demo.enums.StatusConta;
import com.example.demo.enums.TipoUsuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "usuario")
@EqualsAndHashCode(callSuper = false)
public class Usuario extends BaseEntity {

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "cpf", nullable = false, unique = true)
    private String cpf;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "senha", nullable = true)
    private String senha;

    @Column(name = "telefone")
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelAcesso nivelAcesso = NivelAcesso.PADRAO;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false)
    private TipoUsuario tipoUsuario;

    /**
     * Status da conta. O Hibernate adiciona a coluna automaticamente (ddl-auto=update)
     * com DEFAULT 'ATIVO'. O StatusContaMigration corrige registros legados na startup.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_conta", nullable = false,
            columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'ATIVO'")
    private StatusConta statusConta = StatusConta.PENDENTE;

    private String codigoRecuperacao;

    private LocalDateTime codigoRecuperacaoExpiracao;
}
