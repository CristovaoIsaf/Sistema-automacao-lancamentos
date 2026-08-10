import { apiGet, apiGetNullable } from './client';
import type { NotaConta, RedacaoNota } from '../types/notaConta';

// Fase 14 do plano de 20 fases — "Notas às Contas".
export async function obterNotaConta(conta: string, inicio?: string, fim?: string): Promise<NotaConta> {
  const params = new URLSearchParams();
  if (inicio) params.append('inicio', inicio);
  if (fim) params.append('fim', fim);
  const qs = params.toString();
  return apiGet<NotaConta>(`/notas/${encodeURIComponent(conta)}${qs ? `?${qs}` : ''}`);
}

// Devolve null quando o FastAPI não respondeu (ver NotaContaController —
// 204 No Content) — nunca um erro, é uma parte opcional da nota.
export async function obterRedacaoNota(conta: string, inicio?: string, fim?: string): Promise<RedacaoNota | null> {
  const params = new URLSearchParams();
  if (inicio) params.append('inicio', inicio);
  if (fim) params.append('fim', fim);
  const qs = params.toString();
  return apiGetNullable<RedacaoNota>(`/notas/${encodeURIComponent(conta)}/redacao${qs ? `?${qs}` : ''}`);
}
