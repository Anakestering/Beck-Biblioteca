package com.example.demo.service;

import com.example.demo.dto.ReservaSalaDTO;
import com.example.demo.entity.*;
import com.example.demo.enums.StatusReserva;
import com.example.demo.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReservaSalaService {

    private static final int DURACAO_MINUTOS = 45;
    private static final int CHECKIN_ANTECEDENCIA_MIN = 5;
    private static final int CHECKIN_TOLERANCIA_MIN = 15;

    private static final List<StatusReserva> STATUS_BLOQUEADORES = List.of(
            StatusReserva.APROVADA,
            StatusReserva.PENDENTE_APROVACAO,
            StatusReserva.EM_ANDAMENTO);

    private final ReservaSalaRepository reservaRepo;
    private final SalaRepository salaRepo;
    private final UsuarioRepository usuarioRepo;

    public ReservaSalaService(
        ReservaSalaRepository reservaRepo,
        SalaRepository salaRepo,
        UsuarioRepository usuarioRepo) {  
    this.reservaRepo = reservaRepo;
    this.salaRepo = salaRepo;
    this.usuarioRepo = usuarioRepo;
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

    if (dto.getQtdePessoas() > sala.getCapacidadePessoas()) {
        throw new RuntimeException(
                "Quantidade de pessoas excede a capacidade da sala (" + sala.getCapacidadePessoas() + ").");
    }

    if (!reservaRepo.findSobrepostas(sala.getId(), inicio, fim, STATUS_BLOQUEADORES).isEmpty()) {
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
    reserva.setStatus(StatusReserva.APROVADA);

    return reservaRepo.save(reserva);
}

    // ─── Check-in ─────────────────────────────────────────────────────────────
    // Faz checkin no bloco atual e em todos os blocos consecutivos seguintes
    // do mesmo usuário nesta sala

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
            throw new RuntimeException("Check-in disponível somente a partir de "
                    + CHECKIN_ANTECEDENCIA_MIN + " minutos antes do início.");
        }
        if (agora.isAfter(janelaFim)) {
            throw new RuntimeException("Prazo de check-in encerrado. A reserva será marcada como atrasada.");
        }

        // Faz checkin no bloco atual
        reserva.setCheckinEm(agora);
        reserva.setStatus(StatusReserva.EM_ANDAMENTO);
        reservaRepo.save(reserva);

        // Propaga checkin para todos os blocos consecutivos seguintes (mesmo usuário,
        // mesma sala)
        LocalDateTime cursor = reserva.getFimPrevisto();
        while (true) {
            Optional<ReservaSala> proximo = reservaRepo
                    .findByUsuarioIdESalaIdEInicioPrevisto(
                            reserva.getUsuario().getId(),
                            reserva.getSala().getId(),
                            cursor,
                            List.of(StatusReserva.APROVADA));
            if (proximo.isEmpty())
                break;
            ReservaSala r = proximo.get();
            r.setCheckinEm(agora);
            r.setStatus(StatusReserva.EM_ANDAMENTO);
            reservaRepo.save(r);
            cursor = r.getFimPrevisto();
        }

        return reserva;
    }

    // ─── Check-out ────────────────────────────────────────────────────────────
    // Finaliza o bloco atual e cancela todos os blocos seguintes 

    @Transactional
    public ReservaSala checkout(Long reservaId, String emailUsuarioLogado) {
        ReservaSala reserva = buscarAtiva(reservaId);
        validarDono(reserva.getUsuario().getEmail(), emailUsuarioLogado);

        if (reserva.getStatus() != StatusReserva.EM_ANDAMENTO) {
            throw new RuntimeException("Check-out só é permitido para reservas em andamento.");
        }

        LocalDateTime agora = LocalDateTime.now();

        // Finaliza o bloco atual
        reserva.setCheckoutEm(agora);
        reserva.setStatus(StatusReserva.FINALIZADA);
        reserva.setCheckoutAutomatico(false);
        reservaRepo.save(reserva);

        // Cancela blocos seguintes EM_ANDAMENTO 
        LocalDateTime cursor = reserva.getFimPrevisto();
        while (true) {
            Optional<ReservaSala> proximo = reservaRepo
                    .findByUsuarioIdESalaIdEInicioPrevisto(
                            reserva.getUsuario().getId(),
                            reserva.getSala().getId(),
                            cursor,
                            List.of(StatusReserva.EM_ANDAMENTO));
            if (proximo.isEmpty())
                break;
            ReservaSala r = proximo.get();
            r.setStatus(StatusReserva.CANCELADA);
            r.setCanceladaEm(agora);
            reservaRepo.save(r);
            cursor = r.getFimPrevisto();
        }

        return reserva;
    }

    // ─── Cancelar ─────────────────────────────────────────────────────────────
    // Cancela o bloco atual e todos os consecutivos seguintes do mesmo grupo.

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

        LocalDateTime agora = LocalDateTime.now();

        // Cancela o bloco atual
        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setCanceladaEm(agora);
        reservaRepo.save(reserva);

        // Cancela todos os blocos consecutivos seguintes do mesmo grupo
        LocalDateTime cursor = reserva.getFimPrevisto();
        while (true) {
            Optional<ReservaSala> proximo = reservaRepo
                    .findByUsuarioIdESalaIdEInicioPrevisto(
                            reserva.getUsuario().getId(),
                            reserva.getSala().getId(),
                            cursor,
                            List.of(StatusReserva.APROVADA, StatusReserva.PENDENTE_APROVACAO));
            if (proximo.isEmpty())
                break;
            ReservaSala r = proximo.get();
            r.setStatus(StatusReserva.CANCELADA);
            r.setCanceladaEm(agora);
            reservaRepo.save(r);
            cursor = r.getFimPrevisto();
        }

        return reserva;
    }

    @Transactional
