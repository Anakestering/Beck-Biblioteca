package com.example.demo.service;

import com.example.demo.dtoEstatisticas.*;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class EstatisticasService {

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

    public List<EstatisticasRecursoDTO> getRecursosSalas(LocalDateTime inicio, LocalDateTime fim, List<Long> salaIds) {
        List<Sala> salas = salaIds != null && !salaIds.isEmpty()
                ? salaRepo.findAllById(salaIds)
                : salaRepo.findAll();

        List<ReservaSala> reservas = reservaSalaRepo.findFinalizadasParaEstatisticas(inicio, fim, salaIds);

        Map<Long, Long> minutos = new HashMap<>();
        Map<Long, Long> contagem = new HashMap<>();
        for (ReservaSala r : reservas) {
            Long id = r.getSala().getId();
            long mins = Duration.between(r.getCheckinEm(), r.getCheckoutEm()).toMinutes();
            minutos.merge(id, mins, Long::sum);
            contagem.merge(id, 1L, Long::sum);
        }

        return salas.stream()
                .map(s -> new EstatisticasRecursoDTO(
                        s.getId(),
                        s.getNome(),
                        minutos.getOrDefault(s.getId(), 0L),
                        contagem.getOrDefault(s.getId(), 0L)))
                .sorted(Comparator.comparing(EstatisticasRecursoDTO::nome))
                .toList();
    }

    public List<EstatisticasRecursoDTO> getRecursosComputadores(LocalDateTime inicio, LocalDateTime fim,
            List<Long> computadorIds) {
        List<Computador> computadores = computadorIds != null && !computadorIds.isEmpty()
                ? computadorRepo.findAllById(computadorIds)
                : computadorRepo.findAll();

        List<ReservaComputador> reservas = reservaComputadorRepo.findFinalizadasParaEstatisticas(inicio, fim,
                computadorIds);

        Map<Long, Long> minutos = new HashMap<>();
        Map<Long, Long> contagem = new HashMap<>();
        for (ReservaComputador r : reservas) {
            Long id = r.getComputador().getId();
            long mins = Duration.between(r.getCheckinEm(), r.getCheckoutEm()).toMinutes();
            minutos.merge(id, mins, Long::sum);
            contagem.merge(id, 1L, Long::sum);
        }

        return computadores.stream()
                .map(c -> new EstatisticasRecursoDTO(
                        c.getId(),
                        c.getCodigo(),
                        minutos.getOrDefault(c.getId(), 0L),
                        contagem.getOrDefault(c.getId(), 0L)))
                .sorted(Comparator.comparing(EstatisticasRecursoDTO::nome))
                .toList();
    }

    // ─── Status das Reservas ──────────────────────────────────────────────────

    public EstatisticasReservasDTO getStatusReservas(LocalDateTime inicio, LocalDateTime fim, List<Long> salaIds,
            List<Long> computadorIds) {
        long finalizadas = 0, canceladas = 0, atrasadas = 0, rejeitadas = 0;

        if (salaIds == null || !salaIds.isEmpty()) {
            List<Object[]> rows = reservaSalaRepo.findStatusReservasParaEstatisticas(inicio, fim, salaIds);
            for (Object[] row : rows) {
                String status = row[1].toString();
                long count = ((Number) row[2]).longValue();
                switch (status) {
                    case "FINALIZADA" -> finalizadas += count;
                    case "CANCELADA"  -> canceladas  += count;
                    case "ATRASADO"   -> atrasadas   += count;
                    case "REJEITADA"  -> rejeitadas  += count;
                }
            }
        }

        if (computadorIds == null || !computadorIds.isEmpty()) {
            List<Object[]> rows = reservaComputadorRepo.findStatusReservasParaEstatisticas(inicio, fim, computadorIds);
            for (Object[] row : rows) {
                String status = row[1].toString();
                long count = ((Number) row[2]).longValue();
                switch (status) {
                    case "FINALIZADA" -> finalizadas += count;
                    case "CANCELADA"  -> canceladas  += count;
                    case "ATRASADO"   -> atrasadas   += count;
                    case "REJEITADA"  -> rejeitadas  += count;
                }
            }
        }

        long total = finalizadas + canceladas + atrasadas + rejeitadas;
        return new EstatisticasReservasDTO(finalizadas, canceladas, atrasadas, rejeitadas, total);
    }

    // ─── Usuários ─────────────────────────────────────────────────────────────

    public Map<String, Object> getUsuarioStats(LocalDateTime inicio, LocalDateTime fim) {
        long total   = usuarioRepo.countByAtivo(true);
        long novos   = usuarioRepo.countNovosUsuarios(inicio, fim);
        long ativos  = usuarioRepo.countUsuariosAtivos(inicio, fim);
        long inativos = total - ativos;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total",    total);
        stats.put("novos",    novos);
        stats.put("ativos",   ativos);
        stats.put("inativos", inativos);
        return stats;
    }

    public List<EstatisticasUsuarioDTO> getRankingUsuarios() {
        return usuarioRepo.findRankingUsuarios().stream()
                .map(row -> new EstatisticasUsuarioDTO(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue()))
                .toList();
    }

    // ─── Histórico Linear ─────────────────────────────────────────────────────

    public List<EstatisticasLinearDTO> getHistorico(LocalDateTime inicio, LocalDateTime fim, String agrupamento) {
        List<ReservaSala>      reservasSala = reservaSalaRepo.findFinalizadasParaEstatisticas(inicio, fim, null);
        List<ReservaComputador> reservasPc  = reservaComputadorRepo.findFinalizadasParaEstatisticas(inicio, fim, null);

        // Usa LocalDate como chave para que o TreeMap ordene cronologicamente
        // (strings "dd/MM/yyyy" ordenariam errado)
        TreeMap<LocalDate, Long> agregado = new TreeMap<>();

        for (ReservaSala r : reservasSala) {
            if (r.getCheckinEm() == null) continue;
            agregado.merge(chaveData(r.getCheckinEm(), agrupamento), 1L, Long::sum);
        }
        for (ReservaComputador r : reservasPc) {
            if (r.getCheckinEm() == null) continue;
            agregado.merge(chaveData(r.getCheckinEm(), agrupamento), 1L, Long::sum);
        }

        // Serializa como "yyyy-MM-dd" — formato ISO, único aceito pelo front
        return agregado.entrySet().stream()
                .map(e -> new EstatisticasLinearDTO(e.getKey().toString(), e.getValue()))
                .toList();
    }

    /**
     * Retorna o primeiro dia do período ao qual o datetime pertence:
     *   dia    → o próprio dia
     *   semana → segunda-feira da semana ISO
     *   mes    → dia 1 do mês
     */
    private LocalDate chaveData(LocalDateTime dt, String agrupamento) {
        LocalDate d = dt.toLocalDate();
        return switch (agrupamento) {
            case "semana" -> d.with(WeekFields.ISO.dayOfWeek(), 1);
            case "mes"    -> d.withDayOfMonth(1);
            default       -> d;
        };
    }
}