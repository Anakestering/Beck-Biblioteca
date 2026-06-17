package com.example.demo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.enums.NivelAcesso;

public class JwtUtilTest {

    //ARRANGE
    private JwtUtil jwt;

    
    //antes de cada, execulte esse aq
    @BeforeEach
    //cria uma chave ficticia para iniciar os testes
    void configurar(){
        jwt = new JwtUtil();

        ReflectionTestUtils.setField(jwt, "secret", "chave-secreta-de-teste-com-pelo-menos-32-caracteres-aqui!");

        jwt.init();
    }

    @Test@DisplayName("O teste deve verificar se o token retorna o email corretamente")
    void deveExtrairEmailDoToken(){

        //ARRANGE - ORGANIZAR/ARRUMAR (preparando ambiente de teste)
        String email = "email@teste.com";
        String token = jwt.generateToken(email, NivelAcesso.ADMIN.toString());

        //ACT - AGIR
        String emailExtraido = jwt.extractUsername(token);

        //ASSERT - VERIFICAR/AFIRMAR
        assertEquals(email, emailExtraido, "O email extraído deve ser idêntico ao email usado na geração");

    }
    
    @Test
    @DisplayName("O teste deve retornar se o nível de acesso está correto")
    void deveExtrairNivelAcessoDoToken(){
        //ARRANGE
        String token = jwt.generateToken("email@teste.com", NivelAcesso.ADMIN.toString());

        //ACT
        String role = jwt.extractRole(token);

        //ASSERT
        assertEquals(NivelAcesso.ADMIN.toString(), role);
    }

    @Test
    @DisplayName("Validar token adulterado")
    void validarAdulterado(){
        String token = jwt.generateToken("usuario@teste.com", NivelAcesso.PADRAO.toString());

        String tokenAtualizado = token + "xxxmalicioso";

        boolean valido = jwt.validateToken(tokenAtualizado);

        assertFalse(valido);
    }

    @Test
    @DisplayName("Verificar token vazio")
    void ValidarTokenVazio(){
        String token = "";

        boolean valido = jwt.validateToken(token);

        assertFalse(valido);
    }

}
