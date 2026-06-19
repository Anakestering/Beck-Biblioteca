package com.example.demo.dtoEstatisticas;

import java.util.List;
import java.util.Map;

public record EstatisticasUsuariosDTO(
    List<DistribuicaoTipoDTO> distribuicao,
    List<RankingUsuarioDTO> ranking,
    List<RankingUsuarioDTO> naoCompareceram,
    List<CrescimentoMesDTO> crescimento,
    long totalAtivos,
    long totalCadastrados,
    /** Total de usuários cadastrados por tipoUsuario (todos os tempos) */
    Map<String, Long> totalPorTipo,
    /** Novos cadastros no período por tipoUsuario */
    Map<String, Long> novosPorTipo,
    /** Usuários com statusConta=ATIVO por tipoUsuario */
    Map<String, Long> ativosPorTipo
) {}
