import { apiGet, apiPost, apiPut } from './client';
import type { CategoriaIA } from '../types/contabilidade';

export async function listarCategoriasIA(): Promise<CategoriaIA[]> {
  return apiGet<CategoriaIA[]>('/api/ia/categorias');
}

export async function criarCategoriaIA(dados: Omit<CategoriaIA, 'id'>): Promise<CategoriaIA> {
  return apiPost<CategoriaIA>('/api/ia/categorias', dados);
}

export async function atualizarCategoriaIA(id: string, dados: Partial<CategoriaIA>): Promise<CategoriaIA> {
  return apiPut<CategoriaIA>(`/api/ia/categorias/${id}`, dados);
}

export async function testarCategorizacao(descricao: string): Promise<{
  categoria: string;
  confianca: number;
  contaDebito: string;
  contaCredito: string;
}> {
  return apiPost('/api/ia/testar', { descricao });
}

export async function obterMetricasIA(): Promise<{
  precisao: number;
  documentosCategorizados: number;
  categoriasAtivas: number;
  tempoMedio: number;
}> {
  return apiGet('/api/ia/metricas');
}
