package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Versão de DocumentoContabilistico sem o campo "conteudo" (byte[]),
 * para não devolver o ficheiro inteiro em cada item de uma listagem.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoResponseDTO {

    private Long id;

    private String nomeFicheiro;

    private String tipoConteudo;

    private LocalDateTime dataUpload;

    // Campos aditivos — arquivo organizado (ver DocumentoController.listar()).
    private Long entidadeId;

    private String entidadeNome;

    private String entidadeNif;

    private long tamanho;

    // "Pendente" | "Analisado" | "Aprovado" | "Rejeitado" — derivado da Sugestao
    // mais recente ligada a este documento (ver DocumentoController.listar()).
    private String estado;

    // Fase 10 do plano de 20 fases ("pesquisa: entidade; NIF; número;
    // série; tipo; data; valor") — também vindos da Sugestao mais recente
    // (podem ser null se o documento ainda não foi analisado).
    private String tipoDocumento;

    private String numeroDocumento;

    private String valor;

    // Fase 16 do plano de 20 fases ("auditor: inconsistências") — o
    // ResultadoValidacao (motor determinístico, ver
    // fastapi/app/services/document_validation.py) da Sugestao mais
    // recente, tal e qual persistido em Sugestao.validacaoJson. Já existia
    // no fluxo de upload (LancamentoDiario.tsx) mas nunca tinha sido
    // exposto para consulta posterior — reutiliza o mesmo parseValidacao()
    // do frontend, sem novo formato. null se o documento ainda não foi
    // analisado ou a sugestão é anterior à Fase 3 (sem validação).
    private String validacaoJson;

    // Auditoria C05 ("a sugestão original fica preservada mesmo após
    // correção humana") — null enquanto a classificação nunca foi corrigida
    // por um humano; ver Sugestao.tipoDocumentoOriginalIA e
    // DocumentoController.corrigirClassificacao.
    private String tipoDocumentoOriginalIA;

    private String numeroDocumentoOriginalIA;

    private String valorOriginalIA;

    private java.time.LocalDateTime corrigidoEm;
}
