export interface LinhaLancamento {

    conta: string;

    debito: number;

    credito: number;

    descricao: string;

}

export interface LancamentoRequest {

    data: string;

    descricao: string;

    linhas: LinhaLancamento[];

}

export interface LancamentoResponse {

    id: number;

    data: string;

    descricao: string;

    estado: string;

    origem: string;

    editadoManualmente?: boolean | null;

    linhas: LinhaLancamento[];

    // Modelação de IVA no domínio Java — soma das linhas de IVA
    // (34.5.1/34.5.2), sempre presente (0 quando não há linha de IVA).
    valorIva?: number;

    // Fase 9 do plano de 20 fases — histórico filtrável por documento/
    // entidade/utilizador (ver LancamentoEnriquecimentoService no backend).
    // null para lançamentos MANUAL (sem documento/entidade de origem).
    documentoId?: number | null;

    entidadeId?: number | null;

    entidadeNome?: string | null;

    validadoPor?: number | null;

    validadoPorNome?: string | null;

    // Auditoria C01/C03 — ver LancamentoEnriquecimentoService no backend.
    criadoPor?: number | null;

    criadoPorNome?: string | null;

    motivoCancelamento?: string | null;

    cancelamentoSolicitadoPor?: number | null;

    cancelamentoSolicitadoPorNome?: string | null;

    estornoDeId?: number | null;

}

// Auditoria C04 — uma versão anterior de um lançamento (ver
// LancamentoHistoricoDTO no backend).
export interface LancamentoHistoricoVersao {

    id: number;

    dataAnterior: string;

    descricaoAnterior: string;

    linhasAnteriores: LinhaLancamento[];

    alteradoPor?: number | null;

    alteradoPorNome?: string | null;

    alteradoEm: string;

}