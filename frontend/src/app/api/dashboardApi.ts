import { apiGet } from './client';
import type { DashboardDados } from '../types/dashboard';
import type { DashboardAdministrador } from '../types/dashboardAdmin';
import type { Lancamento } from '../types/contabilidade';

export async function obterDashboard(): Promise<DashboardDados> {
  return apiGet<DashboardDados>('/api/dashboard');
}

// Fase 15 do plano de 20 fases — dados exclusivos da visão de
// Administrador (atividade por utilizador, pendências); o backend
// rejeita com 403 se o perfil autenticado não for ADMINISTRADOR.
export async function obterDashboardAdministrador(): Promise<DashboardAdministrador> {
  return apiGet<DashboardAdministrador>('/api/dashboard/administrador');
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
