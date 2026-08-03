import { apiGet, apiPost } from './client';
import type { ExportacaoSAFT, GerarSAFTRequest, GerarSAFTResponse } from '../types/saft';

export async function gerarSAFT(dados: GerarSAFTRequest): Promise<GerarSAFTResponse> {
  return apiPost<GerarSAFTResponse>('/api/saft/gerar', dados);
}

export async function listarExportacoesSAFT(): Promise<ExportacaoSAFT[]> {
  return apiGet<ExportacaoSAFT[]>('/api/saft/historico');
}

export async function downloadSAFT(id: string): Promise<Blob> {
  const response = await fetch(
    `${import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}/api/saft/${id}/download`,
    { method: 'GET' }
  );

  if (!response.ok) {
    throw new Error(`Erro ${response.status}`);
  }

  return response.blob();
}
