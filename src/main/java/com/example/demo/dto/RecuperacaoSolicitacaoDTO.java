package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecuperacaoSolicitacaoDTO {
    
    @NotBlank(message = "O email deve ser preenchido.")
    @Email(message = "O email deve ser válido.")
    private String email;
}
