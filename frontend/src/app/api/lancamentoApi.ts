import { apiGet, apiGetBlob, apiPost, apiPut } from "./client";

import type {
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


export async function listarLancamentos(): Promise<LancamentoResponse[]> {

    return apiGet<LancamentoResponse[]>(
        "/api/lancamentos"
    );

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