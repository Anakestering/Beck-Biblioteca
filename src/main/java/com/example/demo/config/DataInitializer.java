package com.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.entity.Usuario;
import com.example.demo.enums.NivelAcesso;
import com.example.demo.enums.TipoUsuario;
import com.example.demo.repository.UsuarioRepository;

@Configuration
public class DataInitializer {

    private final PasswordEncoder passwordEncoder;

    public DataInitializer(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public CommandLineRunner initDatabase(UsuarioRepository repository) {
        return args -> {
            if (repository.count() <= 0) {
                Usuario usuario = new Usuario();

                usuario.setNome("Administrador");
                usuario.setCpf("11111111111");
                usuario.setEmail("admin@admin.com");
                usuario.setNivelAcesso(NivelAcesso.ADMIN);
                usuario.setTipoUsuario(TipoUsuario.COLABORADOR);
                usuario.setSenha(passwordEncoder.encode("111111"));

                repository.save(usuario);

                // cria usuário padrão
                Usuario padrao = new Usuario();
                padrao.setNome("UsuarioPadrao");
                padrao.setCpf("22222222222");
                padrao.setEmail("user@user.com");
                padrao.setNivelAcesso(NivelAcesso.PADRAO);
                padrao.setTipoUsuario(TipoUsuario.OUTRO);
                padrao.setSenha(passwordEncoder.encode("222222"));
                repository.save(padrao);

                System.out.println("Usuário ADMIN criado com sucesso: 111.111.111-11 / 111111");
            } else {
                System.out.println("Usuário ADMIN e PADRAO já existe no banco!");
            }

        };
    }

}
