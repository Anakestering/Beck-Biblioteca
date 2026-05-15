package com.example.demo.service;

import com.example.demo.dto.ReservaSalaDTO;
import com.example.demo.entity.*;
import com.example.demo.enums.StatusAprovacao;
import com.example.demo.enums.StatusReserva;
import com.example.demo.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaSalaService {

    // Duração fixa de cada reserva
    private static final int DURACAO_MINUTOS = 45;

    // Limite de tempos consecutivos antes de exigir aprovação (3 × 45min = 135min)
    private static final int MAX_TEMPOS_SEM_APROVACAO = 4;

    // Check-in: de 5min antes até 15min depois
    private static final int CHECKIN_ANTECEDENCIA_MIN = 5;
    private static final int CHECKIN_TOLERANCIA_MIN = 15;

    private static final List<StatusReserva> STATUS_BLOQUEADORES = List.of(
            StatusReserva.APROVADA,
            StatusReserva.PENDENTE_APROVACAO,
            StatusReserva.EM_ANDAMENTO
    );

    private final ReservaSalaRepository reservaRepo;
    private final SalaRepository salaRepo;
    private final UsuarioRepository usuarioRepo;
    private final AprovacaoReservaRepository aprovacaoRepo;

    public ReservaSalaService(
            ReservaSalaRepository reservaRepo,
            SalaRepository salaRepo,
            UsuarioRepository usuarioRepo,
            AprovacaoReservaRepository aprovacaoRepo
    ) {
        this.reservaRepo = reservaRepo;
        this.salaRepo = salaRepo;
        this.usuarioRepo = usuarioRepo;
        this.aprovacaoRepo = aprovacaoRepo;
    }

    // ─── Criar ────────────────────────────────────────────────────────────────

    @Transactional
    public ReservaSala criar(ReservaSalaDTO dto, String emailUsuarioLogado) {
        Sala sala = salaRepo.findById(dto.getSalaId())
                .orElseThrow(() -> new RuntimeException("Sala não encontrada."));

        Usuario usuarioLogado = usuarioRepo.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        Usuario dono = (dto.getUsuarioId() != null)
                ? usuarioRepo.findById(dto.getUsuarioId())
                        .orElseThrow(() -> new RuntimeException("Usuário não encontrado."))
                : usuarioLogado;

        LocalDateTime inicio = dto.getInicioPrevisto();
        LocalDateTime fim = inicio.plusMinutes(DURACAO_MINUTOS);

        // Validação de capacidade
        if (dto.getQtdePessoas() > sala.getCapacidadePessoas()) {
            throw new RuntimeException(
                    "Quantidade de pessoas excede a capacidade da sala (" + sala.getCapacidadePessoas() + ")."
            );
        }

        // Validação de sobreposição (sala já reservada nesse horário)
        if (!reservaRepo.findSobrepostas(sala.getId(), inicio, fim, STATUS_BLOQUEADORES).isEmpty()) {
            throw new RuntimeException("Já existe uma reserva para esta sala neste horário.");
        }

        // Verifica se o usuário tem reservas consecutivas nesta sala (blocos contínuos)
        int temposConsecutivos = contarTemposConsecutivos(dono.getId(), sala.getId(), inicio, fim);
        boolean precisaAprovacao = temposConsecutivos > MAX_TEMPOS_SEM_APROVACAO;

        ReservaSala reserva = new ReservaSala();
        reserva.setSala(sala);
        reserva.setUsuario(dono);
        reserva.setCriadaPorUsuario(usuarioLogado);
        reserva.setInicioPrevisto(inicio);
        reserva.setFimPrevisto(fim);
        reserva.setQtdePessoas(dto.getQtdePessoas());
        reserva.setObservacao(dto.getObservacao());
        reserva.setStatus(precisaAprovacao ? StatusReserva.PENDENTE_APROVACAO : StatusReserva.APROVADA);

        reserva = reservaRepo.save(reserva);

        if (precisaAprovacao) {
            criarAprovacao(reserva, null);
        }

        return reserva;
    }

    // ─── Check-in ─────────────────────────────────────────────────────────────

    @Transactional
    public ReservaSala checkin(Long reservaId, String emailUsuarioLogado) {
        ReservaSala reserva = buscarAtiva(reservaId);
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
    public ReservaSala checkout(Long reservaId, String emailUsuarioLogado) {
        ReservaSala reserva = buscarAtiva(reservaId);
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
    public ReservaSala cancelar(Long reservaId, String emailUsuarioLogado) {
        ReservaSala reserva = buscarAtiva(reservaId);
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
        // Passou 15min do início sem check-in → ATRASADO
        LocalDateTime limite = LocalDateTime.now().minusMinutes(CHECKIN_TOLERANCIA_MIN);
        reservaRepo.findParaAtrasado(limite).forEach(r -> {
            r.setStatus(StatusReserva.ATRASADO);
            r.setAtrasadoEm(LocalDateTime.now());
            reservaRepo.save(r);
        });
    }

    // ─── Listagens ────────────────────────────────────────────────────────────

    public List<ReservaSala> listarPorEmailUsuario(String email) {
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        return reservaRepo.findByUsuarioId(usuario.getId());
    }

    public List<ReservaSala> listarTodas() {
        return reservaRepo.findAll();
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    /**
     * Conta quantos tempos consecutivos o usuário já tem nesta sala
     * encostados no início da nova reserva.
     * Exemplo: se ele já tem 2 reservas encadenadas terminando exatamente
     * em `inicio`, o bloco atual seria o 3º — se passar de 3, exige aprovação.
     */
    private int contarTemposConsecutivos(Long usuarioId, Long salaId, LocalDateTime inicio, LocalDateTime fim) {
        int count = 1; // conta a reserva que está sendo criada

        // Verifica para trás (reservas que terminam exatamente onde a nova começa)
        LocalDateTime cursor = inicio;
        while (true) {
            LocalDateTime cursorFinal = cursor;
            boolean temAnterior = reservaRepo
                    .findByUsuarioIdESalaIdEFimPrevisto(usuarioId, salaId, cursorFinal, STATUS_BLOQUEADORES)
                    .isPresent();
            if (!temAnterior) break;
            count++;
            cursor = cursor.minusMinutes(DURACAO_MINUTOS);
        }

        // Verifica para frente (reservas que começam exatamente onde a nova termina)
        cursor = fim;
        while (true) {
            LocalDateTime cursorInicio = cursor;
            boolean temProxima = reservaRepo
                    .findByUsuarioIdESalaIdEInicioPrevisto(usuarioId, salaId, cursorInicio, STATUS_BLOQUEADORES)
                    .isPresent();
            if (!temProxima) break;
            count++;
            cursor = cursor.plusMinutes(DURACAO_MINUTOS);
        }

        return count;
    }

    private ReservaSala buscarAtiva(Long id) {
        return reservaRepo.findById(id)
                .filter(ReservaSala::isAtivo)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada."));
    }

    private void validarDono(String emailDono, String emailLogado) {
        if (!emailDono.equals(emailLogado)) {
            throw new RuntimeException("Você não tem permissão para alterar esta reserva.");
        }
    }

    private void criarAprovacao(ReservaSala reserva, String motivo) {
        AprovacaoReserva aprovacao = new AprovacaoReserva();
        aprovacao.setReservaSala(reserva);
        aprovacao.setStatus(StatusAprovacao.PENDENTE);
        aprovacao.setSolicitadaEm(LocalDateTime.now());
        aprovacao.setMotivo(motivo);
        aprovacaoRepo.save(aprovacao);
    }
}