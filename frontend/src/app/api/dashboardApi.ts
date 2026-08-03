import { apiGet } from './client';
import type { DashboardDados } from '../types/dashboard';
import type { Lancamento } from '../types/contabilidade';

export async function obterDashboard(): Promise<DashboardDados> {
  return apiGet<DashboardDados>('/api/dashboard');
}

export async function obterResumo(): Promise<DashboardDados['kpis']> {
  return apiGet<DashboardDados['kpis']>('/api/dashboard/resumo');
}

export async function obterGraficoMensal(meses?: number): Promise<DashboardDados['graficoMensal']> {
  const params = meses ? `?meses=${meses}` : '';
  return apiGet(`/api/dashboard/grafico-mensal${params}`);
}

export async function obterDocumentosRecentes(limite?: number): Promise<DashboardDados['documentosRecentes']> {
  const params = limite ? `?limite=${limite}` : '';
  return apiGet(`/api/dashboard/documentos-recentes${params}`);
}

export async function obterLancamentosRecentes(limite?: number): Promise<Lancamento[]> {
  const params = limite ? `?limite=${limite}` : '';
  return apiGet<Lancamento[]>(`/api/lancamentos/recentes${params}`);
}
