package com.example.demo.service;

import com.example.demo.dtoEstatisticas.EstatisticasHeatmapDTO;
import com.example.demo.repository.ReservaComputadorRepository;
import com.example.demo.repository.ReservaSalaRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
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

    private final ReservaSalaRepository reservaSalaRepo;
    private final ReservaComputadorRepository reservaComputadorRepo;

    public HeatmapService(
            ReservaSalaRepository reservaSalaRepo,
            ReservaComputadorRepository reservaComputadorRepo) {
        this.reservaSalaRepo = reservaSalaRepo;
        this.reservaComputadorRepo = reservaComputadorRepo;
    }

    public List<EstatisticasHeatmapDTO> getHeatmap(LocalDateTime inicio, LocalDateTime fim) {

        // Fallback se vier nulo
        if (inicio == null)
            inicio = LocalDateTime.now().with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
        if (fim == null)
            fim = LocalDateTime.now();

        // ─── Mapa com array de 3 posições: [0]=1ª Metade, [1]=2ª Metade, [2]=Valor
        // para Cor ───
        Map<String, long[]> totais = new HashMap<>();

        List<Object[]> salaRows = reservaSalaRepo.findHeatmapParaEstatisticas(inicio, fim);
        List<Object[]> pcRows = reservaComputadorRepo.findHeatmapParaEstatisticas(inicio, fim);

        for (Object[] row : salaRows) {
            LocalDateTime checkin = toLocalDateTime(row[0]);
            LocalDateTime checkout = toLocalDateTime(row[1]);
            long pessoas = ((Number) row[2]).longValue();
            distribuirHoras(checkin, checkout, pessoas, totais);
        }

        for (Object[] row : pcRows) {
            LocalDateTime checkin = toLocalDateTime(row[0]);
            LocalDateTime checkout = toLocalDateTime(row[1]);
            long pessoas = ((Number) row[2]).longValue();
            distribuirHoras(checkin, checkout, pessoas, totais);
        }

        // ─── Conta quantas vezes cada dia da semana aparece no período ────────
        Map<Integer, Long> ocorrencias = new HashMap<>();
        for (int d = 1; d <= 5; d++)
            ocorrencias.put(d, 0L);

        LocalDate cursor = inicio.toLocalDate();
        LocalDate fimDate = fim.toLocalDate();
        while (!cursor.isAfter(fimDate)) {
            int diaSemana = cursor.getDayOfWeek().getValue(); // 1=seg...5=sex
            if (diaSemana >= 1 && diaSemana <= 5)
                ocorrencias.merge(diaSemana, 1L, Long::sum);
            cursor = cursor.plusDays(1);
        }

        // ─── Monta o DTO com as metades e a média da cor ─────────────────────
        List<EstatisticasHeatmapDTO> resultado = new ArrayList<>();

        for (Map.Entry<String, long[]> entry : totais.entrySet()) {
            String[] parts = entry.getKey().split(":");
            int dia = Integer.parseInt(parts[0]);
            int hora = Integer.parseInt(parts[1]);

            long[] dadosHora = entry.getValue();
            long primeiraMetade = dadosHora[0];
            long segundaMetade = dadosHora[1];
            long valorParaCor = dadosHora[2]; // Acumulado de quem ficou > 30 minutos

            long qtdDias = ocorrencias.getOrDefault(dia, 1L);
            if (qtdDias == 0)
                qtdDias = 1L; // evita divisão por zero

            // A média que dita a COR do bloco agora se baseia estritamente na regra de > 30
            // min
            long media = Math.round((double) valorParaCor / qtdDias);

            // ATENÇÃO: Adicione os novos campos no construtor do seu DTO
            resultado.add(new EstatisticasHeatmapDTO(dia, hora, primeiraMetade, segundaMetade, valorParaCor, media));
        }

        resultado.sort(Comparator.comparingInt(EstatisticasHeatmapDTO::diaSemana)
                .thenComparingInt(EstatisticasHeatmapDTO::hora));

        return resultado;
    }

    // ─── Converte Timestamp ou LocalDateTime para LocalDateTime ─────────────────
    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null)
            return null;
        if (value instanceof LocalDateTime)
            return (LocalDateTime) value;
        if (value instanceof Timestamp)
            return ((Timestamp) value).toLocalDateTime();
        return null;
    }

    // ─── Distribui pessoas aplicando as travas de 25min (metades) e 30min (cor)
    // ───
    private void distribuirHoras(LocalDateTime checkin, LocalDateTime checkout,
            long pessoas, Map<String, long[]> totais) {
        if (checkin == null || checkout == null || pessoas <= 0)
            return;

        LocalDateTime cursor = checkin.withMinute(0).withSecond(0).withNano(0);
        while (cursor.isBefore(checkout)) {
            int diaSemana = cursor.getDayOfWeek().getValue();
            if (diaSemana >= 1 && diaSemana <= 5) {
                int hora = cursor.getHour();
                if (hora < 7 || hora > 21) { 
                    cursor = cursor.plusHours(1);
                    continue;
                }
                String chave = diaSemana + ":" + hora;

                totais.putIfAbsent(chave, new long[3]);
                long[] dadosHora = totais.get(chave);

                LocalDateTime inicioDaHora = cursor;
                LocalDateTime meioDaHora = cursor.withMinute(30);
                LocalDateTime fimDaHora = cursor.plusHours(1);

                // 1) Lógica da Primeira Metade (xx:00 às xx:30)
                LocalDateTime in1 = checkin.isBefore(inicioDaHora) ? inicioDaHora : checkin;
                LocalDateTime out1 = checkout.isBefore(meioDaHora) ? checkout : meioDaHora;
                if (in1.isBefore(out1)) {
                    long min1 = java.time.Duration.between(in1, out1).toMinutes();
                    if (min1 >= 25) {
                        dadosHora[0] += pessoas;
                    }
                }

                // 2) Lógica da Segunda Metade (xx:30 às xx:00)
                LocalDateTime in2 = checkin.isBefore(meioDaHora) ? meioDaHora : checkin;
                LocalDateTime out2 = checkout.isBefore(fimDaHora) ? checkout : fimDaHora;
                if (in2.isBefore(out2)) {
                    long min2 = java.time.Duration.between(in2, out2).toMinutes();
                    if (min2 >= 25) {
                        dadosHora[1] += pessoas;
                    }
                }

                // 3) Lógica de Fluxo para a COR (xx:00 às xx:00 completo)
                LocalDateTime inCor = checkin.isBefore(inicioDaHora) ? inicioDaHora : checkin;
                LocalDateTime outCor = checkout.isBefore(fimDaHora) ? checkout : fimDaHora;
                if (inCor.isBefore(outCor)) {
                    long minCor = java.time.Duration.between(inCor, outCor).toMinutes();
                    if (minCor >= 20) { // Se ficou 20+ minutos na hora, conta para a cor
                        dadosHora[2] += pessoas;
                    }
                }
            }
            cursor = cursor.plusHours(1);
        }
    }
}