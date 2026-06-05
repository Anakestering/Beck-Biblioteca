package com.example.demo.controller;

import com.example.demo.annotations.Admin;
import com.example.demo.dtoRelatorio.*;
import com.example.demo.service.HeatmapService;
import com.example.demo.service.RelatorioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/relatorios")
public class RelatorioController {

    @Autowired
    private RelatorioService relatorioService;
    
    @Autowired
    private HeatmapService heatmapService;

    // ─── Salas ────────────────────────────────────────────────────────────────

    @Admin
    @GetMapping("/salas/recursos")
    public List<RelatorioRecursoDTO> getRecursosSalas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) List<Long> salaIds) {
        return relatorioService.getRecursosSalas(inicio, fim, salaIds);
    }

    // ─── Computadores ─────────────────────────────────────────────────────────

    @Admin
    @GetMapping("/computadores/recursos")
    public List<RelatorioRecursoDTO> getRecursosComputadores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) List<Long> computadorIds) {
        return relatorioService.getRecursosComputadores(inicio, fim, computadorIds);
    }

    // ─── Status das Reservas ──────────────────────────────────────────────────

    @Admin
    @GetMapping("/status-reservas")
    public RelatorioStatusReservasDTO getStatusReservas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) List<Long> salaIds,
            @RequestParam(required = false) List<Long> computadorIds) {
        return relatorioService.getStatusReservas(inicio, fim, salaIds, computadorIds);
    }

    // ─── Heatmap ──────────────────────────────────────────────────────────────

    @Admin
    @GetMapping("/heatmap")
    public List<RelatorioHeatmapDTO> getHeatmap(
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
        return relatorioService.getUsuarioStats(inicio, fim);
    }

    @Admin
    @GetMapping("/usuarios/ranking")
    public List<RelatorioUsuarioDTO> getRankingUsuarios() {
        return relatorioService.getRankingUsuarios();
    }
}