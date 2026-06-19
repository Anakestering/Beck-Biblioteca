package com.example.demo.service;

import com.example.demo.dtoEstatisticas.*;
import com.example.demo.repository.PedidoReservaRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import com.example.demo.enums.StatusReserva;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EstatisticasUsuariosService {

    private final PedidoReservaRepository pedidoRepo;
    private final UsuarioRepository usuarioRepo;

    public EstatisticasUsuariosService(PedidoReservaRepository pedidoRepo, UsuarioRepository usuarioRepo) {
        this.pedidoRepo = pedidoRepo;
        this.usuarioRepo = usuarioRepo;
    }

    public EstatisticasUsuariosDTO getEstatisticasUsuarios(LocalDateTime inicio, LocalDateTime fim) {

        // ─── Distribuição por tipo ─────────────────────────────────────────────
        List<Object[]> rowsFin   = pedidoRepo.findDistribuicaoPorTipoEStatus(StatusReserva.FINALIZADA, inicio, fim);
        List<Object[]> rowsAtr   = pedidoRepo.findDistribuicaoPorTipoEStatus(StatusReserva.ATRASADO, inicio, fim);
        List<Object[]> rowsCanc  = pedidoRepo.findDistribuicaoPorTipoEStatus(StatusReserva.CANCELADA, inicio, fim);

        Map<String, long[]> mapFin  = toMap(rowsFin);
        Map<String, long[]> mapAtr  = toMap(rowsAtr);
        Map<String, long[]> mapCanc = toMap(rowsCanc);

        Set<String> tipos = new LinkedHashSet<>();
        tipos.addAll(mapFin.keySet());
        tipos.addAll(mapAtr.keySet());
        tipos.addAll(mapCanc.keySet());

        List<DistribuicaoTipoDTO> distribuicao = new ArrayList<>();
        for (String tipo : tipos) {
            long[] fin  = mapFin.getOrDefault(tipo,  new long[]{0, 0});
            long[] atr  = mapAtr.getOrDefault(tipo,  new long[]{0, 0});
            long[] canc = mapCanc.getOrDefault(tipo, new long[]{0, 0});

            long usuariosFinalizados = fin[0];
            long pedidosFinalizados  = fin[1];
            double mediaVisitas = usuariosFinalizados > 0
                    ? (double) pedidosFinalizados / usuariosFinalizados
                    : 0.0;

            distribuicao.add(new DistribuicaoTipoDTO(
                    tipo,
                    usuariosFinalizados, pedidosFinalizados, Math.round(mediaVisitas * 10.0) / 10.0,
                    atr[0],  atr[1],
                    canc[0], canc[1]
            ));
        }

        // ─── Ranking de usuários (com ao menos 1 finalizada) ──────────────────
        List<Object[]> rankingRows = pedidoRepo.findRankingUsuariosNoPeriodo(inicio, fim);
        List<RankingUsuarioDTO> ranking = new ArrayList<>();
        for (Object[] r : rankingRows) {
            long finalizados = ((Number) r[4]).longValue();
            long cancelados  = ((Number) r[5]).longValue();
            long abandono    = ((Number) r[6]).longValue();
            double taxaAbandono = (finalizados + abandono) > 0
                    ? (double) abandono / (finalizados + abandono) * 100
                    : 0.0;
            ranking.add(new RankingUsuarioDTO(
                    ((Number) r[0]).longValue(),
                    (String) r[1],
                    r[2] != null ? r[2].toString() : null,
                    r[3] != null ? r[3].toString() : null,
                    finalizados, cancelados, abandono,
                    Math.round(taxaAbandono * 10.0) / 10.0
            ));
        }

        // ─── Não compareceram (pedidos mas 0 finalizadas) ─────────────────────
        List<Object[]> naoRows = pedidoRepo.findNaoComparaceramNoPeriodo(inicio, fim);
        List<RankingUsuarioDTO> naoCompareceram = new ArrayList<>();
        for (Object[] r : naoRows) {
            long cancelados = ((Number) r[4]).longValue();
            long abandono   = ((Number) r[5]).longValue();
            naoCompareceram.add(new RankingUsuarioDTO(
                    ((Number) r[0]).longValue(),
                    (String) r[1],
                    r[2] != null ? r[2].toString() : null,
                    r[3] != null ? r[3].toString() : null,
                    0, cancelados, abandono, 0.0
            ));
        }

        // ─── Crescimento por mês ───────────────────────────────────────────────
        List<Object[]> cadastroRows  = usuarioRepo.findNovosCadastrosPorMes(inicio, fim);
        List<Object[]> primeiroUsoRows = pedidoRepo.findPrimeiroUsoPorMes(inicio, fim);

        Map<String, Long> mapCadastros   = new LinkedHashMap<>();
        Map<String, Long> mapPrimeiroUso = new LinkedHashMap<>();
        for (Object[] r : cadastroRows)
            mapCadastros.put((String) r[0], ((Number) r[1]).longValue());
        for (Object[] r : primeiroUsoRows)
            mapPrimeiroUso.put((String) r[0], ((Number) r[1]).longValue());

        Set<String> meses = new TreeSet<>();
        meses.addAll(mapCadastros.keySet());
        meses.addAll(mapPrimeiroUso.keySet());

        List<CrescimentoMesDTO> crescimento = new ArrayList<>();
        for (String mes : meses) {
            crescimento.add(new CrescimentoMesDTO(
                    mes,
                    mapCadastros.getOrDefault(mes, 0L),
                    mapPrimeiroUso.getOrDefault(mes, 0L)
            ));
        }

        // ─── Totais ────────────────────────────────────────────────────────────
        // ─── Totais ────────────────────────────────────────────────────────────────
        long totalAtivos      = ranking.size();
        long totalCadastrados = usuarioRepo.countUsuariosCadastrados();

        // ─── Total e novos por tipo ────────────────────────────────────────────
        Map<String, Long> totalPorTipo = new LinkedHashMap<>();
        for (Object[] r : usuarioRepo.findTotalByTipoUsuario())
            totalPorTipo.put(r[0] != null ? r[0].toString() : "OUTRO", ((Number) r[1]).longValue());

        Map<String, Long> novosPorTipo = new LinkedHashMap<>();
        for (Object[] r : usuarioRepo.findNovosByTipoUsuario(inicio, fim))
            novosPorTipo.put(r[0] != null ? r[0].toString() : "OUTRO", ((Number) r[1]).longValue());

        Map<String, Long> ativosPorTipo = new LinkedHashMap<>();
        for (Object[] r : usuarioRepo.findAtivosByTipoUsuario())
            ativosPorTipo.put(r[0] != null ? r[0].toString() : "OUTRO", ((Number) r[1]).longValue());

        return new EstatisticasUsuariosDTO(distribuicao, ranking, naoCompareceram, crescimento,
                totalAtivos, totalCadastrados, totalPorTipo, novosPorTipo, ativosPorTipo);
    }

    /** Converte rows [tipo, usuarios, pedidos] para Map<tipo, [usuarios, pedidos]> */
    private Map<String, long[]> toMap(List<Object[]> rows) {
        Map<String, long[]> map = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String tipo = r[0] != null ? r[0].toString() : "OUTRO";
            long usuarios = ((Number) r[1]).longValue();
            long pedidos  = ((Number) r[2]).longValue();
            map.put(tipo, new long[]{usuarios, pedidos});
        }
        return map;
    }
}
