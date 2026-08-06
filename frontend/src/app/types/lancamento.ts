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

}