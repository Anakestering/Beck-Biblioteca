package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.demo.enums.TipoUsuario;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank(message = "O nome deve ser preenchido.")
    private String nome;

    @NotBlank(message = "O CPF deve ser preenchido.")
    private String cpf;

    private String telefone;

    @Email(message = "O email deve ser válido.")
    @NotBlank(message = "O email deve ser preenchido.")
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean ativo;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nivelAcesso;

    private TipoUsuario tipoUsuario;
    private UsuarioOutroInfoDTO outroInfo;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private java.time.LocalDateTime createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean pendente;
}