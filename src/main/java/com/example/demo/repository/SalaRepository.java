// ─── SalaRepository.java ─────────────────────────────────────────────────────
package com.example.demo.repository;

import com.example.demo.entity.Sala;
import org.springframework.stereotype.Repository;

@Repository
public interface SalaRepository extends BaseRepository<Sala, Long> {
    boolean existsByNome(String nome);
}