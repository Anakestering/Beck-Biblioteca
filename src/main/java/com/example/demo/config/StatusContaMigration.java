package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Usuario;
import com.example.demo.enums.StatusConta;
import com.example.demo.repository.UsuarioRepository;

/**
 * Migração de dados executada na startup.
 *
 * Contexto: a coluna status_conta foi adicionada com DEFAULT 'ATIVO', então
 * todos os registros existentes recebem ATIVO automaticamente pelo banco.
 * Este runner corrige os casos que não deveriam ser ATIVO:
 *   - ativo=false               → INATIVO  (conta desativada pelo admin)
 *   - ativo=true e senha=null   → PENDENTE (cadastrado mas nunca criou senha)
 *
 * Idempotente: só altera usuários ainda marcados como ATIVO que não deveriam ser.
 */
@Component
public class StatusContaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StatusContaMigration.class);

    private final UsuarioRepository usuarioRepository;

    public StatusContaMigration(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int corrigidos = 0;

        for (Usuario u : usuarioRepository.findAll()) {
            StatusConta correto = statusCorreto(u);
            if (u.getStatusConta() != correto) {
                u.setStatusConta(correto);
                usuarioRepository.save(u);
                corrigidos++;
            }
        }

        if (corrigidos > 0) {
            log.info("StatusContaMigration: {} usuario(s) corrigido(s).", corrigidos);
        }
    }

    private StatusConta statusCorreto(Usuario u) {
        if (!u.isAtivo())          return StatusConta.INATIVO;
        if (u.getSenha() == null)  return StatusConta.PENDENTE;
        return StatusConta.ATIVO;
    }
}
