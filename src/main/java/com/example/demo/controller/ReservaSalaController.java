package com.example.demo.controller;

import com.example.demo.annotations.Admin;
import com.example.demo.dto.ReservaSalaDTO;
import com.example.demo.entity.ReservaSala;
import com.example.demo.service.ReservaSalaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/reservas/sala")
@CrossOrigin(origins = "http://localhost:3000")
public class ReservaSalaController {

    private final ReservaSalaService service;

    public ReservaSalaController(ReservaSalaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReservaSala> criar(@RequestBody @Valid ReservaSalaDTO dto) {
        String email = getEmailLogado();
        return ResponseEntity.ok(service.criar(dto, email));
    }

    @GetMapping("/{salaId}/ocupados")
    public ResponseEntity<List<LocalDateTime>> horariosOcupados(
            @PathVariable Long salaId,
            @RequestParam String data) {
        return ResponseEntity.ok(service.horariosOcupados(salaId, LocalDateTime.parse(data + "T00:00:00")));
    }

    @PostMapping("/{id}/checkin")
    public ResponseEntity<ReservaSala> checkin(@PathVariable Long id) {
        return ResponseEntity.ok(service.checkin(id, getEmailLogado()));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<ReservaSala> checkout(@PathVariable Long id) {
        return ResponseEntity.ok(service.checkout(id, getEmailLogado()));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservaSala> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelar(id, getEmailLogado()));
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<ReservaSala>> minhasReservas() {
        return ResponseEntity.ok(service.listarPorEmailUsuario(getEmailLogado()));
    }

    @GetMapping
    @Admin
    public ResponseEntity<List<ReservaSala>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
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

    @PostMapping("/{id}/cancelar-admin")
    @Admin
    public ResponseEntity<ReservaSala> cancelarComoAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelarComoAdmin(id));
    }

    private String getEmailLogado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}