package com.example.demo.scheduler;

import com.example.demo.service.ReservaComputadorService;
import com.example.demo.service.ReservaSalaService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservaScheduler {

    private final ReservaSalaService salaService;
    private final ReservaComputadorService computadorService;

    public ReservaScheduler(ReservaSalaService salaService, ReservaComputadorService computadorService) {
        this.salaService = salaService;
        this.computadorService = computadorService;
    }

    // Roda a cada 5 minutos — detecta reservas APROVADAS sem check-in após 15min
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void processarAtrasados() {
        salaService.processarAtrasado();
        computadorService.processarAtrasado();
    }

    // Roda a cada 5 minutos — finaliza reservas EM_ANDAMENTO que passaram do fim
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void processarAutoCheckout() {
        salaService.processarAutoCheckout();
        computadorService.processarAutoCheckout();
    }
}