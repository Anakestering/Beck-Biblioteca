package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
                SELECT u
                FROM Usuario u
                ORDER BY u.nome ASC
            """)
    List<Usuario> findAllIncludingInactive();

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.ativo = :ativo")
    long countByAtivo(@Param("ativo") boolean ativo);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.createdAt >= :data")
    long countByCreatedAtGreaterThanEqual(@Param("data") LocalDateTime data);

    @Query("""
                SELECT u FROM Usuario u
                WHERE u.nivelAcesso <> com.example.demo.enums.NivelAcesso.ADMIN
                AND (
                    LOWER(u.nome)  LIKE LOWER(CONCAT('%', :termo, '%')) OR
                    LOWER(u.email) LIKE LOWER(CONCAT('%', :termo, '%')) OR
                    u.cpf          LIKE CONCAT('%', :termo, '%')
                )
                ORDER BY u.nome ASC
            """)
    List<Usuario> buscarPorTermo(@Param("termo") String termo);

    // ─── Relatórios ─────────────────────────────────────────────────────────

    @Query("""
                SELECT u.id, u.nome, u.email,
                       (SELECT COUNT(rs) FROM ReservaSala rs WHERE rs.usuario.id = u.id AND rs.status = 'FINALIZADA') +
                       (SELECT COUNT(rc) FROM ReservaComputador rc WHERE rc.usuario.id = u.id AND rc.status = 'FINALIZADA') as total
                FROM Usuario u
                WHERE u.nivelAcesso <> com.example.demo.enums.NivelAcesso.ADMIN
                  AND u.ativo = TRUE
                GROUP BY u.id, u.nome, u.email
                ORDER BY total DESC
            """)
    List<Object[]> findRankingUsuarios();

    @Query("""
                SELECT COUNT(u) FROM Usuario u
                WHERE u.nivelAcesso <> com.example.demo.enums.NivelAcesso.ADMIN
                  AND u.ativo = TRUE
                  AND (:inicio IS NULL OR u.createdAt >= :inicio)
                  AND (:fim IS NULL OR u.createdAt <= :fim)
            """)
    long countNovosUsuarios(LocalDateTime inicio, LocalDateTime fim);

    @Query("""
                SELECT COUNT(DISTINCT r.usuario.id) FROM ReservaSala r
                WHERE r.status = 'FINALIZADA'
                  AND r.ativo = TRUE
                  AND (:inicio IS NULL OR r.checkinEm >= :inicio)
                  AND (:fim IS NULL OR r.checkoutEm <= :fim)
            """)
    long countUsuariosAtivos(LocalDateTime inicio, LocalDateTime fim);
}