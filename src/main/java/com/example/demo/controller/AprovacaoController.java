package com.example.demo.controller;

import com.example.demo.annotations.Admin;
import com.example.demo.entity.AprovacaoReserva;
import com.example.demo.service.AprovacaoReservaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/aprovacoes")
@CrossOrigin(origins = "http://localhost:3000")
@Admin
public class AprovacaoController {

    private final AprovacaoReservaService service;

    public AprovacaoController(AprovacaoReservaService service) {
        this.service = service;
    }

    @GetMapping("/pendentes")
    public ResponseEntity<List<AprovacaoReserva>> listarPendentes() {
        return ResponseEntity.ok(service.listarPendentes());
    }

    @PostMapping("/{id}/aprovar")
    public ResponseEntity<AprovacaoReserva> aprovar(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo
    ) {
        return ResponseEntity.ok(service.aprovar(id, motivo, getEmailLogado()));
    }

    @PostMapping("/{id}/rejeitar")
    public ResponseEntity<AprovacaoReserva> rejeitar(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo
    ) {
        return ResponseEntity.ok(service.rejeitar(id, motivo, getEmailLogado()));
    }

    private String getEmailLogado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}