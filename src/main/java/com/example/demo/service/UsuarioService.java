package com.example.demo.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dto.RecuperacaoSolicitacaoDTO;
import com.example.demo.dto.RecuperarSenhaDTO;
import com.example.demo.dto.TrocarSenhaDTO;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.dto.UsuarioStatsDTO;
import com.example.demo.entity.Usuario;
import com.example.demo.enums.NivelAcesso;
import com.example.demo.repository.UsuarioRepository;

@Service
public class UsuarioService extends BaseService<Usuario, UsuarioDTO> {

    public UsuarioService(UsuarioRepository repository) {
        super(repository);
    }

    @Autowired
    private UsuarioRepository repo;

    @Autowired
    EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Cadastro de novo usuário
    public void cadastrar(Usuario usuario) {

        if (repo.existsByEmail(usuario.getEmail())) {
            throw new RuntimeException("Email já existe");
        }

        if (repo.existsByCpf(usuario.getCpf())) {
            throw new RuntimeException("CPF já existe");
        }

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuario.setAtivo(true);

        // nível padrão
        usuario.setNivelAcesso(NivelAcesso.PADRAO);

        repo.save(usuario);
    }

    @Override
    public List<UsuarioDTO> read() {
        return repo.findAllIncludingInactive()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public UsuarioDTO update(Long id, UsuarioDTO dto) {
        Usuario usuario = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Verifica se o e-mail foi alterado e já existe em outro usuário
        if (!usuario.getEmail().equals(dto.getEmail())
                && repo.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email já existe");
        }

        // Verifica se o CPF foi alterado e já existe em outro usuário
        if (!usuario.getCpf().equals(dto.getCpf())
                && repo.existsByCpf(dto.getCpf())) {
            throw new RuntimeException("CPF já existe");
        }

        // Atualiza apenas os campos permitidos
        usuario.setNome(dto.getNome());
        usuario.setCpf(dto.getCpf());
        usuario.setTelefone(dto.getTelefone());
        usuario.setEmail(dto.getEmail());

        Usuario salvo = repo.save(usuario);

        return toDto(salvo);
    }

    @Transactional
    public void ativar(Long id) {
        Usuario usuario = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        usuario.setAtivo(true);
        usuario.setDeletedAt(null);

        repo.save(usuario);
    }

    // usuarios criados na semana e o total
    public UsuarioStatsDTO buscarStats() {
        LocalDateTime inicioDaSemana = LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .atStartOfDay();

        long total = repo.count();
        long ativos = repo.countByAtivo(true);
        long semana = repo.countByCreatedAtGreaterThanEqual(inicioDaSemana);

        return new UsuarioStatsDTO(total, ativos, semana);
    }

    // UsuarioService
    public List<UsuarioDTO> buscar(String termo) {
        return repo.buscarPorTermo(termo)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // UsuarioService.java
    public UsuarioDTO buscarPorEmail(String email) {
        Usuario usuario = repo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        return toDto(usuario);
    }

    private final Map<String, long[]> tentativas = new ConcurrentHashMap<>();

    private boolean podeEnviar(String chave) {
        long agora = System.currentTimeMillis();
        long janela = 15 * 60 * 1000L;

        tentativas.compute(chave, (k, registro) -> {
            if (registro == null)
                return new long[] { agora, 1 };
            long tempoInicio = registro[0];
            long contagem = registro[1];

            if ((agora - tempoInicio) >= janela)
                return new long[] { agora, 1 };
            registro[1] = contagem + 1;
            return registro;
        });

        long[] registro = tentativas.get(chave);
        return registro[1] <= 5;
    }

    @Transactional
    public void solicitarCodigo(RecuperacaoSolicitacaoDTO dto, String ip) {

        String chave = ip + ":" + dto.getEmail();

        if (!podeEnviar(chave)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Tente novamente mais tarde.");
        }

        String email = dto.getEmail();

        Usuario usuario = repo.findByEmail(email).orElse(null);

        if (usuario == null) {
            return;
        }

        String codigo = String.valueOf(10000000 + new Random().nextInt(90000000));

        usuario.setCodigoRecuperacao(codigo);
        usuario.setCodigoRecuperacaoExpiracao(LocalDateTime.now().plusMinutes(20));

        repo.save(usuario);

        try {
            emailService.enviarEmail(
                    email,
                    "SOLICITAÇÃO DE RECUPERAÇÃO DE SENHA",
                    "SEU CÓDIGO É: " + codigo);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Poxa vida, deu ruim no email :(");
        }
    }

    @Transactional
    public void alterarSenha(RecuperarSenhaDTO dto) {
        String email = dto.getEmail();
        String codigo = dto.getCodigo();
        String novaSenha = dto.getNovaSenha();

        Usuario usuario = repo.findByEmail(email).orElse(null);

        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código incorreto.");
        }

        // verifica se existe o codigo
        if (usuario.getCodigoRecuperacao() == null || usuario.getCodigoRecuperacaoExpiracao() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhuma solicitação de recuperação foi feita.");
        }

        // verifica se o codigo enviado tem msm 8 caracteres, tirei do dto
        if (codigo.length() != 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código incorreto.");
        }

        // verifica se é igual
        if (!usuario.getCodigoRecuperacao().equals(codigo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código incorreto.");
        }

        // verifica se esta expirado
        if (usuario.getCodigoRecuperacaoExpiracao().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código incorreto.");
        }

        String novaSenhaCriptografada = passwordEncoder.encode(novaSenha);
        usuario.setSenha(novaSenhaCriptografada);

        usuario.setCodigoRecuperacao(null);
        usuario.setCodigoRecuperacaoExpiracao(null);

        repo.save(usuario);
    }

    public void trocarSenha(String email, TrocarSenhaDTO dto) {
        Usuario usuario = repo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        if (!passwordEncoder.matches(dto.getSenhaAtual(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta.");
        }

        usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        repo.save(usuario);
    }
}