package com.example.demo.service;

import com.example.demo.dtoEstatisticas.*;
import com.example.demo.entity.Computador;
import com.example.demo.entity.PedidoReserva;
import com.example.demo.entity.ReservaComputador;
import com.example.demo.entity.ReservaSala;
import com.example.demo.entity.Sala;
import com.example.demo.repository.ComputadorRepository;
import com.example.demo.repository.PedidoReservaRepository;
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
    private final PedidoReservaRepository pedidoRepo;
    private final UsuarioRepository usuarioRepo;
    private final SalaRepository salaRepo;
    private final ComputadorRepository computadorRepo;

    public EstatisticasService(
            ReservaSalaRepository reservaSalaRepo,
            ReservaComputadorRepository reservaComputadorRepo,
            PedidoReservaRepository pedidoRepo,
            UsuarioRepository usuarioRepo,
            SalaRepository salaRepo,
            ComputadorRepository computadorRepo) {
        this.reservaSalaRepo = reservaSalaRepo;
        this.reservaComputadorRepo = reservaComputadorRepo;
        this.pedidoRepo = pedidoRepo;
        this.usuarioRepo = usuarioRepo;
        this.salaRepo = salaRepo;
        this.computadorRepo = computadorRepo;
    }

    // ─── Recursos (Salas e PCs) ───────────────────────────────────────────────

    public List<EstatisticasRecursoDTO> getRecursosSalas(LocalDateTime inicio, LocalDateTime fim, List<Long> salaIds, int diasFuturo) {
        List<Sala> salas = salaIds != null && !salaIds.isEmpty()
                ? salaRepo.findAllById(salaIds)
                : salaRepo.findAll();

        // Acumula minutos e contagem de reservas finalizadas por sala
        Map<Long, Long> minutos  = new HashMap<>();
        Map<Long, Long> contagem = new HashMap<>();
        for (ReservaSala r : reservaSalaRepo.findFinalizadasParaEstatisticas(inicio, fim, salaIds)) {
            acumularMinutos(minutos, r.getSala().getId(), r.getCheckinEm(), r.getCheckoutEm());
            contagem.merge(r.getSala().getId(), 1L, Long::sum);
        }

        // Minutos futuros: reservas APROVADA/PENDENTE nos próximos X dias
        Map<Long, Long> minutosFuturos = acumularMinutosDasRows(
                reservaSalaRepo.findMinutosFuturosPorSala(
                        LocalDateTime.now(), LocalDateTime.now().plusDays(diasFuturo), salaIds));

        long disponiveis = calcularMinutosDisponiveis(inicio, fim);

        return salas.stream()
                .map(s -> new EstatisticasRecursoDTO(
                        s.getId(),
                        s.getNome(),
                        minutos.getOrDefault(s.getId(), 0L),
                        contagem.getOrDefault(s.getId(), 0L),
                        disponiveis,
                        minutosFuturos.getOrDefault(s.getId(), 0L)))
                .sorted(Comparator.comparing(EstatisticasRecursoDTO::nome))
                .toList();
    }

    public List<EstatisticasRecursoDTO> getRecursosComputadores(LocalDateTime inicio, LocalDateTime fim,
            List<Long> computadorIds, int diasFuturo) {
        List<Computador> computadores = computadorIds != null && !computadorIds.isEmpty()
                ? computadorRepo.findAllById(computadorIds)
                : computadorRepo.findAll();

        // Acumula minutos e contagem de reservas finalizadas por computador
        Map<Long, Long> minutos  = new HashMap<>();
        Map<Long, Long> contagem = new HashMap<>();
        for (ReservaComputador r : reservaComputadorRepo.findFinalizadasParaEstatisticas(inicio, fim, computadorIds)) {
            acumularMinutos(minutos, r.getComputador().getId(), r.getCheckinEm(), r.getCheckoutEm());
            contagem.merge(r.getComputador().getId(), 1L, Long::sum);
        }

        // Minutos futuros: reservas APROVADA/PENDENTE nos próximos X dias
        Map<Long, Long> minutosFuturos = acumularMinutosDasRows(
                reservaComputadorRepo.findMinutosFuturosPorComputador(
                        LocalDateTime.now(), LocalDateTime.now().plusDays(diasFuturo), computadorIds));

        long disponiveis = calcularMinutosDisponiveis(inicio, fim);

        return computadores.stream()
                .map(c -> new EstatisticasRecursoDTO(
                        c.getId(),
                        c.getCodigo(),
                        minutos.getOrDefault(c.getId(), 0L),
                        contagem.getOrDefault(c.getId(), 0L),
                        disponiveis,
                        minutosFuturos.getOrDefault(c.getId(), 0L)))
                .sorted(Comparator.comparing(EstatisticasRecursoDTO::nome))
                .toList();
    }

    /**
     * Adiciona a duração de um período (checkin → checkout) ao mapa acumulador
     * indexado por recurso ID. Ignora registros com datas nulas.
     */
    private void acumularMinutos(Map<Long, Long> mapa, Long id, LocalDateTime checkin, LocalDateTime checkout) {
        if (checkin == null || checkout == null) return;
        mapa.merge(id, Duration.between(checkin, checkout).toMinutes(), Long::sum);
    }

    /**
     * Converte o resultado de queries de minutos futuros (Object[] → [id, soma])
     * em mapa id → totalMinutos. Padrão das queries findMinutosFuturosPor*.
     */
    private Map<Long, Long> acumularMinutosDasRows(List<Object[]> rows) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] row : rows) {
            mapa.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return mapa;
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

        // ─── Pedidos (para taxa de abandono e média de pessoas) ─────────────
        List<PedidoReserva> pedidosFinalizados = pedidoRepo.findFinalizadasParaEstatisticas(inicio, fim);
        List<PedidoReserva> pedidosAtrasados   = pedidoRepo.findAtrasadasParaEstatisticas(inicio, fim);

        // ─── Reservas individuais — linha principal do gráfico (só inicioPrevisto) ──
        List<LocalDateTime> iniciosSala = reservaSalaRepo.findIniciosFinalizados(inicio, fim);
        List<LocalDateTime> iniciosPc   = reservaComputadorRepo.findIniciosFinalizados(inicio, fim);

        // ─── Agrega reservas individuais por data (linha principal) ──────────
        TreeMap<LocalDate, Long> agregadoPedidos = new TreeMap<>();
        for (LocalDateTime dt : iniciosSala)
            agregadoPedidos.merge(chaveData(dt, agrupamento), 1L, Long::sum);
        for (LocalDateTime dt : iniciosPc)
            agregadoPedidos.merge(chaveData(dt, agrupamento), 1L, Long::sum);

        // ─── Agrega pedidos por data (para tooltip — representa visitas) ──────
        TreeMap<LocalDate, Long> agregadoReservas = new TreeMap<>();
        for (PedidoReserva p : pedidosFinalizados)
            agregadoReservas.merge(chaveData(p.getInicioPrevisto(), agrupamento), 1L, Long::sum);

        // ─── Agregação de abandonos por data (pedidos ATRASADO) ──────────────
        TreeMap<LocalDate, Long> agregadoAbandono = new TreeMap<>();
        for (PedidoReserva p : pedidosAtrasados)
            agregadoAbandono.merge(chaveData(p.getInicioPrevisto(), agrupamento), 1L, Long::sum);

        // ─── Média de pessoas por dia útil ────────────────────────────────────
        long totalPessoas = pedidosFinalizados.stream().mapToLong(PedidoReserva::getQtdePessoas).sum();
        long diasUteis = calcularDiasUteis(inicio, fim);
        double mediaPessoasDia = diasUteis > 0
                ? Math.round((totalPessoas * 10.0) / diasUteis) / 10.0
                : 0.0;

        // ─── Taxa de abandono global no período ───────────────────────────────
        long totalFinalizados = pedidosFinalizados.size();
        long totalAtrasados   = pedidosAtrasados.size();
        double taxaAbandono = (totalFinalizados + totalAtrasados) > 0
                ? Math.round((totalAtrasados * 1000.0) / (totalFinalizados + totalAtrasados)) / 10.0
                : 0.0;

        // ─── Janela da média móvel ────────────────────────────────────────────
        int janela = switch (agrupamento) {
            case "semana" -> 4;
            case "mes"    -> 3;
            default       -> 7;
        };

        // ─── Pontos de pedidos finalizados com MM e totalReservas ─────────────
        List<Map.Entry<LocalDate, Long>> entries = new ArrayList<>(agregadoPedidos.entrySet());
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
            long totalReservas = agregadoReservas.getOrDefault(e.getKey(), 0L);
            pontos.add(new EstatisticasHistoricoDTO.Ponto(e.getKey().toString(), e.getValue(), mm, totalReservas));
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
        EstatisticasHistoricoDTO.Tendencia tendencia         = calcularTendencia(entries);
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
        List<Object[]> rowsSala = reservaSalaRepo.findCheckinCheckoutFinalizados(inicio, fim);
        List<Object[]> rowsPc   = reservaComputadorRepo.findCheckinCheckoutFinalizados(inicio, fim);

        // Minutos usados agrupados por dia da semana (1=Seg ... 5=Sex)
        Map<Integer, Long> minutosUsadosPorDia = new HashMap<>();
        for (int i = 1; i <= 5; i++)
            minutosUsadosPorDia.put(i, 0L);

        for (Object[] row : rowsSala) {
            LocalDateTime checkin  = (LocalDateTime) row[0];
            LocalDateTime checkout = (LocalDateTime) row[1];
            int dow = checkin.getDayOfWeek().getValue();
            if (dow > 5) continue;
            minutosUsadosPorDia.merge(dow, Duration.between(checkin, checkout).toMinutes(), Long::sum);
        }
        for (Object[] row : rowsPc) {
            LocalDateTime checkin  = (LocalDateTime) row[0];
            LocalDateTime checkout = (LocalDateTime) row[1];
            int dow = checkin.getDayOfWeek().getValue();
            if (dow > 5) continue;
            minutosUsadosPorDia.merge(dow, Duration.between(checkin, checkout).toMinutes(), Long::sum);
        }

        // Quantas vezes cada dia da semana aparece no período (ex: 52 segundas num ano)
        Map<Integer, Long> ocorrenciasPorDia = new HashMap<>();
        for (int i = 1; i <= 5; i++)
            ocorrenciasPorDia.put(i, 0L);

        // Se inicio/fim forem null (filtro "Desde o início"), derivar do dado real
        LocalDateTime inicioEfetivo = inicio;
        LocalDateTime fimEfetivo    = fim;
        if (inicioEfetivo == null) {
            inicioEfetivo = java.util.stream.Stream.concat(
                    rowsSala.stream().map(r -> (LocalDateTime) r[0]),
                    rowsPc.stream().map(r -> (LocalDateTime) r[0])
            ).min(Comparator.naturalOrder()).orElse(LocalDateTime.now().minusYears(5));
        }
        if (fimEfetivo == null) fimEfetivo = LocalDateTime.now();

        LocalDate cursor  = inicioEfetivo.toLocalDate();
        LocalDate fimDate = fimEfetivo.toLocalDate();
        while (!cursor.isAfter(fimDate)) {
            int dow = cursor.getDayOfWeek().getValue();
            if (dow <= 5)
                ocorrenciasPorDia.merge(dow, 1L, Long::sum);
            cursor = cursor.plusDays(1);
        }

        // Taxa = minutos usados / (ocorrencias * 900min disponíveis por dia)
        final long MINUTOS_DIA = 15 * 60L; // 7h–22h = 900min
        String[] nomes = { "", "Seg", "Ter", "Qua", "Qui", "Sex" };
        long totalRecursos = computadorRepo.countByAtivoTrue() + salaRepo.countByAtivoTrue();

        List<EstatisticasOcupacaoDiaDTO> resultado = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
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

        // Minutos usados por recurso — via queries de agregação SQL (sem carregar entidades)
        Map<Long, Long> minutosSalaMap = acumularMinutosDasRows(
                reservaSalaRepo.findMinutosFinalizadosPorSala(inicio, fim));
        Map<Long, Long> minutosPcMap = acumularMinutosDasRows(
                reservaComputadorRepo.findMinutosFinalizadosPorComputador(inicio, fim));

        long disponiveis = calcularMinutosDisponiveis(inicio, fim);

        List<Sala> todasSalas = salaRepo.findAll();
        List<Computador> todosComputadores = computadorRepo.findAll();

        double somaOcupacao = 0;
        int totalComDisponiveis = 0;
        String nomeRecurso = "-";
        String tipoRecurso = "-";
        long maxMinutos = -1;

        for (Sala s : todasSalas) {
            long usados = minutosSalaMap.getOrDefault(s.getId(), 0L);
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
            long usados = minutosPcMap.getOrDefault(c.getId(), 0L);
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

        long totalPedidos = pedidoRepo.countFinalizadasParaEstatisticas(inicio, fim);

        return new EstatisticasResumoDTO(totalPedidos, finalizadas, taxaOcupacao, taxaNoShow, nomeRecurso, tipoRecurso);
    }

}
