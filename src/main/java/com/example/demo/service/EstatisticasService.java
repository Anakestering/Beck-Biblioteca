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
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class EstatisticasService {

    private final ReservaSalaRepository reservaSalaRepo;
    private final ReservaComputadorRepository reservaComputadorRepo;
    private final UsuarioRepository usuarioRepo;
    private final SalaRepository salaRepo;
    private final ComputadorRepository computadorRepo;

    public EstatisticasService(
            ReservaSalaRepository reservaSalaRepo,
            ReservaComputadorRepository reservaComputadorRepo,
            UsuarioRepository usuarioRepo,
            SalaRepository salaRepo,
            ComputadorRepository computadorRepo) {
        this.reservaSalaRepo = reservaSalaRepo;
        this.reservaComputadorRepo = reservaComputadorRepo;
        this.usuarioRepo = usuarioRepo;
        this.salaRepo = salaRepo;
        this.computadorRepo = computadorRepo;
    }

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

        long disponiveis = calcularMinutosDisponiveis(inicio, fim);

        return salas.stream()
                .map(s -> new EstatisticasRecursoDTO(
                        s.getId(),
                        s.getNome(),
                        minutos.getOrDefault(s.getId(), 0L),
                        contagem.getOrDefault(s.getId(), 0L),
                        disponiveis))
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

        long disponiveis = calcularMinutosDisponiveis(inicio, fim);

        return computadores.stream()
                .map(c -> new EstatisticasRecursoDTO(
                        c.getId(),
                        c.getCodigo(),
                        minutos.getOrDefault(c.getId(), 0L),
                        contagem.getOrDefault(c.getId(), 0L),
                        disponiveis))
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
                    case "CANCELADA" -> canceladas += count;
                    case "ATRASADO" -> atrasadas += count;
                    case "REJEITADA" -> rejeitadas += count;
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
                    case "CANCELADA" -> canceladas += count;
                    case "ATRASADO" -> atrasadas += count;
                    case "REJEITADA" -> rejeitadas += count;
                }
            }
        }

        long total = finalizadas + canceladas + atrasadas + rejeitadas;
        return new EstatisticasReservasDTO(finalizadas, canceladas, atrasadas, rejeitadas, total);
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

    public EstatisticasHistoricoDTO getHistorico(LocalDateTime inicio, LocalDateTime fim, String agrupamento) {
        List<ReservaSala> reservasSala = reservaSalaRepo.findFinalizadasParaEstatisticas(inicio, fim, null);
        List<ReservaComputador> reservasPc = reservaComputadorRepo.findFinalizadasParaEstatisticas(inicio, fim, null);
        List<ReservaSala> atrasadasSala = reservaSalaRepo.findAtrasadasParaEstatisticas(inicio, fim);
        List<ReservaComputador> atrasadasPc = reservaComputadorRepo.findAtrasadasParaEstatisticas(inicio, fim);

        // ─── Agregação de finalizadas ─────────────────────────────────────────
        TreeMap<LocalDate, Long> agregado = new TreeMap<>();
        for (ReservaSala r : reservasSala) {
            if (r.getCheckinEm() == null) continue;
            agregado.merge(chaveData(r.getCheckinEm(), agrupamento), 1L, Long::sum);
        }
        for (ReservaComputador r : reservasPc) {
            if (r.getCheckinEm() == null) continue;
            agregado.merge(chaveData(r.getCheckinEm(), agrupamento), 1L, Long::sum);
        }

        // ─── Agregação de abandonos ───────────────────────────────────────────
        TreeMap<LocalDate, Long> agregadoAbandono = new TreeMap<>();
        for (ReservaSala r : atrasadasSala) {
            agregadoAbandono.merge(chaveData(r.getInicioPrevisto(), agrupamento), 1L, Long::sum);
        }
        for (ReservaComputador r : atrasadasPc) {
            agregadoAbandono.merge(chaveData(r.getInicioPrevisto(), agrupamento), 1L, Long::sum);
        }

        // ─── Média de pessoas por dia útil ────────────────────────────────────
        long totalPessoas = reservasSala.stream()
                .filter(r -> r.getCheckinEm() != null)
                .mapToLong(r -> r.getQtdePessoas()).sum()
                + reservasPc.stream()
                        .filter(r -> r.getCheckinEm() != null)
                        .mapToLong(r -> r.getQtdePessoas()).sum();
        long diasUteis = calcularDiasUteis(inicio, fim);
        double mediaPessoasDia = diasUteis > 0
                ? Math.round((totalPessoas * 10.0) / diasUteis) / 10.0
                : 0.0;

        // ─── Taxa de abandono global no período ───────────────────────────────
        long totalFinalizadas = reservasSala.size() + reservasPc.size();
        long totalAtrasadas = atrasadasSala.size() + atrasadasPc.size();
        double taxaAbandono = (totalFinalizadas + totalAtrasadas) > 0
                ? Math.round((totalAtrasadas * 1000.0) / (totalFinalizadas + totalAtrasadas)) / 10.0
                : 0.0;

        // ─── Janela da média móvel ────────────────────────────────────────────
        int janela = switch (agrupamento) {
            case "semana" -> 4;
            case "mes" -> 3;
            default -> 7;
        };

        // ─── Pontos de finalizadas com média móvel ────────────────────────────
        List<Map.Entry<LocalDate, Long>> entries = new ArrayList<>(agregado.entrySet());
        List<EstatisticasHistoricoDTO.Ponto> pontos = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<LocalDate, Long> e = entries.get(i);
            Double mm = null;
            if (i >= janela - 1) {
                double soma = 0;
                for (int j = i - janela + 1; j <= i; j++)
                    soma += entries.get(j).getValue();
                mm = soma / janela;
            }
            pontos.add(new EstatisticasHistoricoDTO.Ponto(e.getKey().toString(), e.getValue(), mm));
        }

        // ─── Pontos de abandonos com média móvel ──────────────────────────────
        List<Map.Entry<LocalDate, Long>> entriesAbandono = new ArrayList<>(agregadoAbandono.entrySet());
        List<EstatisticasHistoricoDTO.PontoAbandono> abandonos = new ArrayList<>();
        for (int i = 0; i < entriesAbandono.size(); i++) {
            Map.Entry<LocalDate, Long> e = entriesAbandono.get(i);
            Double mm = null;
            if (i >= janela - 1) {
                double soma = 0;
                for (int j = i - janela + 1; j <= i; j++)
                    soma += entriesAbandono.get(j).getValue();
                mm = soma / janela;
            }
            abandonos.add(new EstatisticasHistoricoDTO.PontoAbandono(e.getKey().toString(), e.getValue(), mm));
        }

        // ─── Tendências ───────────────────────────────────────────────────────
        EstatisticasHistoricoDTO.Tendencia tendencia = calcularTendencia(entries);
        EstatisticasHistoricoDTO.Tendencia tendenciaAbandono = calcularTendencia(entriesAbandono);

        return new EstatisticasHistoricoDTO(pontos, abandonos, tendencia, tendenciaAbandono, mediaPessoasDia, taxaAbandono);
    }

    /**
     * Compara a média de reservas da primeira metade do período com a segunda.
     * Retorna null se houver menos de 4 pontos (amostra insuficiente).
     */
    private EstatisticasHistoricoDTO.Tendencia calcularTendencia(List<Map.Entry<LocalDate, Long>> entries) {
        if (entries.size() < 4)
            return null;
        int meio = entries.size() / 2;
        double mediaAntes = entries.subList(0, meio).stream()
                .mapToLong(Map.Entry::getValue).average().orElse(0);
        double mediaDepois = entries.subList(meio, entries.size()).stream()
                .mapToLong(Map.Entry::getValue).average().orElse(0);
        if (mediaAntes == 0)
            return null;
        double pct = ((mediaDepois - mediaAntes) / mediaAntes) * 100;
        return new EstatisticasHistoricoDTO.Tendencia(Math.abs(pct), pct >= 0);
    }

    // ─── Ocupação por dia da semana ───────────────────────────────────────────

    public List<EstatisticasOcupacaoDiaDTO> getOcupacaoSemana(LocalDateTime inicio, LocalDateTime fim) {
        List<ReservaSala> reservasSala = reservaSalaRepo.findFinalizadasParaEstatisticas(inicio, fim, null);
        List<ReservaComputador> reservasPc = reservaComputadorRepo.findFinalizadasParaEstatisticas(inicio, fim, null);

        // Minutos usados agrupados por dia da semana (1=Seg ... 5=Sex)
        Map<Integer, Long> minutosUsadosPorDia = new HashMap<>();
        for (int i = 1; i <= 5; i++)
            minutosUsadosPorDia.put(i, 0L);

        for (ReservaSala r : reservasSala) {
            if (r.getCheckinEm() == null || r.getCheckoutEm() == null)
                continue;
            int dow = r.getCheckinEm().getDayOfWeek().getValue(); // 1=Seg...7=Dom
            if (dow > 5)
                continue;
            long mins = Duration.between(r.getCheckinEm(), r.getCheckoutEm()).toMinutes();
            minutosUsadosPorDia.merge(dow, mins, Long::sum);
        }
        for (ReservaComputador r : reservasPc) {
            if (r.getCheckinEm() == null || r.getCheckoutEm() == null)
                continue;
            int dow = r.getCheckinEm().getDayOfWeek().getValue();
            if (dow > 5)
                continue;
            long mins = Duration.between(r.getCheckinEm(), r.getCheckoutEm()).toMinutes();
            minutosUsadosPorDia.merge(dow, mins, Long::sum);
        }

        // Quantas vezes cada dia da semana aparece no período (ex: 52 segundas num ano)
        Map<Integer, Long> ocorrenciasPorDia = new HashMap<>();
        for (int i = 1; i <= 5; i++)
            ocorrenciasPorDia.put(i, 0L);
        LocalDate cursor = inicio.toLocalDate();
        LocalDate fimDate = fim.toLocalDate();
        while (!cursor.isAfter(fimDate)) {
            int dow = cursor.getDayOfWeek().getValue();
            if (dow <= 5)
                ocorrenciasPorDia.merge(dow, 1L, Long::sum);
            cursor = cursor.plusDays(1);
        }

        // Taxa = minutos usados / (ocorrencias * 900min disponíveis por dia)
        final long MINUTOS_DIA = 15 * 60L; // 7h–22h = 900min
        String[] nomes = { "", "Seg", "Ter", "Qua", "Qui", "Sex" };

        List<EstatisticasOcupacaoDiaDTO> resultado = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            long totalRecursos = computadorRepo.countByAtivoTrue() + salaRepo.countByAtivoTrue();
            long disponiveis = ocorrenciasPorDia.get(i) * MINUTOS_DIA * totalRecursos;
            double taxa = disponiveis > 0
                    ? Math.min(100.0, Math.round((minutosUsadosPorDia.get(i) * 1000.0) / disponiveis) / 10.0)
                    : 0.0;
            resultado.add(new EstatisticasOcupacaoDiaDTO(i, nomes[i], taxa));
        }
        return resultado;
    }

    private long calcularDiasUteis(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null)
            return 0;
        long diasUteis = 0;
        LocalDate cursor = inicio.toLocalDate();
        LocalDate fimDate = fim.toLocalDate();
        while (!cursor.isAfter(fimDate)) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY)
                diasUteis++;
            cursor = cursor.plusDays(1);
        }
        return diasUteis;
    }

    /**
     * Conta quantos minutos úteis existem num período (seg–sex, 7h–22h =
     * 900min/dia).
     * Usado para calcular o percentual de ocupação de salas e PCs.
     */
    private long calcularMinutosDisponiveis(LocalDateTime inicio, LocalDateTime fim) {
        final long MINUTOS_DIA = 15 * 60; // 7h às 22h = 15h = 900min
        return calcularDiasUteis(inicio, fim) * MINUTOS_DIA;
    }

    /**
     * Retorna o primeiro dia do período ao qual o datetime pertence:
     * dia → o próprio dia
     * semana → segunda-feira da semana ISO
     * mes → dia 1 do mês
     */
    private LocalDate chaveData(LocalDateTime dt, String agrupamento) {
        LocalDate d = dt.toLocalDate();
        return switch (agrupamento) {
            case "semana" -> d.with(WeekFields.ISO.dayOfWeek(), 1);
            case "mes" -> d.withDayOfMonth(1);
            default -> d;
        };
    }

    // ─── Resumo (cards do topo) ───────────────────────────────────────────────

    public EstatisticasResumoDTO getResumo(LocalDateTime inicio, LocalDateTime fim) {

        // Queries de contagem e minutos por status
        List<Object[]> rowsSala = reservaSalaRepo.findResumoParaEstatisticas(inicio, fim);
        List<Object[]> rowsPc   = reservaComputadorRepo.findResumoParaEstatisticas(inicio, fim);

        long finalizadas = 0, atrasadas = 0;

        for (Object[] row : rowsSala) {
            String status = row[0].toString();
            long count    = ((Number) row[1]).longValue();
            if ("FINALIZADA".equals(status)) finalizadas += count;
            if ("ATRASADO".equals(status))   atrasadas   += count;
        }
        for (Object[] row : rowsPc) {
            String status = row[0].toString();
            long count    = ((Number) row[1]).longValue();
            if ("FINALIZADA".equals(status)) finalizadas += count;
            if ("ATRASADO".equals(status))   atrasadas   += count;
        }

        double taxaNoShow = (finalizadas + atrasadas) > 0
                ? Math.round((atrasadas * 100.0 / (finalizadas + atrasadas)) * 10.0) / 10.0
                : 0.0;

        // Minutos usados e recurso mais usado — direto das reservas finalizadas
        List<ReservaSala> reservasSala = reservaSalaRepo.findFinalizadasParaEstatisticas(inicio, fim, null);
        List<ReservaComputador> reservasPc = reservaComputadorRepo.findFinalizadasParaEstatisticas(inicio, fim, null);

        long disponiveis = calcularMinutosDisponiveis(inicio, fim);

        // Agrupa minutos por sala
        Map<Long, long[]> minutosSala = new HashMap<>();
        for (ReservaSala r : reservasSala) {
            long mins = Duration.between(r.getCheckinEm(), r.getCheckoutEm()).toMinutes();
            minutosSala.computeIfAbsent(r.getSala().getId(), k -> new long[]{0, 0});
            minutosSala.get(r.getSala().getId())[0] += mins;
        }

        // Agrupa minutos por computador
        Map<Long, long[]> minutosPc = new HashMap<>();
        for (ReservaComputador r : reservasPc) {
            long mins = Duration.between(r.getCheckinEm(), r.getCheckoutEm()).toMinutes();
            minutosPc.computeIfAbsent(r.getComputador().getId(), k -> new long[]{0, 0});
            minutosPc.get(r.getComputador().getId())[0] += mins;
        }

        // Calcula ocupação média e recurso mais usado sem chamar os métodos públicos
        List<Sala> todasSalas = salaRepo.findAll();
        List<Computador> todosComputadores = computadorRepo.findAll();

        double somaOcupacao = 0;
        int totalComDisponiveis = 0;
        String nomeRecurso = "-";
        String tipoRecurso = "-";
        long maxMinutos = -1;

        for (Sala s : todasSalas) {
            long usados = minutosSala.containsKey(s.getId()) ? minutosSala.get(s.getId())[0] : 0;
            if (disponiveis > 0) {
                somaOcupacao += (usados * 100.0) / disponiveis;
                totalComDisponiveis++;
            }
            if (usados > maxMinutos) {
                maxMinutos = usados;
                nomeRecurso = s.getNome();
                tipoRecurso = "SALA";
            }
        }

        for (Computador c : todosComputadores) {
            long usados = minutosPc.containsKey(c.getId()) ? minutosPc.get(c.getId())[0] : 0;
            if (disponiveis > 0) {
                somaOcupacao += (usados * 100.0) / disponiveis;
                totalComDisponiveis++;
            }
            if (usados > maxMinutos) {
                maxMinutos = usados;
                nomeRecurso = c.getCodigo();
                tipoRecurso = "PC";
            }
        }

        double taxaOcupacao = totalComDisponiveis > 0
                ? Math.round((somaOcupacao / totalComDisponiveis) * 10.0) / 10.0
                : 0.0;

        return new EstatisticasResumoDTO(finalizadas, taxaOcupacao, taxaNoShow, nomeRecurso, tipoRecurso);
    }

}