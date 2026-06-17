package com.example.demo.dto;

import com.example.demo.enums.TipoUsuario;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AtualizarTipoDTO {
    private TipoUsuario tipoUsuario;
    private UsuarioOutroInfoDTO outroInfo;
}
