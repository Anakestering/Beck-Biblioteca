package com.example.demo.controller;

import com.example.demo.annotations.Admin;
import com.example.demo.dto.ReservaComputadorDTO;
import com.example.demo.entity.ReservaComputador;
import com.example.demo.service.ReservaComputadorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ReservaComputador> criar(
            @RequestBody @Valid ReservaComputadorDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ReservaComputador reserva = service.criar(dto, userDetails.getUsername());
        return ResponseEntity.ok(reserva);
    }

    @PostMapping("/{id}/checkin")
    public ResponseEntity<ReservaComputador> checkin(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(service.checkin(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<ReservaComputador> checkout(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(service.checkout(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservaComputador> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(service.cancelar(id, userDetails.getUsername()));
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<ReservaComputador>> minhasReservas(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(service.listarPorEmailUsuario(userDetails.getUsername()));
    }

    // ─── Endpoints exclusivos de Admin ───────────────────────────────────────

    @GetMapping
    @Admin
    public ResponseEntity<List<ReservaComputador>> listarTodas() {
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