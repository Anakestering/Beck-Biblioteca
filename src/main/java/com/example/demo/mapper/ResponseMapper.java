package com.example.demo.mapper;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResponseMapper {

    public UsuarioResponseDTO toUsuarioDTO(Usuario u) {
        if (u == null)
            return null;
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(u.getId());
        dto.setNome(u.getNome());
        dto.setCpf(u.getCpf());
        dto.setEmail(u.getEmail());
        dto.setTelefone(u.getTelefone());
        dto.setNivelAcesso(u.getNivelAcesso() != null ? u.getNivelAcesso().name() : null);
        dto.setAtivo(u.isAtivo());
        dto.setCreatedAt(u.getCreatedAt());
        dto.setUpdatedAt(u.getUpdatedAt());
        return dto;
    }

    public ComputadorResponseDTO toComputadorDTO(Computador c) {
        if (c == null)
            return null;
        ComputadorResponseDTO dto = new ComputadorResponseDTO();
        dto.setId(c.getId());
        dto.setCodigo(c.getCodigo());
        dto.setCapacidadePessoas(c.getCapacidadePessoas());
        dto.setObservacao(c.getObservacao());
        dto.setAtivo(c.isAtivo());
        return dto;
    }

    public SalaResponseDTO toSalaDTO(Sala s) {
        if (s == null)
            return null;
        SalaResponseDTO dto = new SalaResponseDTO();
        dto.setId(s.getId());
        dto.setNome(s.getNome());
        dto.setCapacidadePessoas(s.getCapacidadePessoas());
        dto.setAtivo(s.isAtivo());
        return dto;
    }

    public ReservaComputadorResponseDTO toReservaComputadorDTO(ReservaComputador r) {
        if (r == null)
            return null;
        ReservaComputadorResponseDTO dto = new ReservaComputadorResponseDTO();
        dto.setId(r.getId());
        dto.setComputador(toComputadorDTO(r.getComputador()));
        dto.setStatus(r.getStatus());
        dto.setInicioPrevisto(r.getInicioPrevisto());
        dto.setFimPrevisto(r.getFimPrevisto());
        dto.setQtdePessoas(r.getQtdePessoas());
        dto.setObservacao(r.getObservacao());
        dto.setCheckinEm(r.getCheckinEm());
        dto.setCheckoutEm(r.getCheckoutEm());
        dto.setCanceladaEm(r.getCanceladaEm());
        dto.setAtrasadoEm(r.getAtrasadoEm());
        dto.setCheckoutAutomatico(r.isCheckoutAutomatico());
        return dto;
    }

    public ReservaSalaResponseDTO toReservaSalaDTO(ReservaSala r) {
        if (r == null)
            return null;
        ReservaSalaResponseDTO dto = new ReservaSalaResponseDTO();
        dto.setId(r.getId());
        dto.setSala(toSalaDTO(r.getSala()));
        dto.setStatus(r.getStatus());
        dto.setInicioPrevisto(r.getInicioPrevisto());
        dto.setFimPrevisto(r.getFimPrevisto());
        dto.setQtdePessoas(r.getQtdePessoas());
        dto.setObservacao(r.getObservacao());
        dto.setCheckinEm(r.getCheckinEm());
        dto.setCheckoutEm(r.getCheckoutEm());
        dto.setCanceladaEm(r.getCanceladaEm());
        dto.setAtrasadoEm(r.getAtrasadoEm());
        dto.setCheckoutAutomatico(r.isCheckoutAutomatico());
        return dto;
    }

    public PedidoReservaResponseDTO toPedidoDTO(PedidoReserva p) {
        if (p == null)
            return null;
        PedidoReservaResponseDTO dto = new PedidoReservaResponseDTO();
        dto.setId(p.getId());
        dto.setUsuario(toUsuarioDTO(p.getUsuario()));
        dto.setCriadaPorUsuario(toUsuarioDTO(p.getCriadaPorUsuario()));
        dto.setTipo(p.getTipo());
        dto.setStatus(p.getStatus());
        dto.setInicioPrevisto(p.getInicioPrevisto());
        dto.setFimPrevisto(p.getFimPrevisto());
        dto.setQtdePessoas(p.getQtdePessoas());
        dto.setObservacao(p.getObservacao());

        // Deduplica por computador — pega só o primeiro bloco de cada PC
        dto.setReservasComputador(
                p.getReservasComputador().stream()
                        .collect(java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toMap(
                                        r -> r.getComputador().getId(),
                                        this::toReservaComputadorDTO,
                                        (a, b) -> a,
                                        java.util.LinkedHashMap::new),
                                m -> new java.util.ArrayList<>(m.values()))));

        // Deduplica por sala — pega só o primeiro bloco de cada sala
        dto.setReservasSala(
                p.getReservasSala().stream()
                        .collect(java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toMap(
                                        r -> r.getSala().getId(),
                                        this::toReservaSalaDTO,
                                        (a, b) -> a,
                                        java.util.LinkedHashMap::new),
                                m -> new java.util.ArrayList<>(m.values()))));

        return dto;
    }

    public AprovacaoReservaResponseDTO toAprovacaoDTO(AprovacaoReserva a) {
        if (a == null)
            return null;
        AprovacaoReservaResponseDTO dto = new AprovacaoReservaResponseDTO();
        dto.setId(a.getId());
        dto.setPedido(toPedidoDTO(a.getPedido()));
        dto.setStatus(a.getStatus());
        dto.setSolicitadaEm(a.getSolicitadaEm());
        dto.setDecididaEm(a.getDecididaEm());
        dto.setDecididaPorUsuario(toUsuarioDTO(a.getDecididaPorUsuario()));
        dto.setMotivo(a.getMotivo());
        return dto;
    }

    public List<PedidoReservaResponseDTO> toPedidoList(List<PedidoReserva> list) {
        return list.stream().map(this::toPedidoDTO).toList();
    }

    public List<AprovacaoReservaResponseDTO> toAprovacaoList(List<AprovacaoReserva> list) {
        return list.stream().map(this::toAprovacaoDTO).toList();
    }
}