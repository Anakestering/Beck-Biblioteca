package com.example.demo.repository;

import com.example.demo.entity.UsuarioOutroInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioOutroInfoRepository extends JpaRepository<UsuarioOutroInfo, Long> {
    Optional<UsuarioOutroInfo> findByUsuarioIdAndAtivoTrue(Long usuarioId);
    Optional<UsuarioOutroInfo> findByUsuarioId(Long usuarioId);
}