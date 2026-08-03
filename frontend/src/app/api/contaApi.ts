import { apiGet, apiPost, apiPut } from './client';
import type { Conta } from '../types/contabilidade';

export async function listarContas(): Promise<Conta[]> {
  return apiGet<Conta[]>('/api/contas');
}

export async function obterConta(id: string): Promise<Conta> {
  return apiGet<Conta>(`/api/contas/${id}`);
}

export async function criarConta(dados: Omit<Conta, 'id'>): Promise<Conta> {
  return apiPost<Conta>('/api/contas', dados);
}

export async function atualizarConta(id: string, dados: Partial<Conta>): Promise<Conta> {
  return apiPut<Conta>(`/api/contas/${id}`, dados);
}

export async function toggleContaAtiva(id: string, ativa: boolean): Promise<void> {
  await apiPut(`/api/contas/${id}/ativa`, { ativa });
}
