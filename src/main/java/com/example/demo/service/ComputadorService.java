package com.example.demo.service;

import com.example.demo.dto.ComputadorDTO;
import com.example.demo.entity.Computador;
import com.example.demo.repository.ComputadorRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComputadorService {

    private final ComputadorRepository repo;

    public ComputadorService(ComputadorRepository repo) {
        this.repo = repo;
    }

    public List<ComputadorDTO> listar() {
        return repo.findAll().stream().map(this::toDto).toList();
    }

    public ComputadorDTO buscar(Long id) {
        return toDto(repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Computador não encontrado.")));
    }

    @Transactional
    public ComputadorDTO criar(ComputadorDTO dto) {
        if (repo.existsByCodigo(dto.getCodigo())) {
            throw new RuntimeException("Já existe um computador com este código.");
        }
        Computador computador = new Computador();
        computador.setCodigo(dto.getCodigo());
        computador.setCapacidadePessoas(dto.getCapacidadePessoas());
        return toDto(repo.save(computador));
    }

    @Transactional
    public ComputadorDTO atualizar(Long id, ComputadorDTO dto) {
        Computador computador = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Computador não encontrado."));
        computador.setCodigo(dto.getCodigo());
        computador.setCapacidadePessoas(dto.getCapacidadePessoas());
        return toDto(repo.save(computador));
    }

    @Transactional
    public void deletar(Long id) {
        repo.softDeleteById(id);
    }

    private ComputadorDTO toDto(Computador c) {
        ComputadorDTO dto = new ComputadorDTO();
        BeanUtils.copyProperties(c, dto);
        return dto;
    }
}