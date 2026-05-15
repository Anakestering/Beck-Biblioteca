package com.example.demo.service;

import com.example.demo.dto.SalaDTO;
import com.example.demo.entity.Sala;
import com.example.demo.repository.SalaRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalaService {

    private final SalaRepository repo;

    public SalaService(SalaRepository repo) {
        this.repo = repo;
    }

    public List<SalaDTO> listar() {
        return repo.findAll().stream().map(this::toDto).toList();
    }

    public SalaDTO buscar(Long id) {
        return toDto(repo.findById(id).orElseThrow(() -> new RuntimeException("Sala não encontrada.")));
    }

    @Transactional
    public SalaDTO criar(SalaDTO dto) {
        if (repo.existsByNome(dto.getNome())) {
            throw new RuntimeException("Já existe uma sala com este nome.");
        }
        Sala sala = new Sala();
        BeanUtils.copyProperties(dto, sala, "id");
        return toDto(repo.save(sala));
    }

    @Transactional
    public SalaDTO atualizar(Long id, SalaDTO dto) {
        Sala sala = repo.findById(id).orElseThrow(() -> new RuntimeException("Sala não encontrada."));
        sala.setNome(dto.getNome());
        sala.setCapacidadePessoas(dto.getCapacidadePessoas());
        return toDto(repo.save(sala));
    }

    @Transactional
    public void deletar(Long id) {
        repo.softDeleteById(id);
    }

    private SalaDTO toDto(Sala sala) {
        SalaDTO dto = new SalaDTO();
        BeanUtils.copyProperties(sala, dto);
        return dto;
    }
}