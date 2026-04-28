package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.annotations.Admin;
import com.example.demo.annotations.Public;

@RestController
@RequestMapping("/test-security")
public class TesteSecurityController {

    @GetMapping("/public")
    @Public
    public String rotaPublica(){
        return "public";
    }

    @GetMapping("/admin")
    @Admin
    public String rotaAdmin(){
        return "admin";
    }
}
