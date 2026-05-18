package com.example.demo.service;

import com.example.demo.dto.ComputadorDTO;
import com.example.demo.entity.Computador;
import com.example.demo.repository.ComputadorRepository;
import jakarta.transaction.Transactional;
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

    public List<ComputadorDTO> listarTodos() {
        return repo.findTodas().stream().map(this::toDto).toList();
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
        Computador c = new Computador();
        c.setCodigo(dto.getCodigo());
        c.setCapacidadePessoas(dto.getCapacidadePessoas());
        c.setObservacao(dto.getObservacao());
        c.setAtivo(true);
        return toDto(repo.save(c));
    }

    @Transactional
    public ComputadorDTO atualizar(Long id, ComputadorDTO dto) {
        Computador c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Computador não encontrado."));
        c.setCodigo(dto.getCodigo());
        c.setCapacidadePessoas(dto.getCapacidadePessoas());
        c.setObservacao(dto.getObservacao());
        return toDto(repo.save(c));
    }

    @Transactional
    public void ativar(Long id) {
        Computador c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Computador não encontrado."));
        c.setAtivo(true);
        repo.save(c);
    }

    @Transactional
    public void desativar(Long id) {
        repo.softDeleteById(id);
    }

    @Transactional
    public void deletar(Long id) {
        repo.deleteById(id);
    }

    private ComputadorDTO toDto(Computador c) {
        ComputadorDTO dto = new ComputadorDTO();
        dto.setId(c.getId());
        dto.setCodigo(c.getCodigo());
        dto.setCapacidadePessoas(c.getCapacidadePessoas());
        dto.setObservacao(c.getObservacao());
        dto.setAtivo(c.isAtivo());
        return dto;
    }
}