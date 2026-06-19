package com.example.demo.controller;

import com.example.demo.annotations.Admin;
import com.example.demo.dtoEstatisticas.*;
import com.example.demo.service.EstatisticasService;
import com.example.demo.service.EstatisticasUsuariosService;
import com.example.demo.service.HeatmapService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/estatisticas")
public class EstatisticasController {

    // Constructor injection: imutável, testável, dependências visíveis na startup
    private final EstatisticasService estatisticasService;
    private final HeatmapService heatmapService;
    private final EstatisticasUsuariosService estatisticasUsuariosService;

    public EstatisticasController(EstatisticasService estatisticasService,
                                   HeatmapService heatmapService,
                                   EstatisticasUsuariosService estatisticasUsuariosService) {
        this.estatisticasService = estatisticasService;
        this.heatmapService = heatmapService;
        this.estatisticasUsuariosService = estatisticasUsuariosService;
    }

    // ─── Salas ────────────────────────────────────────────────────────────────

    @Admin
    @GetMapping("/salas/recursos")
    public List<EstatisticasRecursoDTO> getRecursosSalas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) List<Long> salaIds,
            @RequestParam(defaultValue = "30") int diasFuturo) {
        return estatisticasService.getRecursosSalas(inicio, fim, salaIds, diasFuturo);
    }

    // ─── Computadores ─────────────────────────────────────────────────────────

    @Admin
    @GetMapping("/computadores/recursos")
    public List<EstatisticasRecursoDTO> getRecursosComputadores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) List<Long> computadorIds,
            @RequestParam(defaultValue = "30") int diasFuturo) {
        return estatisticasService.getRecursosComputadores(inicio, fim, computadorIds, diasFuturo);
    }

    // ─── Status das Reservas ──────────────────────────────────────────────────

    @Admin
    @GetMapping("/status-reservas")
    public EstatisticasReservasDTO getStatusReservas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) List<Long> salaIds,
            @RequestParam(required = false) List<Long> computadorIds) {
        return estatisticasService.getStatusReservas(inicio, fim, salaIds, computadorIds);
    }

    // ─── Heatmap ──────────────────────────────────────────────────────────────

    @Admin
    @GetMapping("/heatmap")
    public List<EstatisticasHeatmapDTO> getHeatmap(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return heatmapService.getHeatmap(inicio, fim);
    }

    // ─── Usuários ─────────────────────────────────────────────────────────────

    @Admin
    @GetMapping("/usuarios/stats")
    public Map<String, Object> getUsuarioStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return estatisticasService.getUsuarioStats(inicio, fim);
    }

    @Admin
    @GetMapping("/usuarios/ranking")
    public List<EstatisticasUsuarioDTO> getRankingUsuarios() {
        return estatisticasService.getRankingUsuarios();
    }

    // ─── Ocupação por dia da semana ──────────────────────────────────────────────

    @Admin
    @GetMapping("/ocupacao-semana")
    public List<EstatisticasOcupacaoDiaDTO> getOcupacaoSemana(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return estatisticasService.getOcupacaoSemana(inicio, fim);
    }


    // ─── Resumo (cards do topo) ──────────────────────────────────────────────────

    @Admin
    @GetMapping("/resumo")
    public EstatisticasResumoDTO getResumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return estatisticasService.getResumo(inicio, fim);
    }

    // ─── Histórico Linear ─────────────────────────────────────────────────────

    @Admin
    @GetMapping("/historico")
    public EstatisticasHistoricoDTO getHistorico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(defaultValue = "dia") String agrupamento) {
        return estatisticasService.getHistorico(inicio, fim, agrupamento);
    }

    // ─── Estatísticas de Usuários ─────────────────────────────────────────────

    @Admin
    @GetMapping("/usuarios")
    public EstatisticasUsuariosDTO getEstatisticasUsuarios(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return estatisticasUsuariosService.getEstatisticasUsuarios(inicio, fim);
    }
}
