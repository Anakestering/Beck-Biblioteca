package com.example.demo.service;

import com.example.demo.entity.AprovacaoReserva;
import com.example.demo.entity.Usuario;
import com.example.demo.enums.StatusAprovacao;
import com.example.demo.enums.StatusReserva;
import com.example.demo.repository.AprovacaoReservaRepository;
import com.example.demo.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AprovacaoReservaService {

    private final AprovacaoReservaRepository repo;
    private final UsuarioRepository usuarioRepo;

    public AprovacaoReservaService(AprovacaoReservaRepository repo, UsuarioRepository usuarioRepo) {
        this.repo = repo;
        this.usuarioRepo = usuarioRepo;
    }

    public List<AprovacaoReserva> listarPendentes() {
        return repo.findByStatus(StatusAprovacao.PENDENTE);
    }

    @Transactional
    public AprovacaoReserva aprovar(Long id, String motivo, String emailAdmin) {
        AprovacaoReserva aprovacao = buscar(id);
        Usuario admin = usuarioRepo.findByEmail(emailAdmin)
                .orElseThrow(() -> new RuntimeException("Admin não encontrado."));

        aprovacao.setStatus(StatusAprovacao.APROVADA);
        aprovacao.setDecididaEm(LocalDateTime.now());
        aprovacao.setDecididaPorUsuario(admin);
        aprovacao.setMotivo(motivo);

        // Atualiza o status da reserva vinculada
        if (aprovacao.getReservaSala() != null) {
            aprovacao.getReservaSala().setStatus(StatusReserva.APROVADA);
        } else if (aprovacao.getReservaComputador() != null) {
            aprovacao.getReservaComputador().setStatus(StatusReserva.APROVADA);
        }

        return repo.save(aprovacao);
    }

    @Transactional
    public AprovacaoReserva rejeitar(Long id, String motivo, String emailAdmin) {
        AprovacaoReserva aprovacao = buscar(id);
        Usuario admin = usuarioRepo.findByEmail(emailAdmin)
                .orElseThrow(() -> new RuntimeException("Admin não encontrado."));

        aprovacao.setStatus(StatusAprovacao.REJEITADA);
        aprovacao.setDecididaEm(LocalDateTime.now());
        aprovacao.setDecididaPorUsuario(admin);
        aprovacao.setMotivo(motivo);

        if (aprovacao.getReservaSala() != null) {
            aprovacao.getReservaSala().setStatus(StatusReserva.REJEITADA);
        } else if (aprovacao.getReservaComputador() != null) {
            aprovacao.getReservaComputador().setStatus(StatusReserva.REJEITADA);
        }

        return repo.save(aprovacao);
    }

    private AprovacaoReserva buscar(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Aprovação não encontrada."));
    }
}