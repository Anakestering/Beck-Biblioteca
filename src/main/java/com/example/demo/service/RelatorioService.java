package com.example.demo.service;

import com.example.demo.dtoRelatorio.*;
import com.example.demo.entity.Computador;
import com.example.demo.entity.ReservaComputador;
import com.example.demo.entity.ReservaSala;
import com.example.demo.entity.Sala;
import com.example.demo.repository.ComputadorRepository;
import com.example.demo.repository.ReservaComputadorRepository;
import com.example.demo.repository.ReservaSalaRepository;
import com.example.demo.repository.SalaRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RelatorioService {

    @Autowired
    private ReservaSalaRepository reservaSalaRepo;
    @Autowired
    private ReservaComputadorRepository reservaComputadorRepo;
    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private SalaRepository salaRepo;
    @Autowired
    private ComputadorRepository computadorRepo;

    // ─── Recursos (Salas e PCs) ───────────────────────────────────────────────

    public List<RelatorioRecursoDTO> getRecursosSalas(LocalDateTime inicio, LocalDateTime fim, List<Long> salaIds) {
        // Busca todas as salas selecionadas
        List<Sala> salas = salaIds != null && !salaIds.isEmpty()
                ? salaRepo.findAllById(salaIds)
                : salaRepo.findAll();

        // Busca reservas finalizadas
        List<ReservaSala> reservas = reservaSalaRepo.findFinalizadasParaRelatorio(inicio, fim, salaIds);

        // Agrega por sala
        Map<Long, Long> minutos = new HashMap<>();
        Map<Long, Long> contagem = new HashMap<>();
        for (ReservaSala r : reservas) {
            Long id = r.getSala().getId();
            long mins = Duration.between(r.getCheckinEm(), r.getCheckoutEm()).toMinutes();
            minutos.merge(id, mins, Long::sum);
            contagem.merge(id, 1L, Long::sum);
        }

        // Retorna todos os recursos, mesmo os zerados
        return salas.stream()
                .map(s -> new RelatorioRecursoDTO(
                        s.getId(),
                        s.getNome(),
                        minutos.getOrDefault(s.getId(), 0L),
                        contagem.getOrDefault(s.getId(), 0L)))
                .sorted(Comparator.comparing(RelatorioRecursoDTO::nome))
                .toList();
    }

    public List<RelatorioRecursoDTO> getRecursosComputadores(LocalDateTime inicio, LocalDateTime fim,
            List<Long> computadorIds) {
        // Busca todos os computadores selecionados
        List<Computador> computadores = computadorIds != null && !computadorIds.isEmpty()
                ? computadorRepo.findAllById(computadorIds)
                : computadorRepo.findAll();

        // Busca reservas finalizadas
        List<ReservaComputador> reservas = reservaComputadorRepo.findFinalizadasParaRelatorio(inicio, fim,
                computadorIds);

        // Agrega por computador
        Map<Long, Long> minutos = new HashMap<>();
        Map<Long, Long> contagem = new HashMap<>();
        for (ReservaComputador r : reservas) {
            Long id = r.getComputador().getId();
            long mins = Duration.between(r.getCheckinEm(), r.getCheckoutEm()).toMinutes();
            minutos.merge(id, mins, Long::sum);
            contagem.merge(id, 1L, Long::sum);
        }

        // Retorna todos os recursos, mesmo os zerados
        return computadores.stream()
                .map(c -> new RelatorioRecursoDTO(
                        c.getId(),
                        c.getCodigo(),
                        minutos.getOrDefault(c.getId(), 0L),
                        contagem.getOrDefault(c.getId(), 0L)))
                .sorted(Comparator.comparing(RelatorioRecursoDTO::nome))
                .toList();
    }

    // ─── Status das Reservas ──────────────────────────────────────────────────

    public RelatorioStatusReservasDTO getStatusReservas(LocalDateTime inicio, LocalDateTime fim, List<Long> salaIds,
            List<Long> computadorIds) {
        long finalizadas = 0, canceladas = 0, atrasadas = 0, rejeitadas = 0;

        if (salaIds == null || !salaIds.isEmpty()) {
            List<Object[]> rows = reservaSalaRepo.findStatusReservasParaRelatorio(inicio, fim, salaIds);
            for (Object[] row : rows) {
                String status = row[1].toString();
                long count = ((Number) row[2]).longValue();
                switch (status) {
                    case "FINALIZADA" -> finalizadas += count;
                    case "CANCELADA" -> canceladas += count;
                    case "ATRASADO" -> atrasadas += count;
                    case "REJEITADA" -> rejeitadas += count;
                }
            }
        }

        if (computadorIds == null || !computadorIds.isEmpty()) {
            List<Object[]> rows = reservaComputadorRepo.findStatusReservasParaRelatorio(inicio, fim, computadorIds);
            for (Object[] row : rows) {
                String status = row[1].toString();
                long count = ((Number) row[2]).longValue();
                switch (status) {
                    case "FINALIZADA" -> finalizadas += count;
                    case "CANCELADA" -> canceladas += count;
                    case "ATRASADO" -> atrasadas += count;
                    case "REJEITADA" -> rejeitadas += count;
                }
            }
        }

        long total = finalizadas + canceladas + atrasadas + rejeitadas;
        return new RelatorioStatusReservasDTO(finalizadas, canceladas, atrasadas, rejeitadas, total);
    }

    

    // ─── Usuários ─────────────────────────────────────────────────────────────

    public Map<String, Object> getUsuarioStats(LocalDateTime inicio, LocalDateTime fim) {
        long total = usuarioRepo.countByAtivo(true);
        long novos = usuarioRepo.countNovosUsuarios(inicio, fim);
        long ativos = usuarioRepo.countUsuariosAtivos(inicio, fim);
        long inativos = total - ativos;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("novos", novos);
        stats.put("ativos", ativos);
        stats.put("inativos", inativos);
        return stats;
    }

    public List<RelatorioUsuarioDTO> getRankingUsuarios() {
        return usuarioRepo.findRankingUsuarios().stream()
                .map(row -> new RelatorioUsuarioDTO(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue()))
                .toList();
    }
}