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

    // Fase 9 do plano de 20 fases — histórico filtrável por documento/
    // entidade/utilizador (ver LancamentoEnriquecimentoService no backend).
    // null para lançamentos MANUAL (sem documento/entidade de origem).
    documentoId?: number | null;

    entidadeId?: number | null;

    entidadeNome?: string | null;

    validadoPor?: number | null;

    validadoPorNome?: string | null;

}