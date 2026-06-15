package com.example.demo.repository;

import com.example.demo.entity.PedidoReserva;
import com.example.demo.enums.StatusReserva;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoReservaRepository extends BaseRepository<PedidoReserva, Long> {

        @Query("""
                            SELECT p FROM PedidoReserva p
                            LEFT JOIN FETCH p.usuario
                            LEFT JOIN FETCH p.criadaPorUsuario
                            WHERE p.id = :id AND p.ativo = TRUE
                        """)
        Optional<PedidoReserva> findByIdComUsuario(Long id);

        @Query("""
                            SELECT DISTINCT p FROM PedidoReserva p
                            LEFT JOIN FETCH p.usuario
                            LEFT JOIN FETCH p.criadaPorUsuario
                            LEFT JOIN FETCH p.reservasComputador rc
                            LEFT JOIN FETCH rc.computador
                            WHERE p.usuario.id = :usuarioId
                              AND p.ativo = TRUE
                        """)
        List<PedidoReserva> findByUsuarioIdComComputadores(Long usuarioId);

        @Query("""
                            SELECT DISTINCT p FROM PedidoReserva p
                            LEFT JOIN FETCH p.usuario
                            LEFT JOIN FETCH p.criadaPorUsuario
                            LEFT JOIN FETCH p.reservasSala rs
                            LEFT JOIN FETCH rs.sala
                            WHERE p.usuario.id = :usuarioId
                              AND p.ativo = TRUE
                        """)
        List<PedidoReserva> findByUsuarioIdComSalas(Long usuarioId);

        @Query("""
                            SELECT DISTINCT p FROM PedidoReserva p
                            LEFT JOIN FETCH p.usuario
                            LEFT JOIN FETCH p.criadaPorUsuario
                            LEFT JOIN FETCH p.reservasComputador rc
                            LEFT JOIN FETCH rc.computador
                            WHERE p.ativo = TRUE
                        """)
        List<PedidoReserva> findTodosComComputadores();

        @Query("""
                            SELECT DISTINCT p FROM PedidoReserva p
                            LEFT JOIN FETCH p.usuario
                            LEFT JOIN FETCH p.criadaPorUsuario
                            LEFT JOIN FETCH p.reservasSala rs
                            LEFT JOIN FETCH rs.sala
                            WHERE p.ativo = TRUE
                        """)
        List<PedidoReserva> findTodosComSalas();

        // Adiciona isso no lugar:
        @Query("""
                            SELECT p FROM PedidoReserva p
                            LEFT JOIN FETCH p.usuario
                            LEFT JOIN FETCH p.criadaPorUsuario
                            LEFT JOIN FETCH p.reservasComputador rc
                            LEFT JOIN FETCH rc.computador
                            WHERE p.id = :id AND p.ativo = TRUE
                        """)
        Optional<PedidoReserva> findByIdComComputadores(Long id);

        @Query("""
                            SELECT p FROM PedidoReserva p
                            LEFT JOIN FETCH p.reservasSala rs
                            LEFT JOIN FETCH rs.sala
                            WHERE p.id = :id AND p.ativo = TRUE
                        """)
        Optional<PedidoReserva> findByIdComSalas(Long id);

        @Query("""
                            SELECT DISTINCT p FROM PedidoReserva p
                            LEFT JOIN FETCH p.usuario
                            LEFT JOIN FETCH p.criadaPorUsuario
                            LEFT JOIN FETCH p.reservasComputador rc
                            LEFT JOIN FETCH rc.computador
                            WHERE p.ativo = TRUE
                            AND (:status IS NULL OR p.status = :status)
                            AND (:dataInicio IS NULL OR p.inicioPrevisto >= :dataInicio)
                            AND (:dataFim IS NULL OR p.inicioPrevisto < :dataFim)
                            AND (:busca IS NULL OR
                                 LOWER(p.usuario.nome) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                                 LOWER(p.usuario.email) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                                 LOWER(rc.computador.codigo) LIKE LOWER(CONCAT('%', :busca, '%')))
                            ORDER BY p.inicioPrevisto DESC
                        """)
        List<PedidoReserva> findTodosFiltradoComComputadores(
                        @Param("status") StatusReserva status,
                        @Param("dataInicio") LocalDateTime dataInicio,
                        @Param("dataFim") LocalDateTime dataFim,
                        @Param("busca") String busca); 

        @Query("""
                            SELECT DISTINCT p FROM PedidoReserva p
                            LEFT JOIN FETCH p.usuario
                            LEFT JOIN FETCH p.criadaPorUsuario
                            LEFT JOIN FETCH p.reservasSala rs
                            LEFT JOIN FETCH rs.sala
                            WHERE p.ativo = TRUE
                            AND (:status IS NULL OR p.status = :status)
                            AND (:dataInicio IS NULL OR p.inicioPrevisto >= :dataInicio)
                            AND (:dataFim IS NULL OR p.inicioPrevisto < :dataFim)
                            AND (:busca IS NULL OR
                            LOWER(p.usuario.nome) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                            LOWER(p.usuario.email) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                            LOWER(rs.sala.nome) LIKE LOWER(CONCAT('%', :busca, '%')))
                            ORDER BY p.inicioPrevisto DESC
                        """)
        List<PedidoReserva> findTodosFiltradoComSalas(
                        @Param("status") StatusReserva status,
                        @Param("dataInicio") LocalDateTime dataInicio,
                        @Param("dataFim") LocalDateTime dataFim,
                        @Param("busca") String busca);

        // ─── Estatísticas ─────────────────────────────────────────────────────────

        @Query("""
                        SELECT p FROM PedidoReserva p
                        WHERE p.ativo = TRUE
                          AND p.status = 'FINALIZADA'
                          AND (:inicio IS NULL OR p.inicioPrevisto >= :inicio)
                          AND (:fim    IS NULL OR p.inicioPrevisto <= :fim)
                    """)
        List<PedidoReserva> findFinalizadasParaEstatisticas(
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fim") LocalDateTime fim);

        @Query("""
                        SELECT p FROM PedidoReserva p
                        WHERE p.ativo = TRUE
                          AND p.status = 'ATRASADO'
                          AND (:inicio IS NULL OR p.inicioPrevisto >= :inicio)
                          AND (:fim    IS NULL OR p.inicioPrevisto <= :fim)
                    """)
        List<PedidoReserva> findAtrasadasParaEstatisticas(
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fim") LocalDateTime fim);

        @Query("""
                        SELECT COUNT(p) FROM PedidoReserva p
                        WHERE p.ativo = TRUE
                          AND p.status = 'FINALIZADA'
                          AND (:inicio IS NULL OR p.inicioPrevisto >= :inicio)
                          AND (:fim    IS NULL OR p.inicioPrevisto <= :fim)
                    """)
        long countFinalizadasParaEstatisticas(
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fim") LocalDateTime fim);

}