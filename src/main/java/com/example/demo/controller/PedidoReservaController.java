package com.example.demo.controller;

import com.example.demo.annotations.Admin;
import com.example.demo.dto.PedidoReservaDTO;
import com.example.demo.dto.PedidoReservaResponseDTO;
import com.example.demo.enums.StatusReserva;
import com.example.demo.mapper.ResponseMapper;
import com.example.demo.service.PedidoReservaService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
@CrossOrigin(origins = "http://localhost:3000")
public class PedidoReservaController {

    private final PedidoReservaService service;
    private final ResponseMapper mapper;

    public PedidoReservaController(PedidoReservaService service, ResponseMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Transactional
    @PostMapping
    public ResponseEntity<PedidoReservaResponseDTO> criar(@RequestBody @Valid PedidoReservaDTO dto) {
        return ResponseEntity.ok(mapper.toPedidoDTO(service.criar(dto, getEmailLogado())));
    }

    @Transactional
    @GetMapping("/minhos")
    public ResponseEntity<List<PedidoReservaResponseDTO>> meusPedidos() {
        return ResponseEntity.ok(mapper.toPedidoList(service.listarPorEmailUsuario(getEmailLogado())));
    }

    @Transactional
    @GetMapping
    public ResponseEntity<List<PedidoReservaResponseDTO>> listarTodos() {
        return ResponseEntity.ok(mapper.toPedidoList(service.listarTodos()));
    }

    @Transactional
    @PostMapping("/{id}/cancelar-admin")
    @Admin
    public ResponseEntity<PedidoReservaResponseDTO> cancelarComoAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toPedidoDTO(service.cancelarComoAdmin(id)));
    }

    @Transactional
    @PostMapping("/{id}/checkin")
    public ResponseEntity<PedidoReservaResponseDTO> checkin(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toPedidoDTO(service.checkin(id, getEmailLogado())));
    }

    @Transactional
    @PostMapping("/{id}/checkout")
    public ResponseEntity<PedidoReservaResponseDTO> checkout(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toPedidoDTO(service.checkout(id, getEmailLogado())));
    }

    @Transactional
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<PedidoReservaResponseDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toPedidoDTO(service.cancelar(id, getEmailLogado())));
    }

    private String getEmailLogado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

   @GetMapping("/filtrar")
    @Admin
    @Transactional
    public ResponseEntity<List<PedidoReservaResponseDTO>> listarFiltrado(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) StatusReserva status,
            @RequestParam(required = false) String busca) {
        return ResponseEntity.ok(mapper.toPedidoList(service.listarTodosFiltrado(data, status, busca)));
    }

    @GetMapping("/filtrar/periodo")
    @Admin
    @Transactional
    public ResponseEntity<List<PedidoReservaResponseDTO>> listarFiltradoPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) StatusReserva status,
            @RequestParam(required = false) String busca) {

        // CORREÇÃO: Removida a vírgula órfã e adicionado o parâmetro 'busca'
        return ResponseEntity.ok(mapper.toPedidoList(service.listarTodosFiltradoPeriodo(dataInicio, dataFim, status, busca)));
    }
}