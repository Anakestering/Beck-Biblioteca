package com.example.demo.controller;

import com.example.demo.annotations.Admin;
import com.example.demo.dto.AprovacaoReservaResponseDTO;
import com.example.demo.mapper.ResponseMapper;
import com.example.demo.service.AprovacaoReservaService;
import jakarta.transaction.Transactional;
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
    private final ResponseMapper mapper;

    public AprovacaoController(AprovacaoReservaService service, ResponseMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Transactional
    @GetMapping("/pendentes")
    public ResponseEntity<List<AprovacaoReservaResponseDTO>> listarPendentes() {
        return ResponseEntity.ok(mapper.toAprovacaoList(service.listarPendentes()));
    }

    @Transactional
    @PostMapping("/{id}/aprovar")
    public ResponseEntity<AprovacaoReservaResponseDTO> aprovar(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo
    ) {
        return ResponseEntity.ok(mapper.toAprovacaoDTO(service.aprovar(id, motivo, getEmailLogado())));
    }

    @Transactional
    @PostMapping("/{id}/rejeitar")
    public ResponseEntity<AprovacaoReservaResponseDTO> rejeitar(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo
    ) {
        return ResponseEntity.ok(mapper.toAprovacaoDTO(service.rejeitar(id, motivo, getEmailLogado())));
    }

    private String getEmailLogado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}