package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.AnaliseResponse;
import isaf.tfc.autolancamentosbackend.dto.ContextoClassificacaoDTO;

/**
 * Abstração do fornecedor de análise de documentos (OCR + classificação + contas).
 * O AnaliseContabilService depende só desta interface, nunca de um cliente HTTP
 * concreto — permite trocar de fornecedor de IA sem alterar a regra de negócio.
 */
public interface AnalisadorDocumentoIA {

    /**
     * `fingerprint` (Fase 4 do mapa de impacto — ver
     * fastapi/app/services/document_fingerprint.py): identidade de
     * conteúdo já calculada no upload (DocumentoContabilistico.hashConteudo),
     * reenviada aqui para o FastAPI não ter de recalculá-la e para servir de
     * chave estável ao cache de OCR/validação das próximas fases. Pode ser
     * null (ex. chamadas fora do fluxo de upload normal) — nesse caso o
     * FastAPI calcula-a a partir dos bytes recebidos.
     *
     * `contextoEmpresa` (Fase 5 do plano de 20 fases — classificação
     * contabilística): reaproveita ContextoClassificacaoDTO (Fase 3, ver
     * ContextoClassificacaoService) só pelos campos de empresa — sempre
     * disponível, ao contrário do histórico da entidade, que só se
     * conhece depois desta chamada. Pode ser null.
     */
    AnaliseResponse analisar(byte[] conteudo, String nomeFicheiro, String fingerprint, ContextoClassificacaoDTO contextoEmpresa);
}
