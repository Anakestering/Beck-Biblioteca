package com.example.demo.service;

import com.example.demo.dtoRelatorio.RelatorioHeatmapDTO;
import com.example.demo.repository.ReservaComputadorRepository;
import com.example.demo.repository.ReservaSalaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HeatmapService {

    @Autowired
    private ReservaSalaRepository reservaSalaRepo;
    @Autowired
    private ReservaComputadorRepository reservaComputadorRepo;

    public List<RelatorioHeatmapDTO> getHeatmap(LocalDateTime inicio, LocalDateTime fim) {

        // Fallback se vier nulo
        if (inicio == null) inicio = LocalDateTime.now().with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
        if (fim == null) fim = LocalDateTime.now();

        // ─── Soma total de pessoas por dia:hora ───────────────────────────────
        Map<String, Long> totais = new HashMap<>();

        for (Object[] row : reservaSalaRepo.findHeatmapParaRelatorio(inicio, fim)) {
            LocalDateTime checkin  = (LocalDateTime) row[0];
            LocalDateTime checkout = (LocalDateTime) row[1];
            long pessoas = ((Number) row[2]).longValue();
            distribuirHoras(checkin, checkout, pessoas, totais);
        }

        for (Object[] row : reservaComputadorRepo.findHeatmapParaRelatorio(inicio, fim)) {
            LocalDateTime checkin  = (LocalDateTime) row[0];
            LocalDateTime checkout = (LocalDateTime) row[1];
            long pessoas = ((Number) row[2]).longValue();
            distribuirHoras(checkin, checkout, pessoas, totais);
        }

        // ─── Conta quantas vezes cada dia da semana aparece no período ────────
        // dia 1=seg, 2=ter, 3=qua, 4=qui, 5=sex
        Map<Integer, Long> ocorrencias = new HashMap<>();
        for (int d = 1; d <= 5; d++) ocorrencias.put(d, 0L);

        LocalDate cursor = inicio.toLocalDate();
        LocalDate fimDate = fim.toLocalDate();
        while (!cursor.isAfter(fimDate)) {
            int diaSemana = cursor.getDayOfWeek().getValue(); // 1=seg...5=sex
            if (diaSemana >= 1 && diaSemana <= 5)
                ocorrencias.merge(diaSemana, 1L, Long::sum);
            cursor = cursor.plusDays(1);
        }

        // ─── Monta o DTO com total e média ───────────────────────────────────
        List<RelatorioHeatmapDTO> resultado = new ArrayList<>();

        for (Map.Entry<String, Long> entry : totais.entrySet()) {
            String[] parts = entry.getKey().split(":");
            int dia  = Integer.parseInt(parts[0]);
            int hora = Integer.parseInt(parts[1]);
            long total = entry.getValue();

            long qtdDias = ocorrencias.getOrDefault(dia, 1L);
            if (qtdDias == 0) qtdDias = 1L; // evita divisão por zero
            long media = Math.round((double) total / qtdDias);

            resultado.add(new RelatorioHeatmapDTO(dia, hora, total, media));
        }

        resultado.sort(Comparator.comparingInt(RelatorioHeatmapDTO::diaSemana)
                .thenComparingInt(RelatorioHeatmapDTO::hora));

        return resultado;
    }

    // ─── Distribui pessoas por cada hora do intervalo checkin→checkout ────────
    private void distribuirHoras(LocalDateTime checkin, LocalDateTime checkout,
                                  long pessoas, Map<String, Long> totais) {
        if (checkin == null || checkout == null || pessoas <= 0) return;

        // itera hora a hora: ex. 14:05→16:50 passa pelas horas 14, 15, 16
        LocalDateTime cursor = checkin.withMinute(0).withSecond(0).withNano(0);
        while (cursor.isBefore(checkout)) {
            int diaSemana = cursor.getDayOfWeek().getValue(); // 1=seg...5=sex
            if (diaSemana >= 1 && diaSemana <= 5) {
                int hora = cursor.getHour();
                totais.merge(diaSemana + ":" + hora, pessoas, Long::sum);
            }
            cursor = cursor.plusHours(1);
        }
    }
}