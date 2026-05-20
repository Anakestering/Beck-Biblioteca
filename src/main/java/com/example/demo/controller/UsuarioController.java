package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;


import com.example.demo.annotations.Public;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.dto.UsuarioStatsDTO;
import com.example.demo.entity.Usuario;
import com.example.demo.service.UsuarioService;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = "http://localhost:3000")

public class UsuarioController extends BaseController<UsuarioDTO> {

    private final UsuarioService usuarioService;

    public UsuarioController(
            UsuarioService usuarioService) {
        super(usuarioService);
        this.usuarioService = usuarioService;
    }

    @PostMapping("/cadastro")
    @Public
    public ResponseEntity<?> cadastrar(
            @RequestBody Usuario usuario) {
        usuarioService.cadastrar(usuario);

        return ResponseEntity.ok("Cadastrado");
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        usuarioService.ativar(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<UsuarioStatsDTO> stats() {
        return ResponseEntity.ok(usuarioService.buscarStats());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<UsuarioDTO>> buscar(@RequestParam String termo) {
        return ResponseEntity.ok(usuarioService.buscar(termo));
    }

}