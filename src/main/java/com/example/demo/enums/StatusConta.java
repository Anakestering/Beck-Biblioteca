package com.example.demo.enums;

/**
 * Status da conta de um usuário.
 *
 * PENDENTE  — admin criou a conta, mas o usuário ainda não criou sua senha.
 *             Login bloqueado.
 * ATIVO     — senha criada, pode acessar o sistema normalmente.
 * INATIVO   — conta desativada pelo admin. Login bloqueado.
 *             Apenas o admin pode reativar (status nunca volta a ATIVO
 *             automaticamente pelo fluxo de senha).
 */
public enum StatusConta {
    PENDENTE,
    ATIVO,
    INATIVO
}
