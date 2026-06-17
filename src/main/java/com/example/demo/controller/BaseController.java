package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.demo.service.BaseService;

import jakarta.validation.Valid;

public abstract class BaseController<D> {

    protected BaseService<?, D> service;
    
    protected BaseController(BaseService<?, D> service){
        this.service = service;
    }

    @PostMapping
    public D create(@RequestBody @Valid D dto){
        return service.create(dto);
    }

    @GetMapping("{id}")
    public D read(@PathVariable Long id){
        return service.read(id);
    }

    @GetMapping
    public List<D> read(){
        return service.read();
    }

    @PutMapping("/{id}")
    public D update(@PathVariable Long id, @RequestBody @Valid D dto){
        return service.update(id, dto);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable Long id){
        service.softDelete(id);
    }

}
