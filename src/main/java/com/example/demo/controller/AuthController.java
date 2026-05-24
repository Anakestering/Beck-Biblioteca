package com.example.demo.controller;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.annotations.Public;
import com.example.demo.config.JwtUtil;
import com.example.demo.dto.AuthDTO;
import com.example.demo.dto.RecuperacaoSolicitacaoDTO;
import com.example.demo.dto.RecuperarSenhaDTO;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioService usuarioService;

    private final Map<String, long[]> tentativasLogin = new ConcurrentHashMap<>();

    private boolean podeLogin(String email) {
        long agora = System.currentTimeMillis();
        long janela = 15 * 60 * 1000L;
        int limite = 5;

        tentativasLogin.compute(email, (k, registro) -> {
            if (registro == null) return new long[]{ agora, 1 };
            if ((agora - registro[0]) >= janela) return new long[]{ agora, 1 };
            registro[1]++;
            return registro;
        });

        return tentativasLogin.get(email)[1] <= limite;
    }

    @PostMapping("/login")
    @Public
    public ResponseEntity<?> login(@RequestBody @Valid AuthDTO dto) {
        String email = dto.getEmail();
        String senha = dto.getSenha();

        if (!podeLogin(email)) {
            return ResponseEntity.status(429).body(
                Map.of("message", "Muitas tentativas. Tente novamente em 15 minutos."));
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isPresent() && passwordEncoder.matches(senha, usuarioOpt.get().getSenha())) {
            tentativasLogin.remove(email);

            String nivelAcesso = usuarioOpt.get().getNivelAcesso().toString();
            String token = jwtUtil.generateToken(email, nivelAcesso);

            return ResponseEntity.ok(Map.of("token", token, "tipo", nivelAcesso));
        }

        return ResponseEntity.status(401).body(
            Map.of("message", "Credenciais inválidas."));
    }

    @Public
    @PostMapping("/recuperar-senha/solicitar")
    public ResponseEntity<?> solicitarCodigo(
            @RequestBody @Valid RecuperacaoSolicitacaoDTO dto,
            HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null)
            ip = request.getRemoteAddr();

        usuarioService.solicitarCodigo(dto, ip);

        return ResponseEntity.ok(Map.of(
                "message", "Se existir uma conta com este email, um código de recuperação foi enviado.",
                "expiresAt", Instant.now().plusSeconds(1200).toString()));
    }

    @Public
    @PostMapping("/recuperar-senha/alterar")
    public ResponseEntity<?> alterarSenha(@RequestBody @Valid RecuperarSenhaDTO dto) {

        usuarioService.alterarSenha(dto);

        return ResponseEntity.ok(
                Map.of("message", "Senha alterada com sucesso!"));
    }
}