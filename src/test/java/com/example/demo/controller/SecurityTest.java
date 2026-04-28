package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

import com.example.demo.config.JwtUtil;
import com.example.demo.enums.NivelAcesso;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTest {

    @Autowired
    private JwtUtil jwt;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void verificaRotaPublica() throws Exception{
        mockMvc.perform(get("/test-security/public"))
        .andExpect(status().isOk())
        .andExpect(content().string("public"));
    }

    @Test
    void verificaRotaAdmin() throws Exception{
        String token = jwt.generateToken(
            "teste@teste.com", NivelAcesso.ADMIN.toString());

        mockMvc.perform(get("/test-security/admin")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(content().string("admin"));
    }

    @Test
    void verificaAdminSemLogin() throws Exception{
        mockMvc.perform(get("/test-security/admin"))
        .andExpect(status().isUnauthorized());
    }

   

    @Test
    void verificaRotaAdminComUsuarioPadrao() throws Exception{
        String token = jwt.generateToken(
            "teste@teste.com", NivelAcesso.PADRAO.toString());

        mockMvc.perform(get("/test-security/admin")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
    }
}
