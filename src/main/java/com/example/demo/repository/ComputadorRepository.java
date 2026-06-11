package com.example.demo.repository;

import com.example.demo.entity.Computador;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComputadorRepository extends BaseRepository<Computador, Long> {

    boolean existsByCodigo(String codigo);

    @Query("SELECT c FROM Computador c ORDER BY c.ativo DESC, c.codigo ASC")
    List<Computador> findTodas();

    long countByAtivoTrue();
}