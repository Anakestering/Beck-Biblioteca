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

        // ─── Estatísticas de Usuários ─────────────────────────────────────────────

        /**
         * Distribuição por tipo de usuário para um dado status de pedido.
         * Retorna: [tipoUsuario, count(distinct usuario), count(pedidos)]
         */
        @Query("""
                        SELECT u.tipoUsuario,
                               COUNT(DISTINCT u.id),
                               COUNT(p)
                        FROM PedidoReserva p
                        JOIN p.usuario u
                        WHERE p.ativo = TRUE
                          AND p.status = :status
                          AND u.nivelAcesso <> com.example.demo.enums.NivelAcesso.ADMIN
                          AND (:inicio IS NULL OR p.inicioPrevisto >= :inicio)
                          AND (:fim    IS NULL OR p.inicioPrevisto <= :fim)
                        GROUP BY u.tipoUsuario
                    """)
        List<Object[]> findDistribuicaoPorTipoEStatus(
                        @Param("status") com.example.demo.enums.StatusReserva status,
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fim") LocalDateTime fim);

        /**
         * Ranking de usuários com ao menos 1 pedido finalizado no período.
         * Retorna: [id, nome, tipoUsuario, finalizados, cancelados, abandono]
         */
        @Query("""
                        SELECT u.id, u.nome, u.tipoUsuario, u.cpf,
                               SUM(CASE WHEN p.status = 'FINALIZADA' THEN 1 ELSE 0 END),
                               SUM(CASE WHEN p.status = 'CANCELADA'  THEN 1 ELSE 0 END),
                               SUM(CASE WHEN p.status = 'ATRASADO'   THEN 1 ELSE 0 END)
                        FROM PedidoReserva p
                        JOIN p.usuario u
                        WHERE p.ativo = TRUE
                          AND u.nivelAcesso <> com.example.demo.enums.NivelAcesso.ADMIN
                          AND (:inicio IS NULL OR p.inicioPrevisto >= :inicio)
                          AND (:fim    IS NULL OR p.inicioPrevisto <= :fim)
                        GROUP BY u.id, u.nome, u.tipoUsuario, u.cpf
                        HAVING SUM(CASE WHEN p.status = 'FINALIZADA' THEN 1 ELSE 0 END) > 0
                        ORDER BY SUM(CASE WHEN p.status = 'FINALIZADA' THEN 1 ELSE 0 END) DESC
                    """)
        List<Object[]> findRankingUsuariosNoPeriodo(
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fim") LocalDateTime fim);

        /**
         * Usuários com pedidos no período mas 0 finalizados (não compareceram).
         * Retorna: [id, nome, tipoUsuario, cancelados, abandono]
         */
        @Query("""
                        SELECT u.id, u.nome, u.tipoUsuario, u.cpf,
                               SUM(CASE WHEN p.status = 'CANCELADA' THEN 1 ELSE 0 END),
                               SUM(CASE WHEN p.status = 'ATRASADO'  THEN 1 ELSE 0 END)
                        FROM PedidoReserva p
                        JOIN p.usuario u
                        WHERE p.ativo = TRUE
                          AND u.nivelAcesso <> com.example.demo.enums.NivelAcesso.ADMIN
                          AND (:inicio IS NULL OR p.inicioPrevisto >= :inicio)
                          AND (:fim    IS NULL OR p.inicioPrevisto <= :fim)
                        GROUP BY u.id, u.nome, u.tipoUsuario, u.cpf
                        HAVING SUM(CASE WHEN p.status = 'FINALIZADA' THEN 1 ELSE 0 END) = 0
                        ORDER BY (SUM(CASE WHEN p.status = 'CANCELADA' THEN 1 ELSE 0 END) +
                                  SUM(CASE WHEN p.status = 'ATRASADO'  THEN 1 ELSE 0 END)) DESC
                    """)
        List<Object[]> findNaoComparaceramNoPeriodo(
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fim") LocalDateTime fim);

        /**
         * Primeiro uso por mês — usuários cuja primeira finalizada ocorreu em cada mês do período.
         * Retorna: [mes (yyyy-MM), count]
         */
        @Query(value = """
                        SELECT DATE_FORMAT(primeira.min_data, '%Y-%m') AS mes,
                               COUNT(*) AS total
                        FROM (
                            SELECT p.usuario_id, MIN(p.inicio_previsto) AS min_data
                            FROM pedido_reserva p
                            JOIN usuario u ON u.id = p.usuario_id
                            WHERE p.ativo = TRUE
                              AND p.status = 'FINALIZADA'
                              AND u.nivel_acesso <> 'ADMIN'
                            GROUP BY p.usuario_id
                        ) primeira
                        WHERE (:inicio IS NULL OR primeira.min_data >= :inicio)
                          AND (:fim    IS NULL OR primeira.min_data <= :fim)
                        GROUP BY mes
                        ORDER BY mes ASC
                    """, nativeQuery = true)
        List<Object[]> findPrimeiroUsoPorMes(
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fim") LocalDateTime fim);
}
