package com.example.demo.service;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.springframework.beans.BeanUtils;
import com.example.demo.entity.BaseEntity;
import com.example.demo.repository.BaseRepository;
import jakarta.transaction.Transactional;

public abstract class BaseService<E extends BaseEntity, D> {

    private final BaseRepository<E, Long> repository;
    private final Class<E> entityClass;
    private final Class<D> dtoClass;

    @SuppressWarnings("unchecked")
    protected BaseService(BaseRepository<E, Long> repository) {
        this.repository = repository;

        ParameterizedType baseSuperClass = (ParameterizedType) getClass().getGenericSuperclass();

        this.entityClass = (Class<E>) baseSuperClass.getActualTypeArguments()[0];
        this.dtoClass = (Class<D>) baseSuperClass.getActualTypeArguments()[1];

    }

    @Transactional
    public D create(D dto) {
        return toDto(repository.save(toEntity(dto)));
    }

    @Transactional
    public D update(Long id, D dto) {
        E e = toEntity(dto);
        e.setId(id);

        return toDto(repository.save(e));
    }

    public D read(Long id) {
        E e = repository.findById(id).orElseThrow();

        return toDto(e);
    }

    public List<D> read() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void softDelete(Long id) {
        repository.softDeleteById(id);
    }

    public E toEntity(D dto) {
        try {
            E e = entityClass.getDeclaredConstructor().newInstance();

            BeanUtils.copyProperties(dto, e);

            return e;
        } catch (Exception ex) {
            throw new RuntimeException("Erro ao converter de DTO para Entity");
        }
    }

    public D toDto(E entity) {
        try {
            D dto = dtoClass.getDeclaredConstructor().newInstance();

            BeanUtils.copyProperties(entity, dto);

            return dto;
        } catch (Exception ex) {
            throw new RuntimeException("Erro ao converter de Entity para DTO");
        }
    }
}