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

    // Duração fixa de 45 minutos
    private static final int DURACAO_MINUTOS = 45;

    // Status que bloqueiam o recurso para sobreposição
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

    // ─── Criar reserva ────────────────────────────────────────────────────────

    @Transactional
    public ReservaSala criar(ReservaSalaDTO dto, String emailUsuarioLogado) {
        Sala sala = salaRepo.findById(dto.getSalaId())
                .orElseThrow(() -> new RuntimeException("Sala não encontrada."));

        Usuario usuarioLogado = usuarioRepo.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        // Dono da reserva: se admin informou outro usuário, usa esse; senão, o próprio logado
        Usuario dono = (dto.getUsuarioId() != null)
                ? usuarioRepo.findById(dto.getUsuarioId())
                        .orElseThrow(() -> new RuntimeException("Usuário dono não encontrado."))
                : usuarioLogado;

        LocalDateTime inicio = dto.getInicioPrevisto();
        LocalDateTime fim = inicio.plusMinutes(DURACAO_MINUTOS); // sempre 45min

        // Validação de capacidade
        if (dto.getQtdePessoas() > sala.getCapacidadePessoas()) {
            throw new RuntimeException(
                    "Quantidade de pessoas excede a capacidade da sala (" + sala.getCapacidadePessoas() + ")."
            );
        }

        // Validação de sobreposição
        List<ReservaSala> sobrepostas = reservaRepo.findSobrepostas(
                sala.getId(), inicio, fim, STATUS_BLOQUEADORES
        );
        if (!sobrepostas.isEmpty()) {
            throw new RuntimeException("Já existe uma reserva para esta sala neste horário.");
        }

        ReservaSala reserva = new ReservaSala();
        reserva.setSala(sala);
        reserva.setUsuario(dono);
        reserva.setCriadaPorUsuario(usuarioLogado);
        reserva.setInicioPrevisto(inicio);
        reserva.setFimPrevisto(fim);
        reserva.setQtdePessoas(dto.getQtdePessoas());
        reserva.setObservacao(dto.getObservacao());

        // Reserva de 45min nunca passa de 3h, então sempre vai direto para APROVADA.
        // Mas mantemos a lógica aqui para o caso de a duração ser ajustada futuramente.
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

    // ─── Check-in ─────────────────────────────────────────────────────────────

    @Transactional
    public ReservaSala checkin(Long reservaId, String emailUsuarioLogado) {
        ReservaSala reserva = buscarAtiva(reservaId);
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

    // ─── Check-out ────────────────────────────────────────────────────────────

    @Transactional
    public ReservaSala checkout(Long reservaId, String emailUsuarioLogado) {
        ReservaSala reserva = buscarAtiva(reservaId);
        validarDonoOuAdmin(reserva.getUsuario().getEmail(), emailUsuarioLogado);

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

    // ─── Jobs automáticos (chamados pelo Scheduler) ───────────────────────────

    @Transactional
    public void processarAutoCheckout() {
        List<ReservaSala> reservas = reservaRepo.findParaAutoCheckout(LocalDateTime.now());
        for (ReservaSala r : reservas) {
            r.setCheckoutEm(r.getFimPrevisto());
            r.setCheckoutAutomatico(true);
            r.setStatus(StatusReserva.FINALIZADA);
            reservaRepo.save(r);
        }
    }

    @Transactional
    public void processarNoShow() {
        // Marca como no-show reservas APROVADAS cujo início passou há mais de 15min sem check-in
        LocalDateTime limite = LocalDateTime.now().minusMinutes(15);
        List<ReservaSala> reservas = reservaRepo.findParaNoShow(limite);
        for (ReservaSala r : reservas) {
            r.setStatus(StatusReserva.ATRASADO);
            r.setNoShowEm(LocalDateTime.now());
            reservaRepo.save(r);
        }
    }

    // ─── Listagens ────────────────────────────────────────────────────────────

    public List<ReservaSala> listarPorUsuario(Long usuarioId) {
        return reservaRepo.findByUsuarioId(usuarioId);
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private ReservaSala buscarAtiva(Long id) {
        return reservaRepo.findById(id)
                .filter(r -> r.isAtivo())
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada."));
    }

    private void validarDonoOuAdmin(String emailDono, String emailLogado) {
        // Admin pode agir em qualquer reserva; usuário só na sua
        // A verificação de role ADMIN deve ser feita no Controller via @Admin
        // Aqui garantimos que o usuário comum só age na própria reserva
        if (!emailDono.equals(emailLogado)) {
            // Se chegou aqui sem ser admin, o Spring Security já deveria ter bloqueado.
            // Mantemos como dupla proteção.
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