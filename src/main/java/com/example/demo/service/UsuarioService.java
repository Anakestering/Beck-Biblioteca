package com.example.demo.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

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
import com.example.demo.dto.UsuarioOutroInfoDTO;
import com.example.demo.dto.UsuarioStatsDTO;
import com.example.demo.entity.Usuario;
import com.example.demo.entity.UsuarioOutroInfo;
import com.example.demo.enums.NivelAcesso;
import com.example.demo.enums.TipoUsuario;
import com.example.demo.repository.UsuarioOutroInfoRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
public class UsuarioService extends BaseService<Usuario, UsuarioDTO> {

    public UsuarioService(UsuarioRepository repository) {
        super(repository);
    }

    @Autowired
    private UsuarioRepository repo;

    @Autowired
    private UsuarioOutroInfoRepository outroInfoRepo;

    @Autowired
    EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ─── Cadastro ─────────────────────────────────────────────────────────────

    @Transactional
    public void cadastrar(UsuarioDTO dto) {
        if (repo.existsByEmail(dto.getEmail()))
            throw new RuntimeException("Email já existe");
        if (repo.existsByCpf(dto.getCpf()))
            throw new RuntimeException("CPF já existe");

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setCpf(dto.getCpf());
        usuario.setEmail(dto.getEmail());
        usuario.setTelefone(dto.getTelefone());
        usuario.setAtivo(true);
        usuario.setNivelAcesso(NivelAcesso.PADRAO);
        usuario.setTipoUsuario(dto.getTipoUsuario());

        String codigo = String.valueOf(10000000 + new Random().nextInt(90000000));
        usuario.setCodigoRecuperacao(codigo);
        usuario.setCodigoRecuperacaoExpiracao(LocalDateTime.now().plusMinutes(20));

        Usuario salvo = repo.save(usuario);

        if (dto.getTipoUsuario() == TipoUsuario.OUTRO && dto.getOutroInfo() != null) {
            salvarOutroInfo(salvo, dto.getOutroInfo());
        }

        enviarEmailCodigo(salvo, codigo, "Bem-vindo! Use o código abaixo para criar sua senha e acessar o sistema.");
    }

    // ─── Listagem ─────────────────────────────────────────────────────────────

    @Override
    public List<UsuarioDTO> read() {
        return repo.findAllIncludingInactive()
                .stream()
                .map(this::toDtoComOutroInfo)
                .toList();
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UsuarioDTO update(Long id, UsuarioDTO dto) {
        Usuario usuario = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!usuario.getEmail().equals(dto.getEmail()) && repo.existsByEmail(dto.getEmail()))
            throw new RuntimeException("Email já existe");
        if (!usuario.getCpf().equals(dto.getCpf()) && repo.existsByCpf(dto.getCpf()))
            throw new RuntimeException("CPF já existe");

        usuario.setNome(dto.getNome());
        usuario.setCpf(dto.getCpf());
        usuario.setTelefone(dto.getTelefone());
        usuario.setEmail(dto.getEmail());

        Usuario salvo = repo.save(usuario);

        return toDtoComOutroInfo(salvo);
    }

    // ─── Atualizar tipo ───────────────────────────────────────────────────────

    @Transactional
    public UsuarioDTO atualizarTipo(Long id, TipoUsuario novoTipo, UsuarioOutroInfoDTO outroInfo) {
        Usuario usuario = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        TipoUsuario tipoAnterior = usuario.getTipoUsuario();
        usuario.setTipoUsuario(novoTipo);

        Usuario salvo = repo.save(usuario);

        if (tipoAnterior == TipoUsuario.OUTRO && novoTipo != TipoUsuario.OUTRO) {
            outroInfoRepo.findByUsuarioIdAndAtivoTrue(id).ifPresent(info -> {
                info.setAtivo(false);
                info.setDeletedAt(LocalDateTime.now());
                outroInfoRepo.save(info);
            });
        }

        if (novoTipo == TipoUsuario.OUTRO && outroInfo != null) {
            UsuarioOutroInfo info = outroInfoRepo.findByUsuarioId(id)
                    .orElse(new UsuarioOutroInfo());
            info.setUsuario(salvo);
            info.setAtivo(true);
            info.setDeletedAt(null);
            preencherOutroInfo(info, outroInfo);
            outroInfoRepo.save(info);
        }

        return toDtoComOutroInfo(salvo);
    }

    // ─── Ativar ───────────────────────────────────────────────────────────────

    @Transactional
    public void ativar(Long id) {
        Usuario usuario = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        usuario.setAtivo(true);
        usuario.setDeletedAt(null);
        repo.save(usuario);
    }

    // ─── Busca ────────────────────────────────────────────────────────────────

    public List<UsuarioDTO> buscar(String termo) {
        return repo.buscarPorTermo(termo)
                .stream()
                .map(this::toDtoComOutroInfo)
                .toList();
    }

