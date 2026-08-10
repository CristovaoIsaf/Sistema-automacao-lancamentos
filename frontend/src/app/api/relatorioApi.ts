import { apiGet } from './client';
import type { RelatorioBalanco, RelatorioDRE } from '../types/relatorio';

// Fase 13 do plano de 20 fases — DRE/Balanço reais, construídos no
// backend a partir do mesmo balancete usado em /balancetes (ver
// DemonstracoesFinanceirasService). obterBalancete/obterRazao/
// exportarRelatorio foram removidos daqui: Balancete já tem a sua própria
// API real (balanceteApi.ts, usada por Balancetes.tsx) e Razão/exportação
// nunca tiveram nenhum endpoint a suportá-los.
export async function obterDRE(inicio?: string, fim?: string): Promise<RelatorioDRE> {
  const params = new URLSearchParams();
  if (inicio) params.append('inicio', inicio);
  if (fim) params.append('fim', fim);
  const qs = params.toString();
  return apiGet<RelatorioDRE>(`/api/demonstracoes/dre${qs ? `?${qs}` : ''}`);
}

export async function obterBalanco(inicio?: string, fim?: string): Promise<RelatorioBalanco> {
  const params = new URLSearchParams();
  if (inicio) params.append('inicio', inicio);
  if (fim) params.append('fim', fim);
  const qs = params.toString();
  return apiGet<RelatorioBalanco>(`/api/demonstracoes/balanco${qs ? `?${qs}` : ''}`);
}
