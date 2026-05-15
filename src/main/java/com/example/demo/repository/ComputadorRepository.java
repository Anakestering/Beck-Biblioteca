package com.example.demo.repository;

import com.example.demo.entity.Computador;
import org.springframework.stereotype.Repository;

@Repository
public interface ComputadorRepository extends BaseRepository<Computador, Long> {

    boolean existsByCodigo(String codigo);
}