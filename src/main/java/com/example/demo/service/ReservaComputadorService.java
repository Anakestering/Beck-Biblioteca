package com.example.demo.service;

import com.example.demo.dto.ReservaComputadorDTO;
import com.example.demo.entity.*;
import com.example.demo.enums.StatusAprovacao;
import com.example.demo.enums.StatusReserva;
import com.example.demo.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaComputadorService {

    private static final int DURACAO_MINUTOS = 45;

    private static final List<StatusReserva> STATUS_BLOQUEADORES = List.of(
            StatusReserva.APROVADA,
            StatusReserva.PENDENTE_APROVACAO,
            StatusReserva.EM_ANDAMENTO
    );

    private final ReservaComputadorRepository reservaRepo;
    private final ComputadorRepository computadorRepo;
    private final UsuarioRepository usuarioRepo;
    private final AprovacaoReservaRepository aprovacaoRepo;

    public ReservaComputadorService(
            ReservaComputadorRepository reservaRepo,
            ComputadorRepository computadorRepo,
            UsuarioRepository usuarioRepo,
            AprovacaoReservaRepository aprovacaoRepo
    ) {
        this.reservaRepo = reservaRepo;
        this.computadorRepo = computadorRepo;
        this.usuarioRepo = usuarioRepo;
        this.aprovacaoRepo = aprovacaoRepo;
    }

    @Transactional
    public ReservaComputador criar(ReservaComputadorDTO dto, String emailUsuarioLogado) {
        Computador computador = computadorRepo.findById(dto.getComputadorId())
                .orElseThrow(() -> new RuntimeException("Computador não encontrado."));

        Usuario usuarioLogado = usuarioRepo.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        Usuario dono = (dto.getUsuarioId() != null)
                ? usuarioRepo.findById(dto.getUsuarioId())
                        .orElseThrow(() -> new RuntimeException("Usuário dono não encontrado."))
                : usuarioLogado;

        LocalDateTime inicio = dto.getInicioPrevisto();
        LocalDateTime fim = inicio.plusMinutes(DURACAO_MINUTOS);

        if (dto.getQtdePessoas() > computador.getCapacidadePessoas()) {
            throw new RuntimeException(
                    "Quantidade de pessoas excede a capacidade do computador (" + computador.getCapacidadePessoas() + ")."
            );
        }

        List<ReservaComputador> sobrepostas = reservaRepo.findSobrepostas(
                computador.getId(), inicio, fim, STATUS_BLOQUEADORES
        );
        if (!sobrepostas.isEmpty()) {
            throw new RuntimeException("Já existe uma reserva para este computador neste horário.");
        }

        ReservaComputador reserva = new ReservaComputador();
        reserva.setComputador(computador);
        reserva.setUsuario(dono);
        reserva.setCriadaPorUsuario(usuarioLogado);
        reserva.setInicioPrevisto(inicio);
        reserva.setFimPrevisto(fim);
        reserva.setQtdePessoas(dto.getQtdePessoas());
        reserva.setObservacao(dto.getObservacao());

        long duracaoHoras = java.time.Duration.between(inicio, fim).toHours();
        if (duracaoHoras > 3) {
            reserva.setStatus(StatusReserva.PENDENTE_APROVACAO);
            reserva = reservaRepo.save(reserva);
            criarAprovacao(reserva, null);
        } else {
            reserva.setStatus(StatusReserva.APROVADA);
            reserva = reservaRepo.save(reserva);
        }

        return reserva;
    }

    @Transactional
    public ReservaComputador checkin(Long reservaId, String emailUsuarioLogado) {
        ReservaComputador reserva = buscarAtiva(reservaId);
        validarDonoOuAdmin(reserva.getUsuario().getEmail(), emailUsuarioLogado);

        if (reserva.getStatus() != StatusReserva.APROVADA) {
            throw new RuntimeException("Check-in só é permitido para reservas com status APROVADA.");
        }

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime janelaInicio = reserva.getInicioPrevisto().minusMinutes(15);
        LocalDateTime janelaFim = reserva.getInicioPrevisto().plusMinutes(15);

        if (agora.isBefore(janelaInicio) || agora.isAfter(janelaFim)) {
            throw new RuntimeException(
                    "Check-in permitido somente entre 15min antes e 15min após o início previsto."
            );
        }

        reserva.setCheckinEm(agora);
        reserva.setStatus(StatusReserva.EM_ANDAMENTO);
        return reservaRepo.save(reserva);
    }

    @Transactional
    public ReservaComputador checkout(Long reservaId, String emailUsuarioLogado) {
        ReservaComputador reserva = buscarAtiva(reservaId);
        validarDonoOuAdmin(reserva.getUsuario().getEmail(), emailUsuarioLogado);

        if (reserva.getStatus() != StatusReserva.EM_ANDAMENTO) {
            throw new RuntimeException("Check-out só é permitido para reservas em andamento.");
        }

        reserva.setCheckoutEm(LocalDateTime.now());
        reserva.setStatus(StatusReserva.FINALIZADA);
        reserva.setCheckoutAutomatico(false);
        return reservaRepo.save(reserva);
    }

    @Transactional
    public ReservaComputador cancelar(Long reservaId, String emailUsuarioLogado) {
        ReservaComputador reserva = buscarAtiva(reservaId);
        validarDonoOuAdmin(reserva.getUsuario().getEmail(), emailUsuarioLogado);

        if (reserva.getStatus() == StatusReserva.CANCELADA
                || reserva.getStatus() == StatusReserva.FINALIZADA
                || reserva.getStatus() == StatusReserva.ATRASADO) {
            throw new RuntimeException("Reserva já encerrada, não pode ser cancelada.");
        }

        LocalDateTime limiteParaCancelar = reserva.getInicioPrevisto().minusHours(1);
        if (LocalDateTime.now().isAfter(limiteParaCancelar)) {
            throw new RuntimeException("Cancelamento permitido somente até 1h antes do início.");
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setCanceladaEm(LocalDateTime.now());
        return reservaRepo.save(reserva);
    }

    @Transactional
    public void processarAutoCheckout() {
        List<ReservaComputador> reservas = reservaRepo.findParaAutoCheckout(LocalDateTime.now());
        for (ReservaComputador r : reservas) {
            r.setCheckoutEm(r.getFimPrevisto());
            r.setCheckoutAutomatico(true);
            r.setStatus(StatusReserva.FINALIZADA);
            reservaRepo.save(r);
        }
    }

    @Transactional
    public void processarNoShow() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(15);
        List<ReservaComputador> reservas = reservaRepo.findParaNoShow(limite);
        for (ReservaComputador r : reservas) {
            r.setStatus(StatusReserva.ATRASADO);
            r.setNoShowEm(LocalDateTime.now());
            reservaRepo.save(r);
        }
    }

    public List<ReservaComputador> listarPorUsuario(Long usuarioId) {
        return reservaRepo.findByUsuarioId(usuarioId);
    }

    private ReservaComputador buscarAtiva(Long id) {
        return reservaRepo.findById(id)
                .filter(r -> r.isAtivo())
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada."));
    }

    private void validarDonoOuAdmin(String emailDono, String emailLogado) {
        if (!emailDono.equals(emailLogado)) {
            throw new RuntimeException("Você não tem permissão para alterar esta reserva.");
        }
    }

    private void criarAprovacao(ReservaComputador reserva, String motivo) {
        AprovacaoReserva aprovacao = new AprovacaoReserva();
        aprovacao.setReservaComputador(reserva);
        aprovacao.setStatus(StatusAprovacao.PENDENTE);
        aprovacao.setSolicitadaEm(LocalDateTime.now());
        aprovacao.setMotivo(motivo);
        aprovacaoRepo.save(aprovacao);
    }
}