public ReservaSala cancelarComoAdmin(Long reservaId) {
    ReservaSala reserva = buscarAtiva(reservaId);

    if (List.of(StatusReserva.CANCELADA, StatusReserva.FINALIZADA, StatusReserva.ATRASADO)
            .contains(reserva.getStatus())) {
        throw new RuntimeException("Esta reserva já foi encerrada.");
    }

    LocalDateTime agora = LocalDateTime.now();
    reserva.setStatus(StatusReserva.CANCELADA);
    reserva.setCanceladaEm(agora);
    reservaRepo.save(reserva);

    LocalDateTime cursor = reserva.getFimPrevisto();
    while (true) {
        Optional<ReservaSala> proximo = reservaRepo
            .findByUsuarioIdESalaIdEInicioPrevisto(
                reserva.getUsuario().getId(),
                reserva.getSala().getId(),
                cursor,
                List.of(StatusReserva.APROVADA, StatusReserva.PENDENTE_APROVACAO)
            );
        if (proximo.isEmpty()) break;
        ReservaSala r = proximo.get();
        r.setStatus(StatusReserva.CANCELADA);
        r.setCanceladaEm(agora);
        reservaRepo.save(r);
        cursor = r.getFimPrevisto();
    }

    return reserva;
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

    public List<ReservaSala> listarPorEmailUsuario(String email) {
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        return reservaRepo.findByUsuarioId(usuario.getId());
    }

    public List<ReservaSala> listarTodas() {
        return reservaRepo.findAll();
    }

    public List<LocalDateTime> horariosOcupados(Long salaId, LocalDateTime data) {
    List<Object[]> reservas = reservaRepo.findHorariosOcupados(salaId, data);
    List<LocalDateTime> blocos = new ArrayList<>();
    for (Object[] row : reservas) {
        LocalDateTime cursor = (LocalDateTime) row[0];
        LocalDateTime fim    = (LocalDateTime) row[1];
        while (cursor.isBefore(fim)) {
            blocos.add(cursor);
            cursor = cursor.plusMinutes(DURACAO_MINUTOS);
        }
    }
    return blocos;
}

    // ─── Helpers privados ─────────────────────────────────────────────────────

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

}