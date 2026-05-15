package com.example.demo.controller;

import com.example.demo.annotations.Admin;
import com.example.demo.dto.ReservaSalaDTO;
import com.example.demo.entity.ReservaSala;
import com.example.demo.service.ReservaSalaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ReservaSala> criar(
            @RequestBody @Valid ReservaSalaDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ReservaSala reserva = service.criar(dto, userDetails.getUsername());
        return ResponseEntity.ok(reserva);
    }

    @PostMapping("/{id}/checkin")
    public ResponseEntity<ReservaSala> checkin(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(service.checkin(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<ReservaSala> checkout(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(service.checkout(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservaSala> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(service.cancelar(id, userDetails.getUsername()));
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<ReservaSala>> minhasReservas(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Busca o ID do usuário logado via email — o service já tem esse método
        return ResponseEntity.ok(service.listarPorEmailUsuario(userDetails.getUsername()));
    }

    // ─── Endpoints exclusivos de Admin ───────────────────────────────────────

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
}