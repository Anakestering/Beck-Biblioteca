package com.example.demo.controller;

import com.example.demo.annotations.Admin;
import com.example.demo.dtoEstatisticas.*;
import com.example.demo.service.HeatmapService;
import com.example.demo.service.EstatisticasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/estatisticas")
public class EstatisticasController {

    @Autowired
    private EstatisticasService estatisticasService;

    @Autowired
    private HeatmapService heatmapService;

    // ─── Salas ────────────────────────────────────────────────────────────────

    @Admin
    @GetMapping("/salas/recursos")
    public List<EstatisticasRecursoDTO> getRecursosSalas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) List<Long> salaIds) {
        return estatisticasService.getRecursosSalas(inicio, fim, salaIds);
    }

    // ─── Computadores ─────────────────────────────────────────────────────────

    @Admin
    @GetMapping("/computadores/recursos")
    public List<EstatisticasRecursoDTO> getRecursosComputadores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) List<Long> computadorIds) {
        return estatisticasService.getRecursosComputadores(inicio, fim, computadorIds);
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

    // ─── Histórico Linear ─────────────────────────────────────────────────────

    @Admin
    @GetMapping("/historico")
    public List<EstatisticasLinearDTO> getHistorico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(defaultValue = "dia") String agrupamento) {
        return estatisticasService.getHistorico(inicio, fim, agrupamento);
    }
}
