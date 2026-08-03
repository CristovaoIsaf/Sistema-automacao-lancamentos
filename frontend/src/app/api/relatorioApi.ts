import { apiGet } from './client';
import type {
  RelatorioDRE,
  RelatorioBalanco,
  RelatorioBalancete,
  RelatorioRazao,
  ExportarRelatorioParams
} from '../types/relatorio';

export async function obterDRE(periodo: string): Promise<RelatorioDRE> {
  return apiGet<RelatorioDRE>(`/api/relatorios/dre?periodo=${periodo}`);
}

export async function obterBalanco(periodo: string): Promise<RelatorioBalanco> {
  return apiGet<RelatorioBalanco>(`/api/relatorios/balanco?periodo=${periodo}`);
}

export async function obterBalancete(periodo: string): Promise<RelatorioBalancete> {
  return apiGet<RelatorioBalancete>(`/api/relatorios/balancete?periodo=${periodo}`);
}

export async function obterRazao(conta: string, periodo: string): Promise<RelatorioRazao> {
  return apiGet<RelatorioRazao>(`/api/relatorios/razao?conta=${conta}&periodo=${periodo}`);
}

export async function exportarRelatorio(params: ExportarRelatorioParams): Promise<Blob> {
  const searchParams = new URLSearchParams({
    formato: params.formato,
    tipo: params.tipo,
    periodo: params.periodo
  });
  if (params.inicio) searchParams.append('inicio', params.inicio);
  if (params.fim) searchParams.append('fim', params.fim);

  const response = await fetch(
    `${import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}/api/relatorios/exportar?${searchParams.toString()}`,
    { method: 'GET' }
  );

  if (!response.ok) {
    throw new Error(`Erro ${response.status}`);
  }

  return response.blob();
}
