import { apiGet } from './client';
import type { BalanceteResponse } from '../types/balancete';

export async function obterBalancete(inicio?: string, fim?: string): Promise<BalanceteResponse> {
  const params = new URLSearchParams();
  if (inicio) params.append('inicio', inicio);
  if (fim) params.append('fim', fim);
  const qs = params.toString();
  return apiGet<BalanceteResponse>(`/api/balancetes${qs ? `?${qs}` : ''}`);
}
