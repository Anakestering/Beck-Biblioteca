package com.example.demo.repository;

import com.example.demo.entity.PedidoReserva;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoReservaRepository extends BaseRepository<PedidoReserva, Long> {

  // Busca pedidos do usuário — faz fetch só do usuario e reservasComputador
  @Query("""
          SELECT DISTINCT p FROM PedidoReserva p
          LEFT JOIN FETCH p.usuario
          LEFT JOIN FETCH p.criadaPorUsuario
          LEFT JOIN FETCH p.reservasComputador rc
          LEFT JOIN FETCH rc.computador
          WHERE p.usuario.id = :usuarioId
            AND p.ativo = TRUE
          ORDER BY p.inicioPrevisto DESC
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

  // Busca todos — mesma separação
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
    WHERE p.id = :id AND p.ativo = TRUE
""")
Optional<PedidoReserva> findByIdComComputadores(Long id);

@Query("""
    SELECT DISTINCT p FROM PedidoReserva p
    LEFT JOIN FETCH p.usuario
    LEFT JOIN FETCH p.criadaPorUsuario
    LEFT JOIN FETCH p.reservasSala rs
    LEFT JOIN FETCH rs.sala
    WHERE p.id = :id AND p.ativo = TRUE
""")
Optional<PedidoReserva> findByIdComSalas(Long id);
}