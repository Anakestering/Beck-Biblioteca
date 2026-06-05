package com.example.demo.repository;

import com.example.demo.entity.ReservaComputador;
import com.example.demo.enums.StatusReserva;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaComputadorRepository extends BaseRepository<ReservaComputador, Long> {

  // Verifica sobreposição de horário para o mesmo computador
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
      List<StatusReserva> statusBloqueadores);

  // Conta quantos PCs diferentes o usuário já reservou neste mesmo intervalo de
  // horário
  @Query("""
          SELECT COUNT(DISTINCT r.computador.id) FROM ReservaComputador r
          WHERE r.usuario.id = :usuarioId
            AND r.status IN :statusBloqueadores
            AND r.inicioPrevisto < :fim
            AND r.fimPrevisto > :inicio
            AND r.ativo = TRUE
      """)
  int countPcsDoUsuarioNoHorario(
      Long usuarioId,
      LocalDateTime inicio,
      LocalDateTime fim,
      List<StatusReserva> statusBloqueadores);

  // Busca reserva do usuário neste computador que termina exatamente em
  // fimPrevisto (consecutivos para trás)
  @Query("""
          SELECT r FROM ReservaComputador r
          WHERE r.usuario.id = :usuarioId
            AND r.computador.id = :computadorId
            AND r.fimPrevisto = :fimPrevisto
            AND r.status IN :statusBloqueadores
            AND r.ativo = TRUE
      """)
  Optional<ReservaComputador> findByUsuarioIdEComputadorIdEFimPrevisto(
      Long usuarioId,
      Long computadorId,
      LocalDateTime fimPrevisto,
      List<StatusReserva> statusBloqueadores);

  // Busca reserva do usuário neste computador que começa exatamente em
  // inicioPrevisto (consecutivos para frente)
  @Query("""
          SELECT r FROM ReservaComputador r
          WHERE r.usuario.id = :usuarioId
            AND r.computador.id = :computadorId
            AND r.inicioPrevisto = :inicioPrevisto
            AND r.status IN :statusBloqueadores
            AND r.ativo = TRUE
      """)
  Optional<ReservaComputador> findByUsuarioIdEComputadorIdEInicioPrevisto(
      Long usuarioId,
      Long computadorId,
      LocalDateTime inicioPrevisto,
      List<StatusReserva> statusBloqueadores);

  @Query("""
          SELECT r FROM ReservaComputador r
          LEFT JOIN FETCH r.computador
          LEFT JOIN FETCH r.usuario
          LEFT JOIN FETCH r.criadaPorUsuario
          LEFT JOIN FETCH r.pedido p
          LEFT JOIN FETCH p.usuario
          LEFT JOIN FETCH p.reservasComputador pc
          LEFT JOIN FETCH pc.computador
          WHERE r.ativo = TRUE
          ORDER BY r.inicioPrevisto DESC
      """)
  List<ReservaComputador> findAll();

  @Query("""
          SELECT r FROM ReservaComputador r
          LEFT JOIN FETCH r.computador
          LEFT JOIN FETCH r.usuario
          LEFT JOIN FETCH r.criadaPorUsuario
          LEFT JOIN FETCH r.pedido p
          LEFT JOIN FETCH p.usuario
          LEFT JOIN FETCH p.reservasComputador pc
          LEFT JOIN FETCH pc.computador
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
  List<ReservaComputador> findParaAtrasado(LocalDateTime limite);

  @Query("""
          SELECT r.inicioPrevisto, r.fimPrevisto FROM ReservaComputador r
          WHERE r.computador.id = :computadorId
            AND CAST(r.inicioPrevisto AS date) = CAST(:data AS date)
            AND r.status IN ('APROVADA', 'PENDENTE_APROVACAO', 'EM_ANDAMENTO')
            AND r.ativo = TRUE
      """)
  List<Object[]> findHorariosOcupados(Long computadorId, LocalDateTime data);

  // ─── Relatórios───────────────────────────────────────────────────────────────

  @Query("""
          SELECT r FROM ReservaComputador r
          LEFT JOIN FETCH r.computador
          WHERE r.status = 'FINALIZADA'
            AND r.ativo = TRUE
            AND (:inicio IS NULL OR r.checkinEm >= :inicio)
            AND (:fim IS NULL OR r.checkoutEm <= :fim)
            AND (:computadorIds IS NULL OR r.computador.id IN :computadorIds)
      """)
  List<ReservaComputador> findFinalizadasParaRelatorio(
      LocalDateTime inicio,
      LocalDateTime fim,
      List<Long> computadorIds);

  @Query("""
          SELECT r.computador.id, r.status, COUNT(r) FROM ReservaComputador r
          WHERE r.ativo = TRUE
            AND (:inicio IS NULL OR r.inicioPrevisto >= :inicio)
            AND (:fim IS NULL OR r.fimPrevisto <= :fim)
            AND (:computadorIds IS NULL OR r.computador.id IN :computadorIds)
            AND r.status IN ('FINALIZADA', 'CANCELADA', 'ATRASADO', 'REJEITADA')
          GROUP BY r.computador.id, r.status
      """)
  List<Object[]> findStatusReservasParaRelatorio(
      LocalDateTime inicio,
      LocalDateTime fim,
      List<Long> computadorIds);

  @Query("""
          SELECT r.checkinEm, r.checkoutEm, r.qtdePessoas
          FROM ReservaComputador r
          WHERE r.status = 'FINALIZADA'
            AND r.ativo = TRUE
            AND (:inicio IS NULL OR r.checkinEm >= :inicio)
            AND (:fim IS NULL OR r.checkoutEm <= :fim)
            AND NOT EXISTS (
              SELECT 1 FROM ReservaComputador r2
              WHERE r2.usuario.id = r.usuario.id
                AND r2.computador.id = r.computador.id
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