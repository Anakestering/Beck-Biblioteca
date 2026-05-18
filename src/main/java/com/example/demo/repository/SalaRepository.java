package com.example.demo.repository;

import com.example.demo.entity.Sala;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaRepository extends BaseRepository<Sala, Long> {

    boolean existsByNome(String nome);

    @Query("SELECT s FROM Sala s ORDER BY s.ativo DESC, s.nome ASC")
    List<Sala> findTodas();
}