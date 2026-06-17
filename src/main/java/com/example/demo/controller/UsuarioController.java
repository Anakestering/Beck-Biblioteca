package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import com.example.demo.annotations.Admin;
import com.example.demo.annotations.Public;
import com.example.demo.dto.AtualizarTipoDTO;
import com.example.demo.dto.TrocarSenhaDTO;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.dto.UsuarioStatsDTO;
import com.example.demo.service.UsuarioService;

import jakarta.validation.Valid;

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
    public ResponseEntity<?> cadastrar(@RequestBody @Valid UsuarioDTO dto) {
        usuarioService.cadastrar(dto);
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

    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> buscarPerfil(Authentication authentication) {
        return ResponseEntity.ok(usuarioService.buscarPorEmail(authentication.getName()));
    }

    @Override
    @PutMapping("/{id}")
    public UsuarioDTO update(
            @PathVariable Long id,
            @RequestBody @Valid UsuarioDTO dto) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UsuarioDTO solicitante = usuarioService.buscarPorEmail(authentication.getName());

        boolean ehAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!solicitante.getId().equals(id) && !ehAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para alterar este usuário.");
        }

        return usuarioService.update(id, dto);
    }

    @Admin
    @PatchMapping("/{id}/tipo")
    public ResponseEntity<UsuarioDTO> atualizarTipo(
            @PathVariable Long id,
            @RequestBody AtualizarTipoDTO body) {
        return ResponseEntity.ok(usuarioService.atualizarTipo(id, body.getTipoUsuario(), body.getOutroInfo()));
    }

    @PatchMapping("/me/senha")
    public ResponseEntity<?> trocarSenha(
            @RequestBody @Valid TrocarSenhaDTO dto,
            Authentication authentication) {

        usuarioService.trocarSenha(authentication.getName(), dto);
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso!"));
    }

}