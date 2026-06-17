package com.example.demo.controller;

import com.example.demo.annotations.Admin;
import com.example.demo.dto.SalaDTO;
import com.example.demo.service.SalaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/salas")
@CrossOrigin(origins = "http://localhost:3000")
public class SalaController {

    private final SalaService service;

    public SalaController(SalaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<SalaDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/todas")
    @Admin
    public ResponseEntity<List<SalaDTO>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    @PostMapping
    @Admin
    public ResponseEntity<SalaDTO> criar(@RequestBody @Valid SalaDTO dto) {
        return ResponseEntity.ok(service.criar(dto));
    }

    @PutMapping("/{id}")
    @Admin
    public ResponseEntity<SalaDTO> atualizar(@PathVariable Long id, @RequestBody @Valid SalaDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @PatchMapping("/{id}/ativar")
    @Admin
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        service.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desativar")
    @Admin
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        service.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Admin
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}