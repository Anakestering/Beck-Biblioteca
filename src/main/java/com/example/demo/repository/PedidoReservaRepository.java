package com.example.demo.repository;

import com.example.demo.entity.PedidoReserva;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoReservaRepository extends BaseRepository<PedidoReserva, Long> {

    @Query("""
        SELECT p FROM PedidoReserva p
        LEFT JOIN FETCH p.usuario
        LEFT JOIN FETCH p.criadaPorUsuario
        LEFT JOIN FETCH p.reservasComputador rc
        LEFT JOIN FETCH rc.computador
        LEFT JOIN FETCH p.reservasSala rs
        LEFT JOIN FETCH rs.sala
        WHERE p.usuario.id = :usuarioId
          AND p.ativo = TRUE
        ORDER BY p.inicioPrevisto DESC
    """)
    List<PedidoReserva> findByUsuarioId(Long usuarioId);

    @Query(value = """
    SELECT DISTINCT p.* FROM pedido_reserva p
    LEFT JOIN usuario u ON p.usuario_id = u.id
    LEFT JOIN usuario cp ON p.criada_por_usuario_id = cp.id
    WHERE p.ativo = TRUE
    ORDER BY
      FIELD(p.status, 'EM_ANDAMENTO', 'APROVADA', 'PENDENTE_APROVACAO',
            'FINALIZADA', 'ATRASADO', 'REJEITADA', 'CANCELADA') ASC,
      p.inicio_previsto ASC
""", nativeQuery = true)
List<PedidoReserva> findTodos();
}