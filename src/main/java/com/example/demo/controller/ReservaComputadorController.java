package com.example.demo.controller;

import com.example.demo.annotations.Admin;
import com.example.demo.dto.ReservaComputadorDTO;
import com.example.demo.entity.ReservaComputador;
import com.example.demo.service.ReservaComputadorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/reservas/computador")
@CrossOrigin(origins = "http://localhost:3000")
public class ReservaComputadorController {

    private final ReservaComputadorService service;

    public ReservaComputadorController(ReservaComputadorService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReservaComputador> criar(@RequestBody @Valid ReservaComputadorDTO dto) {
        return ResponseEntity.ok(service.criar(dto, getEmailLogado()));
    }

    @GetMapping("/{computadorId}/ocupados")
    public ResponseEntity<List<LocalDateTime>> horariosOcupados(
            @PathVariable Long computadorId,
            @RequestParam String data) {
        return ResponseEntity.ok(service.horariosOcupados(computadorId, LocalDateTime.parse(data + "T00:00:00")));
    }

    @PostMapping("/{id}/checkin")
    public ResponseEntity<ReservaComputador> checkin(@PathVariable Long id) {
        return ResponseEntity.ok(service.checkin(id, getEmailLogado()));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<ReservaComputador> checkout(@PathVariable Long id) {
        return ResponseEntity.ok(service.checkout(id, getEmailLogado()));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservaComputador> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelar(id, getEmailLogado()));
    }

    @PostMapping("/admin/processar-atrasados")
    @Admin
    public ResponseEntity<Void> processarAtrasados() {
        service.processarAtrasado();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/processar-auto-checkout")
    @Admin
    public ResponseEntity<Void> processarAutoCheckout() {
        service.processarAutoCheckout();
        return ResponseEntity.noContent().build();
    }

    private String getEmailLogado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping("/{id}/cancelar-admin")
    @Admin
    public ResponseEntity<ReservaComputador> cancelarComoAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelarComoAdmin(id));
    }
}