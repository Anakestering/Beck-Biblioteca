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
import java.time.DayOfWeek;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

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

        TreeMap<LocalDate, Long> agregado = new TreeMap<>();
        for (ReservaSala r : reservasSala) {
            if (r.getCheckinEm() == null)
                continue;
            agregado.merge(chaveData(r.getCheckinEm(), agrupamento), 1L, Long::sum);
        }
        for (ReservaComputador r : reservasPc) {
            if (r.getCheckinEm() == null)
                continue;
            agregado.merge(chaveData(r.getCheckinEm(), agrupamento), 1L, Long::sum);
        }

        // Média de pessoas por dia útil
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

        // Converte para lista ordenada de {data, total}
        List<Map.Entry<LocalDate, Long>> entries = new ArrayList<>(agregado.entrySet());

        // Tamanho da janela de média móvel conforme agrupamento
        int janela = switch (agrupamento) {
            case "semana" -> 4;
            case "mes" -> 3;
            default -> 7; // dia
        };

        // Calcula média móvel para cada ponto
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

        // Calcula tendência: compara média da 1ª metade com a 2ª metade
        EstatisticasHistoricoDTO.Tendencia tendencia = calcularTendencia(entries);

        return new EstatisticasHistoricoDTO(pontos, tendencia, mediaPessoasDia);
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
            long totalRecursos = computadorRepo.count() + salaRepo.count();
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

        // Status de todas as reservas (sem filtro de sala/PC)
        EstatisticasReservasDTO status = getStatusReservas(inicio, fim, null, null);
        long totalReservas = status.total();
        double taxaNoShow = totalReservas > 0
                ? Math.round((status.atrasadas() * 100.0 / totalReservas) * 10.0) / 10.0
                : 0.0;

        // Todos os recursos para calcular ocupação média e recurso mais usado
        List<EstatisticasRecursoDTO> todosRecursos = new ArrayList<>();
        todosRecursos.addAll(getRecursosComputadores(inicio, fim, null));
        todosRecursos.addAll(getRecursosSalas(inicio, fim, null));

        // Taxa de ocupação média (só recursos com minutos disponíveis)
        double taxaOcupacao = todosRecursos.stream()
                .filter(r -> r.minutosDisponiveis() > 0)
                .mapToDouble(r -> (r.totalMinutosUsados() * 100.0) / r.minutosDisponiveis())
                .average()
                .orElse(0.0);
        taxaOcupacao = Math.round(taxaOcupacao * 10.0) / 10.0;

        // Recurso mais usado por minutos
        EstatisticasRecursoDTO maisUsado = todosRecursos.stream()
                .max(Comparator.comparingLong(EstatisticasRecursoDTO::totalMinutosUsados))
                .orElse(null);

        String nomeRecurso = maisUsado != null ? maisUsado.nome() : "-";

        // Determina tipo: se o nome bate com algum computador é PC, senão SALA
        boolean isPC = maisUsado != null && computadorRepo.existsById(maisUsado.id());
        String tipoRecurso = maisUsado == null ? "-" : (isPC ? "PC" : "SALA");

        return new EstatisticasResumoDTO(totalReservas, taxaOcupacao, taxaNoShow, nomeRecurso, tipoRecurso);
    }

}