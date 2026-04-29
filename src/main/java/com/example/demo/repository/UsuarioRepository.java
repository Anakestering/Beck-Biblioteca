package com.example.demo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.demo.entity.Usuario;

@Repository
public interface UsuarioRepository extends BaseRepository<Usuario, Long> {

    @Query("""
                    SELECT u FROM Usuario u
                    WHERE u.email = :email
                    AND u.ativo = TRUE
            """)
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

}
