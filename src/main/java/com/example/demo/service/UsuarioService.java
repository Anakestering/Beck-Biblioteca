package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.dto.UsuarioDTO;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;

@Service
public class UsuarioService extends BaseService<Usuario, UsuarioDTO> {

    public UsuarioService(UsuarioRepository repository) {
        super(repository);
    }

    @Autowired
    private UsuarioRepository repo;

    @Autowired
    private PasswordEncoder encoder;

    public void cadastrar(Usuario usuario) {

        if (repo.existsByEmail(usuario.getEmail())) {
            throw new RuntimeException("Email já existe");
        }

        if (repo.existsByCpf(usuario.getCpf())) {
            throw new RuntimeException("CPF já existe");
        }

        usuario.setSenha(
                encoder.encode(
                        usuario.getSenha()));

        if (repo.existsByUsuario(usuario.getUsuario())) {
            throw new RuntimeException(
                    "Usuário já existe");
        }

        repo.save(usuario);

    }

}
