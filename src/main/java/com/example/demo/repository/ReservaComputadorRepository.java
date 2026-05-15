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
            List<StatusReserva> statusBloqueadores
    );

    // Conta quantos PCs diferentes o usuário já reservou neste mesmo intervalo de horário
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
            List<StatusReserva> statusBloqueadores
    );

    // Busca reserva do usuário neste computador que termina exatamente em fimPrevisto (consecutivos para trás)
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
            List<StatusReserva> statusBloqueadores
    );

    // Busca reserva do usuário neste computador que começa exatamente em inicioPrevisto (consecutivos para frente)
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
    List<ReservaComputador> findParaAtrasado(LocalDateTime limite);
}