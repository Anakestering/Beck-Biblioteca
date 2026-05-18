package com.example.demo.service;

import com.example.demo.dto.PedidoReservaDTO;
import com.example.demo.entity.*;
import com.example.demo.enums.StatusAprovacao;
import com.example.demo.enums.StatusReserva;
import com.example.demo.enums.TipoPedido;
import com.example.demo.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoReservaService {

    private static final int DURACAO_MINUTOS = 45;
    private static final int MAX_TEMPOS_SEM_APROVACAO = 2;
    private static final int MAX_PCS_SEM_APROVACAO = 2;

    private static final List<StatusReserva> STATUS_BLOQUEADORES = List.of(
            StatusReserva.APROVADA,
            StatusReserva.PENDENTE_APROVACAO,
            StatusReserva.EM_ANDAMENTO);

    private final PedidoReservaRepository pedidoRepo;
    private final ComputadorRepository computadorRepo;
    private final SalaRepository salaRepo;
    private final UsuarioRepository usuarioRepo;
    private final ReservaComputadorRepository reservaComputadorRepo;
    private final ReservaSalaRepository reservaSalaRepo;
    private final AprovacaoReservaRepository aprovacaoRepo;

    public PedidoReservaService(
            PedidoReservaRepository pedidoRepo,
            ComputadorRepository computadorRepo,
            SalaRepository salaRepo,
            UsuarioRepository usuarioRepo,
            ReservaComputadorRepository reservaComputadorRepo,
            ReservaSalaRepository reservaSalaRepo,
            AprovacaoReservaRepository aprovacaoRepo) {
        this.pedidoRepo = pedidoRepo;
        this.computadorRepo = computadorRepo;
        this.salaRepo = salaRepo;
        this.usuarioRepo = usuarioRepo;
        this.reservaComputadorRepo = reservaComputadorRepo;
        this.reservaSalaRepo = reservaSalaRepo;
        this.aprovacaoRepo = aprovacaoRepo;
    }

    @Transactional
    public PedidoReserva criar(PedidoReservaDTO dto, String emailUsuarioLogado) {
        Usuario usuarioLogado = usuarioRepo.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        Usuario dono = dto.getUsuarioId() != null
                ? usuarioRepo.findById(dto.getUsuarioId())
                        .orElseThrow(() -> new RuntimeException("Usuário não encontrado."))
                : usuarioLogado;

        LocalDateTime inicio = dto.getInicioPrevisto();
        LocalDateTime fim = dto.getFimPrevisto();

        long minutos = java.time.Duration.between(inicio, fim).toMinutes();
        int blocos = (int) (minutos / DURACAO_MINUTOS);

        boolean precisaAprovacao;
        if (dto.getTipo() == TipoPedido.COMPUTADOR) {
            precisaAprovacao = dto.getItemIds().size() > MAX_PCS_SEM_APROVACAO
                    || blocos > MAX_TEMPOS_SEM_APROVACAO;
        } else {
            precisaAprovacao = dto.getItemIds().size() > 1
                    || blocos > MAX_TEMPOS_SEM_APROVACAO;
        }

        StatusReserva status = precisaAprovacao
                ? StatusReserva.PENDENTE_APROVACAO
                : StatusReserva.APROVADA;

        PedidoReserva pedido = new PedidoReserva();
        pedido.setUsuario(dono);
        pedido.setCriadaPorUsuario(usuarioLogado);
        pedido.setTipo(dto.getTipo());
        pedido.setInicioPrevisto(inicio);
        pedido.setFimPrevisto(fim);
        pedido.setQtdePessoas(dto.getQtdePessoas());
        pedido.setObservacao(dto.getObservacao());
        pedido.setStatus(status);
        pedido = pedidoRepo.save(pedido);

        if (dto.getTipo() == TipoPedido.COMPUTADOR) {
            for (Long computadorId : dto.getItemIds()) {
                Computador computador = computadorRepo.findById(computadorId)
                        .orElseThrow(() -> new RuntimeException("Computador não encontrado: " + computadorId));

                if (!reservaComputadorRepo.findSobrepostas(computadorId, inicio, fim, STATUS_BLOQUEADORES).isEmpty()) {
                    throw new RuntimeException("Computador " + computador.getCodigo() + " já está reservado neste horário.");
                }

                ReservaComputador r = new ReservaComputador();
                r.setPedido(pedido);
                r.setComputador(computador);
                r.setUsuario(dono);
                r.setCriadaPorUsuario(usuarioLogado);
                r.setInicioPrevisto(inicio);
                r.setFimPrevisto(fim);
                r.setQtdePessoas(Math.min(dto.getQtdePessoas(), computador.getCapacidadePessoas()));
                r.setObservacao(dto.getObservacao());
                r.setStatus(status);
                reservaComputadorRepo.save(r);
            }
        } else {
            for (Long salaId : dto.getItemIds()) {
                Sala sala = salaRepo.findById(salaId)
                        .orElseThrow(() -> new RuntimeException("Sala não encontrada: " + salaId));

                if (!reservaSalaRepo.findSobrepostas(salaId, inicio, fim, STATUS_BLOQUEADORES).isEmpty()) {
                    throw new RuntimeException("Sala " + sala.getNome() + " já está reservada neste horário.");
                }

                ReservaSala r = new ReservaSala();
                r.setPedido(pedido);
                r.setSala(sala);
                r.setUsuario(dono);
                r.setCriadaPorUsuario(usuarioLogado);
                r.setInicioPrevisto(inicio);
                r.setFimPrevisto(fim);
                r.setQtdePessoas(Math.min(dto.getQtdePessoas(), sala.getCapacidadePessoas()));
                r.setObservacao(dto.getObservacao());
                r.setStatus(status);
                reservaSalaRepo.save(r);
            }
        }

        if (precisaAprovacao) {
            AprovacaoReserva aprovacao = new AprovacaoReserva();
            aprovacao.setPedido(pedido);
            aprovacao.setStatus(StatusAprovacao.PENDENTE);
            aprovacao.setSolicitadaEm(LocalDateTime.now());
            aprovacaoRepo.save(aprovacao);
        }

        return pedidoRepo.findById(pedido.getId()).orElseThrow();
    }

    public List<PedidoReserva> listarPorEmailUsuario(String email) {
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        return pedidoRepo.findByUsuarioId(usuario.getId());
    }

    public List<PedidoReserva> listarTodos() {
        return pedidoRepo.findTodos();
    }

    @Transactional
public PedidoReserva cancelarComoAdmin(Long pedidoId) {
    PedidoReserva pedido = pedidoRepo.findById(pedidoId)
        .orElseThrow(() -> new RuntimeException("Pedido não encontrado."));

    LocalDateTime agora = LocalDateTime.now();

    for (ReservaComputador r : pedido.getReservasComputador()) {
        if (!List.of(StatusReserva.CANCELADA, StatusReserva.FINALIZADA, StatusReserva.ATRASADO)
                .contains(r.getStatus())) {
            r.setStatus(StatusReserva.CANCELADA);
            r.setCanceladaEm(agora);
        }
    }

    for (ReservaSala r : pedido.getReservasSala()) {
        if (!List.of(StatusReserva.CANCELADA, StatusReserva.FINALIZADA, StatusReserva.ATRASADO)
                .contains(r.getStatus())) {
            r.setStatus(StatusReserva.CANCELADA);
            r.setCanceladaEm(agora);
        }
    }

    pedido.setStatus(StatusReserva.CANCELADA); 
    pedidoRepo.save(pedido);

    return pedido;

    
}
}