package com.example.demo.repository;

import com.example.demo.entity.ReservaComputador;
import com.example.demo.enums.StatusReserva;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservaComputadorRepository extends BaseRepository<ReservaComputador, Long> {

    @Query("""
        SELECT r FROM ReservaComputador r
        WHERE r.computador.id = :computadorId
          AND r.status IN :statusBloqueadores
          AND r.inicioPrevisto < :fim
          AND r.fimPrevisto > :inicio
          AND r.ativo = TRUE
    """)
    List<ReservaComputador> findSobrepostas(
            Long computadorId,
            LocalDateTime inicio,
            LocalDateTime fim,
            List<StatusReserva> statusBloqueadores
    );

    @Query("""
        SELECT r FROM ReservaComputador r
        WHERE r.usuario.id = :usuarioId
          AND r.ativo = TRUE
        ORDER BY r.inicioPrevisto DESC
    """)
    List<ReservaComputador> findByUsuarioId(Long usuarioId);

    @Query("""
        SELECT r FROM ReservaComputador r
        WHERE r.status = 'EM_ANDAMENTO'
          AND r.fimPrevisto <= :agora
          AND r.checkoutEm IS NULL
          AND r.ativo = TRUE
    """)
    List<ReservaComputador> findParaAutoCheckout(LocalDateTime agora);

    @Query("""
        SELECT r FROM ReservaComputador r
        WHERE r.status = 'APROVADA'
          AND r.inicioPrevisto <= :limite
          AND r.checkinEm IS NULL
          AND r.ativo = TRUE
    """)
    List<ReservaComputador> findParaNoShow(LocalDateTime limite);
}
