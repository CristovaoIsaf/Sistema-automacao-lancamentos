import { apiGet } from './client';
import type { LivroRazaoResponse } from '../types/livroRazao';

// Fase 18 do plano de 20 fases — "conta" por query param (ver
// LivroRazaoController: códigos como "34.5.1" têm pontos, arriscado num
// @PathVariable no último segmento do URL).
export async function obterLivroRazao(conta: string, inicio?: string, fim?: string): Promise<LivroRazaoResponse> {
  const params = new URLSearchParams({ conta });
  if (inicio) params.append('inicio', inicio);
  if (fim) params.append('fim', fim);
  return apiGet<LivroRazaoResponse>(`/api/livro-razao?${params.toString()}`);
}
