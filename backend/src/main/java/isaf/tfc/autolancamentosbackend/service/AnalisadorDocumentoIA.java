package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.AnaliseResponse;

/**
 * Abstração do fornecedor de análise de documentos (OCR + classificação + contas).
 * O AnaliseContabilService depende só desta interface, nunca de um cliente HTTP
 * concreto — permite trocar de fornecedor de IA sem alterar a regra de negócio.
 */
public interface AnalisadorDocumentoIA {

    AnaliseResponse analisar(byte[] conteudo, String nomeFicheiro);
}
