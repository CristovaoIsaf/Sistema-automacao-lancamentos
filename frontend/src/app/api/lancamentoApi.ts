import { apiGet, apiGetBlob, apiPost, apiPut } from "./client";

import type {
    LancamentoHistoricoVersao,
    LancamentoRequest,
    LancamentoResponse
} from "../types/lancamento";


export async function criarLancamento(
    dados: LancamentoRequest
): Promise<LancamentoResponse> {

    return apiPost<LancamentoResponse>(
        "/api/lancamentos",
        dados
    );

}


// Auditoria de performance: GET /api/lancamentos passou a ser paginado no
// backend (antes devolvia sempre o histórico inteiro, sem limite). Mesma
// escolha que listarDocumentos (documentoApi.ts): pede uma única página
// generosa, para Lancamentos.tsx continuar a filtrar/contar em memória
// sobre a lista completa sem precisar de "carregar mais" — só deixa de
// ser um "SELECT * sem limite" do lado do servidor.
export async function listarLancamentos(): Promise<LancamentoResponse[]> {

    const pagina = await apiGet<{ itens: LancamentoResponse[]; total: number }>(
        "/api/lancamentos?limite=500"
    );
    return pagina.itens;

}


export async function buscarLancamento(id: number): Promise<LancamentoResponse> {

    return apiGet<LancamentoResponse>(
        `/api/lancamentos/${id}`
    );

}


// LancamentoServiceImpl.atualizar persiste data/descrição e, se `linhas`
// vier preenchido, substitui também as linhas de débito/crédito (revalida
// o equilíbrio) — ver LancamentoDiario.tsx, que envia sempre o conjunto
// completo de linhas ao editar um lançamento existente.
export async function atualizarLancamento(
    id: number,
    dados: LancamentoRequest
): Promise<LancamentoResponse> {

    return apiPut<LancamentoResponse>(
        `/api/lancamentos/${id}`,
        dados
    );

}


// Auditoria C04 — versões anteriores deste lançamento (uma por cada
// edição), mais recente primeiro.
export async function listarHistoricoLancamento(id: number): Promise<LancamentoHistoricoVersao[]> {
    return apiGet<LancamentoHistoricoVersao[]>(`/api/lancamentos/${id}/historico`);
}

// Auditoria C01 — aprova um lançamento manual PENDENTE criado por OUTRO
// contabilista; o backend rejeita (403/erro) se o utilizador autenticado
// for quem o criou.
export async function aprovarLancamento(id: number): Promise<LancamentoResponse> {
    return apiPost<LancamentoResponse>(`/api/lancamentos/${id}/aprovar`, {});
}

// Auditoria C03 — pede a anulação de um lançamento validado; motivo é
// obrigatório (ver LancamentoServiceImpl.solicitarCancelamento).
export async function solicitarCancelamentoLancamento(id: number, motivo: string): Promise<LancamentoResponse> {
    return apiPost<LancamentoResponse>(`/api/lancamentos/${id}/solicitar-cancelamento`, { motivo });
}

// Auditoria C03 — aprova o pedido de anulação (por OUTRO contabilista):
// devolve o lançamento de estorno gerado automaticamente.
export async function aprovarCancelamentoLancamento(id: number): Promise<LancamentoResponse> {
    return apiPost<LancamentoResponse>(`/api/lancamentos/${id}/aprovar-cancelamento`, {});
}

// Auditoria C03 — rejeita o pedido de anulação (por OUTRO contabilista): o
// lançamento volta a VALIDADO, sem nenhum estorno.
export async function rejeitarCancelamentoLancamento(id: number): Promise<LancamentoResponse> {
    return apiPost<LancamentoResponse>(`/api/lancamentos/${id}/rejeitar-cancelamento`, {});
}


/**
 * Descarrega os lançamentos do período indicado em Excel (.xlsx) e
 * dispara o download no browser.
 */
export async function exportarLancamentos(
    inicio: string,
    fim: string,
    estado?: string
): Promise<void> {

    const params = new URLSearchParams({ inicio, fim });
    if (estado) {
        params.set("estado", estado);
    }

    const blob = await apiGetBlob(
        `/api/lancamentos/exportar?${params.toString()}`
    );

    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "lancamentos.xlsx";
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
}