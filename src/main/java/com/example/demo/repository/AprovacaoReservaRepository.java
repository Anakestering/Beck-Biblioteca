package com.example.demo.repository;

import com.example.demo.entity.AprovacaoReserva;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AprovacaoReservaRepository extends BaseRepository<AprovacaoReserva, Long> {

    Optional<AprovacaoReserva> findByReservaSalaId(Long reservaSalaId);

    Optional<AprovacaoReserva> findByReservaComputadorId(Long reservaComputadorId);
}