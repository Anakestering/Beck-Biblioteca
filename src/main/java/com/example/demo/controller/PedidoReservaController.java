package com.example.demo.controller;

import com.example.demo.annotations.Admin;
import com.example.demo.dto.PedidoReservaDTO;
import com.example.demo.entity.PedidoReserva;
import com.example.demo.service.PedidoReservaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
@CrossOrigin(origins = "http://localhost:3000")
public class PedidoReservaController {

    private final PedidoReservaService service;

    public PedidoReservaController(PedidoReservaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PedidoReserva> criar(@RequestBody @Valid PedidoReservaDTO dto) {
        return ResponseEntity.ok(service.criar(dto, getEmailLogado()));
    }

    @GetMapping("/minhos")
    public ResponseEntity<List<PedidoReserva>> meusPedidos() {
        return ResponseEntity.ok(service.listarPorEmailUsuario(getEmailLogado()));
    }

    @GetMapping
    public ResponseEntity<List<PedidoReserva>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    private String getEmailLogado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping("/{id}/cancelar-admin")
@Admin
public ResponseEntity<PedidoReserva> cancelarComoAdmin(@PathVariable Long id) {
    return ResponseEntity.ok(service.cancelarComoAdmin(id));
}


}