package com.example.demo.repository;

import com.example.demo.entity.AprovacaoReserva;
import com.example.demo.enums.StatusAprovacao;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AprovacaoReservaRepository extends BaseRepository<AprovacaoReserva, Long> {

    Optional<AprovacaoReserva> findByReservaSalaId(Long reservaSalaId);

    Optional<AprovacaoReserva> findByReservaComputadorId(Long reservaComputadorId);

    @Query("""
        SELECT a FROM AprovacaoReserva a
        WHERE a.status = :status
          AND a.ativo = TRUE
    """)
    List<AprovacaoReserva> findByStatus(StatusAprovacao status);
}