    public UsuarioDTO buscarPorEmail(String email) {
        Usuario usuario = repo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        return toDtoComOutroInfo(usuario);
    }

    // ─── Stats ────────────────────────────────────────────────────────────────

    public UsuarioStatsDTO buscarStats() {
        LocalDateTime inicioDaSemana = LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .atStartOfDay();

        long total = repo.count();
        long ativos = repo.countByAtivo(true);
        long semana = repo.countByCreatedAtGreaterThanEqual(inicioDaSemana);

        return new UsuarioStatsDTO(total, ativos, semana);
    }

    // ─── Senha ────────────────────────────────────────────────────────────────

    private final Map<String, long[]> tentativas = new ConcurrentHashMap<>();

    private boolean podeEnviar(String chave) {
        long agora = System.currentTimeMillis();
        long janela = 15 * 60 * 1000L;

        tentativas.compute(chave, (k, registro) -> {
            if (registro == null)
                return new long[] { agora, 1 };
            if ((agora - registro[0]) >= janela)
                return new long[] { agora, 1 };
            registro[1]++;
            return registro;
        });

        return tentativas.get(chave)[1] <= 5;
    }

    @Transactional
    public void solicitarCodigo(RecuperacaoSolicitacaoDTO dto, String ip) {
        if (!podeEnviar(ip + ":" + dto.getEmail()))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Tente novamente mais tarde.");

        Usuario usuario = repo.findByEmail(dto.getEmail()).orElse(null);
        if (usuario == null)
            return;

        String codigo = String.valueOf(10000000 + new Random().nextInt(90000000));
        usuario.setCodigoRecuperacao(codigo);
        usuario.setCodigoRecuperacaoExpiracao(LocalDateTime.now().plusMinutes(20));
        repo.save(usuario);

        enviarEmailCodigo(usuario, codigo, "Recebemos uma solicitação para redefinir sua senha. Use o código abaixo.");
    }

    @Transactional
    public void alterarSenha(RecuperarSenhaDTO dto) {
        Usuario usuario = repo.findByEmail(dto.getEmail()).orElse(null);
        if (usuario == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código incorreto.");
        if (usuario.getCodigoRecuperacao() == null || usuario.getCodigoRecuperacaoExpiracao() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhuma solicitação de recuperação foi feita.");
        if (dto.getCodigo().length() != 8)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código incorreto.");
        if (!usuario.getCodigoRecuperacao().equals(dto.getCodigo()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código incorreto.");
        if (usuario.getCodigoRecuperacaoExpiracao().isBefore(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código incorreto.");

        usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        usuario.setCodigoRecuperacao(null);
        usuario.setCodigoRecuperacaoExpiracao(null);
        repo.save(usuario);
    }

    public void trocarSenha(String email, TrocarSenhaDTO dto) {
        Usuario usuario = repo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (!passwordEncoder.matches(dto.getSenhaAtual(), usuario.getSenha()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta.");
        usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        repo.save(usuario);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private UsuarioDTO toDtoComOutroInfo(Usuario u) {
        UsuarioDTO dto = toDto(u);
        dto.setPendente(u.getSenha() == null);
        if (u.getTipoUsuario() == TipoUsuario.OUTRO) {
            outroInfoRepo.findByUsuarioIdAndAtivoTrue(u.getId()).ifPresent(info -> {
                dto.setOutroInfo(new UsuarioOutroInfoDTO(
                        info.getOndeConheceu(),
                        info.isTrabalha(),
                        info.getOndeTrabalha()));
            });
        }
        return dto;
    }

    private void salvarOutroInfo(Usuario usuario, UsuarioOutroInfoDTO infoDTO) {
        UsuarioOutroInfo info = new UsuarioOutroInfo();
        info.setUsuario(usuario);
        preencherOutroInfo(info, infoDTO);
        outroInfoRepo.save(info);
    }

    private void preencherOutroInfo(UsuarioOutroInfo info, UsuarioOutroInfoDTO dto) {
        info.setOndeConheceu(dto.getOndeConheceu() != null
                ? dto.getOndeConheceu().substring(0, Math.min(dto.getOndeConheceu().length(), 200))
                : null);
        info.setTrabalha(dto.isTrabalha());
        info.setOndeTrabalha(dto.isTrabalha() && dto.getOndeTrabalha() != null
                ? dto.getOndeTrabalha().substring(0, Math.min(dto.getOndeTrabalha().length(), 200))
                : null);
    }

    private void enviarEmailCodigo(Usuario usuario, String codigo, String mensagem) {
        String link = "http://localhost:3000/recuperar-senha?email=" + usuario.getEmail();
        emailService.enviarEmailComCodigo(usuario.getEmail(), usuario.getNome(), codigo, link, mensagem);
    }
}