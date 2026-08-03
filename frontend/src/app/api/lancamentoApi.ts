import { apiGet, apiGetBlob, apiPost } from "./client";

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