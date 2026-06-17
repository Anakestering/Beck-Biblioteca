package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecuperarSenhaDTO {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String codigo;

    @NotBlank
    @Size(min = 8, max = 18, message = "A senha deve ter no mínimo 8 caracteres.")
    private String novaSenha;
    
}
