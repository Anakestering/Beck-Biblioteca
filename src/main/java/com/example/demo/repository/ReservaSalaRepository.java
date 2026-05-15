package com.example.demo.repository;

import com.example.demo.entity.ReservaSala;
import com.example.demo.enums.StatusReserva;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservaSalaRepository extends BaseRepository<ReservaSala, Long> {

    /**
     * Verifica sobreposição de horário para a mesma sala.
     * Condição de overlap: inicio < fimExistente AND fim > inicioExistente
     * Considera apenas status que "bloqueiam" o recurso.
     */
    @Query("""
        SELECT r FROM ReservaSala r
        WHERE r.sala.id = :salaId
          AND r.status IN :statusBloqueadores
          AND r.inicioPrevisto < :fim
          AND r.fimPrevisto > :inicio
          AND r.ativo = TRUE
    """)
    List<ReservaSala> findSobrepostas(
            Long salaId,
            LocalDateTime inicio,
            LocalDateTime fim,
            List<StatusReserva> statusBloqueadores
    );

    /**
     * Busca reservas ativas de um usuário.
     */
    @Query("""
        SELECT r FROM ReservaSala r
        WHERE r.usuario.id = :usuarioId
          AND r.ativo = TRUE
        ORDER BY r.inicioPrevisto DESC
    """)
    List<ReservaSala> findByUsuarioId(Long usuarioId);

    /**
     * Busca reservas EM_ANDAMENTO que ultrapassaram o fim previsto (para auto checkout).
     */
    @Query("""
        SELECT r FROM ReservaSala r
        WHERE r.status = 'EM_ANDAMENTO'
          AND r.fimPrevisto <= :agora
          AND r.checkoutEm IS NULL
          AND r.ativo = TRUE
    """)
    List<ReservaSala> findParaAutoCheckout(LocalDateTime agora);

    /**
     * Busca reservas APROVADAS com inicio há mais de 15min e sem check-in (no-show).
     */
    @Query("""
        SELECT r FROM ReservaSala r
        WHERE r.status = 'APROVADA'
          AND r.inicioPrevisto <= :limite
          AND r.checkinEm IS NULL
          AND r.ativo = TRUE
    """)
    List<ReservaSala> findParaNoShow(LocalDateTime limite);
}