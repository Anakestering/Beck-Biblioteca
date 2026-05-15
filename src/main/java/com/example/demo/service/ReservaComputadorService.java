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
    private static final int MAX_TEMPOS_SEM_APROVACAO = 4;
    private static final int CHECKIN_ANTECEDENCIA_MIN = 5;
    private static final int CHECKIN_TOLERANCIA_MIN = 15;

    // Mais de 3 PCs no mesmo horário exige aprovação
    private static final int MAX_PCS_MESMO_HORARIO_SEM_APROVACAO = 3;

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

    // ─── Criar ────────────────────────────────────────────────────────────────

    @Transactional
    public ReservaComputador criar(ReservaComputadorDTO dto, String emailUsuarioLogado) {
        Computador computador = computadorRepo.findById(dto.getComputadorId())
                .orElseThrow(() -> new RuntimeException("Computador não encontrado."));

        Usuario usuarioLogado = usuarioRepo.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        Usuario dono = (dto.getUsuarioId() != null)
                ? usuarioRepo.findById(dto.getUsuarioId())
                        .orElseThrow(() -> new RuntimeException("Usuário não encontrado."))
                : usuarioLogado;

        LocalDateTime inicio = dto.getInicioPrevisto();
        LocalDateTime fim = inicio.plusMinutes(DURACAO_MINUTOS);

        // Validação de capacidade
        if (dto.getQtdePessoas() > computador.getCapacidadePessoas()) {
            throw new RuntimeException(
                    "Quantidade de pessoas excede a capacidade do computador (" + computador.getCapacidadePessoas() + ")."
            );
        }

        // Validação de sobreposição (este PC já está reservado)
        if (!reservaRepo.findSobrepostas(computador.getId(), inicio, fim, STATUS_BLOQUEADORES).isEmpty()) {
            throw new RuntimeException("Este computador já está reservado neste horário.");
        }

        // Quantos PCs diferentes o usuário já reservou neste mesmo horário?
        int pcsNoMesmoHorario = reservaRepo.countPcsDoUsuarioNoHorario(dono.getId(), inicio, fim, STATUS_BLOQUEADORES);

        // Tempos consecutivos neste mesmo PC
        int temposConsecutivos = contarTemposConsecutivos(dono.getId(), computador.getId(), inicio, fim);

        boolean precisaAprovacao = pcsNoMesmoHorario >= MAX_PCS_MESMO_HORARIO_SEM_APROVACAO
                || temposConsecutivos > MAX_TEMPOS_SEM_APROVACAO;

        ReservaComputador reserva = new ReservaComputador();
        reserva.setComputador(computador);
        reserva.setUsuario(dono);
        reserva.setCriadaPorUsuario(usuarioLogado);
        reserva.setInicioPrevisto(inicio);
        reserva.setFimPrevisto(fim);
        reserva.setQtdePessoas(dto.getQtdePessoas());
        reserva.setObservacao(dto.getObservacao());
        reserva.setStatus(precisaAprovacao ? StatusReserva.PENDENTE_APROVACAO : StatusReserva.APROVADA);

        reserva = reservaRepo.save(reserva);

        if (precisaAprovacao) {
            criarAprovacao(reserva);
        }

        return reserva;
    }

    // ─── Check-in ─────────────────────────────────────────────────────────────

    @Transactional
    public ReservaComputador checkin(Long reservaId, String emailUsuarioLogado) {
        ReservaComputador reserva = buscarAtiva(reservaId);
        validarDono(reserva.getUsuario().getEmail(), emailUsuarioLogado);

        if (reserva.getStatus() != StatusReserva.APROVADA) {
            throw new RuntimeException("Check-in só é permitido para reservas com status APROVADA.");
        }

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime janelaInicio = reserva.getInicioPrevisto().minusMinutes(CHECKIN_ANTECEDENCIA_MIN);
        LocalDateTime janelaFim = reserva.getInicioPrevisto().plusMinutes(CHECKIN_TOLERANCIA_MIN);

        if (agora.isBefore(janelaInicio)) {
            throw new RuntimeException("Check-in disponível somente a partir de " + CHECKIN_ANTECEDENCIA_MIN + " minutos antes do início.");
        }
        if (agora.isAfter(janelaFim)) {
            throw new RuntimeException("Prazo de check-in encerrado. A reserva será marcada como atrasada.");
        }

        reserva.setCheckinEm(agora);
        reserva.setStatus(StatusReserva.EM_ANDAMENTO);
        return reservaRepo.save(reserva);
    }

    // ─── Check-out ────────────────────────────────────────────────────────────

    @Transactional
    public ReservaComputador checkout(Long reservaId, String emailUsuarioLogado) {
        ReservaComputador reserva = buscarAtiva(reservaId);
        validarDono(reserva.getUsuario().getEmail(), emailUsuarioLogado);

        if (reserva.getStatus() != StatusReserva.EM_ANDAMENTO) {
            throw new RuntimeException("Check-out só é permitido para reservas em andamento.");
        }

        reserva.setCheckoutEm(LocalDateTime.now());
        reserva.setStatus(StatusReserva.FINALIZADA);
        reserva.setCheckoutAutomatico(false);
        return reservaRepo.save(reserva);
    }

    // ─── Cancelar ─────────────────────────────────────────────────────────────

    @Transactional
    public ReservaComputador cancelar(Long reservaId, String emailUsuarioLogado) {
        ReservaComputador reserva = buscarAtiva(reservaId);
        validarDono(reserva.getUsuario().getEmail(), emailUsuarioLogado);

        if (List.of(StatusReserva.CANCELADA, StatusReserva.FINALIZADA, StatusReserva.ATRASADO)
                .contains(reserva.getStatus())) {
            throw new RuntimeException("Esta reserva já foi encerrada.");
        }

        if (LocalDateTime.now().isAfter(reserva.getInicioPrevisto().minusHours(1))) {
            throw new RuntimeException("Cancelamento permitido somente até 1h antes do início.");
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setCanceladaEm(LocalDateTime.now());
        return reservaRepo.save(reserva);
    }

    // ─── Jobs automáticos ─────────────────────────────────────────────────────

    @Transactional
    public void processarAutoCheckout() {
        reservaRepo.findParaAutoCheckout(LocalDateTime.now()).forEach(r -> {
            r.setCheckoutEm(r.getFimPrevisto());
            r.setCheckoutAutomatico(true);
            r.setStatus(StatusReserva.FINALIZADA);
            reservaRepo.save(r);
        });
    }

    @Transactional
    public void processarAtrasado() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(CHECKIN_TOLERANCIA_MIN);
        reservaRepo.findParaAtrasado(limite).forEach(r -> {
            r.setStatus(StatusReserva.ATRASADO);
            r.setAtrasadoEm(LocalDateTime.now());
            reservaRepo.save(r);
        });
    }

    // ─── Listagens ────────────────────────────────────────────────────────────

    public List<ReservaComputador> listarPorEmailUsuario(String email) {
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        return reservaRepo.findByUsuarioId(usuario.getId());
    }

    public List<ReservaComputador> listarTodas() {
        return reservaRepo.findAll();
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private int contarTemposConsecutivos(Long usuarioId, Long computadorId, LocalDateTime inicio, LocalDateTime fim) {
        int count = 1;

        LocalDateTime cursor = inicio;
        while (true) {
            boolean temAnterior = reservaRepo
                    .findByUsuarioIdEComputadorIdEFimPrevisto(usuarioId, computadorId, cursor, STATUS_BLOQUEADORES)
                    .isPresent();
            if (!temAnterior) break;
            count++;
            cursor = cursor.minusMinutes(DURACAO_MINUTOS);
        }

        cursor = fim;
        while (true) {
            boolean temProxima = reservaRepo
                    .findByUsuarioIdEComputadorIdEInicioPrevisto(usuarioId, computadorId, cursor, STATUS_BLOQUEADORES)
                    .isPresent();
            if (!temProxima) break;
            count++;
            cursor = cursor.plusMinutes(DURACAO_MINUTOS);
        }

        return count;
    }

    private ReservaComputador buscarAtiva(Long id) {
        return reservaRepo.findById(id)
                .filter(ReservaComputador::isAtivo)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada."));
    }

    private void validarDono(String emailDono, String emailLogado) {
        if (!emailDono.equals(emailLogado)) {
            throw new RuntimeException("Você não tem permissão para alterar esta reserva.");
        }
    }

    private void criarAprovacao(ReservaComputador reserva) {
        AprovacaoReserva aprovacao = new AprovacaoReserva();
        aprovacao.setReservaComputador(reserva);
        aprovacao.setStatus(StatusAprovacao.PENDENTE);
        aprovacao.setSolicitadaEm(LocalDateTime.now());
        aprovacaoRepo.save(aprovacao);
    }
}