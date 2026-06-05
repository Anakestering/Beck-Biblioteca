package com.example.demo.repository;

import com.example.demo.entity.ReservaSala;
import com.example.demo.enums.StatusReserva;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaSalaRepository extends BaseRepository<ReservaSala, Long> {

  // Verifica sobreposição de horário para a mesma sala
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
      List<StatusReserva> statusBloqueadores);

  // Busca reserva do usuário nesta sala que termina exatamente em fimPrevisto
  // (consecutivos para trás)
  @Query("""
          SELECT r FROM ReservaSala r
          WHERE r.usuario.id = :usuarioId
            AND r.sala.id = :salaId
            AND r.fimPrevisto = :fimPrevisto
            AND r.status IN :statusBloqueadores
            AND r.ativo = TRUE
      """)
  Optional<ReservaSala> findByUsuarioIdESalaIdEFimPrevisto(
      Long usuarioId,
      Long salaId,
      LocalDateTime fimPrevisto,
      List<StatusReserva> statusBloqueadores);

  // Busca reserva do usuário nesta sala que começa exatamente em inicioPrevisto
  // (consecutivos para frente)
  @Query("""
          SELECT r FROM ReservaSala r
          WHERE r.usuario.id = :usuarioId
            AND r.sala.id = :salaId
            AND r.inicioPrevisto = :inicioPrevisto
            AND r.status IN :statusBloqueadores
            AND r.ativo = TRUE
      """)
  Optional<ReservaSala> findByUsuarioIdESalaIdEInicioPrevisto(
      Long usuarioId,
      Long salaId,
      LocalDateTime inicioPrevisto,
      List<StatusReserva> statusBloqueadores);

  @Query("""
          SELECT r FROM ReservaSala r
          LEFT JOIN FETCH r.sala
          LEFT JOIN FETCH r.usuario
          LEFT JOIN FETCH r.criadaPorUsuario
          LEFT JOIN FETCH r.pedido p
          LEFT JOIN FETCH p.usuario
          LEFT JOIN FETCH p.reservasSala rs
          LEFT JOIN FETCH rs.sala
          WHERE r.ativo = TRUE
          ORDER BY r.inicioPrevisto DESC
      """)
  List<ReservaSala> findAll();

  @Query("""
          SELECT r FROM ReservaSala r
          LEFT JOIN FETCH r.sala
          LEFT JOIN FETCH r.usuario
          LEFT JOIN FETCH r.criadaPorUsuario
          LEFT JOIN FETCH r.pedido p
          LEFT JOIN FETCH p.usuario
          LEFT JOIN FETCH p.reservasSala rs
          LEFT JOIN FETCH rs.sala
          WHERE r.usuario.id = :usuarioId
            AND r.ativo = TRUE
          ORDER BY r.inicioPrevisto DESC
      """)
  List<ReservaSala> findByUsuarioId(Long usuarioId);

  // Reservas EM_ANDAMENTO que já passaram do fim (auto checkout)
  @Query("""
          SELECT r FROM ReservaSala r
          WHERE r.status = 'EM_ANDAMENTO'
            AND r.fimPrevisto <= :agora
            AND r.checkoutEm IS NULL
            AND r.ativo = TRUE
      """)
  List<ReservaSala> findParaAutoCheckout(LocalDateTime agora);

  // Reservas APROVADAS sem check-in depois dos 15min de tolerância
  @Query("""
          SELECT r FROM ReservaSala r
          WHERE r.status = 'APROVADA'
            AND r.inicioPrevisto <= :limite
            AND r.checkinEm IS NULL
            AND r.ativo = TRUE
      """)
  List<ReservaSala> findParaAtrasado(LocalDateTime limite);

  @Query("""
          SELECT r.inicioPrevisto, r.fimPrevisto FROM ReservaSala r
          WHERE r.sala.id = :salaId
            AND CAST(r.inicioPrevisto AS date) = CAST(:data AS date)
            AND r.status IN ('APROVADA', 'PENDENTE_APROVACAO', 'EM_ANDAMENTO')
            AND r.ativo = TRUE
      """)
  List<Object[]> findHorariosOcupados(Long salaId, LocalDateTime data);

  // ─── Relatórios───────────────────────────────────────────────────────────────

  @Query("""
          SELECT r FROM ReservaSala r
          LEFT JOIN FETCH r.sala
          WHERE r.status = 'FINALIZADA'
            AND r.ativo = TRUE
            AND (:inicio IS NULL OR r.checkinEm >= :inicio)
            AND (:fim IS NULL OR r.checkoutEm <= :fim)
            AND (:salaIds IS NULL OR r.sala.id IN :salaIds)
      """)
  List<ReservaSala> findFinalizadasParaRelatorio(
      LocalDateTime inicio,
      LocalDateTime fim,
      List<Long> salaIds);

  @Query("""
          SELECT r.sala.id, r.status, COUNT(r) FROM ReservaSala r
          WHERE r.ativo = TRUE
            AND (:inicio IS NULL OR r.inicioPrevisto >= :inicio)
            AND (:fim IS NULL OR r.fimPrevisto <= :fim)
            AND (:salaIds IS NULL OR r.sala.id IN :salaIds)
            AND r.status IN ('FINALIZADA', 'CANCELADA', 'ATRASADO', 'REJEITADA')
          GROUP BY r.sala.id, r.status
      """)
  List<Object[]> findStatusReservasParaRelatorio(
      LocalDateTime inicio,
      LocalDateTime fim,
      List<Long> salaIds);

  @Query("""
          SELECT r.checkinEm, r.checkoutEm, r.qtdePessoas
          FROM ReservaSala r
          WHERE r.status = 'FINALIZADA'
            AND r.ativo = TRUE
            AND (:inicio IS NULL OR r.checkinEm >= :inicio)
            AND (:fim IS NULL OR r.checkoutEm <= :fim)
            AND NOT EXISTS (
              SELECT 1 FROM ReservaSala r2
              WHERE r2.usuario.id = r.usuario.id
                AND r2.sala.id = r.sala.id
                AND r2.checkinEm = r.checkinEm
                AND r2.inicioPrevisto = r.fimPrevisto
                AND r2.status = 'FINALIZADA'
                AND r2.ativo = TRUE
            )
      """)
  List<Object[]> findHeatmapParaRelatorio(
      LocalDateTime inicio,
      LocalDateTime fim);
}