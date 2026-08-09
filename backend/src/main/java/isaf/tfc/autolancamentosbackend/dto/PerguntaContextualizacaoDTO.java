package isaf.tfc.autolancamentosbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Fase 4 do plano de 20 fases — "contextualização assistida": pergunta
 * fechada e específica, só devolvida pelo FastAPI quando o sistema não
 * conseguiu decidir sozinho o tipo de operação (regras + perfil + IA
 * caíram todos em a_classificar). Nunca um chatbot genérico — sempre uma
 * pergunta com opções fixas, cada uma já com o lançamento pré-calculado
 * (ver OpcaoContextualizacaoDTO).
 */
@Data
@NoArgsConstructor
public class PerguntaContextualizacaoDTO {

    private String pergunta;
    private List<OpcaoContextualizacaoDTO> opcoes;
}
