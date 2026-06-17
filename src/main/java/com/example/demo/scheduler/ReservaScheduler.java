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

    @Scheduled(fixedDelay = 60 * 1000)
    public void processarTudo() {
        salaService.processarAtrasado();
        computadorService.processarAtrasado();
        salaService.processarAutoCheckout();
        computadorService.processarAutoCheckout();
    }
}