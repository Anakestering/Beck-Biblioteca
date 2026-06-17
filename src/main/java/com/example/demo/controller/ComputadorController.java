package com.example.demo.controller;

import com.example.demo.annotations.Admin;
import com.example.demo.dto.ComputadorDTO;
import com.example.demo.service.ComputadorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/computadores")
@CrossOrigin(origins = "http://localhost:3000")
public class ComputadorController {

    private final ComputadorService service;

    public ComputadorController(ComputadorService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ComputadorDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/todos")
    @Admin
    public ResponseEntity<List<ComputadorDTO>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComputadorDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    @PostMapping
    @Admin
    public ResponseEntity<ComputadorDTO> criar(@RequestBody @Valid ComputadorDTO dto) {
        return ResponseEntity.ok(service.criar(dto));
    }

    @PutMapping("/{id}")
    @Admin
    public ResponseEntity<ComputadorDTO> atualizar(@PathVariable Long id, @RequestBody @Valid ComputadorDTO dto) {
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