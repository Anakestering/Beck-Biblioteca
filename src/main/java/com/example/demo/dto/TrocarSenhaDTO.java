package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrocarSenhaDTO {
    
    @NotBlank(message = "A senha atual deve ser informada.")
    private String senhaAtual;

    @NotBlank
    @Size(min = 8, max = 18, message = "A nova senha deve ter entre 8 e 18 caracteres.")
    private String novaSenha;
}




