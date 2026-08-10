import { apiGet } from './client';
import type { EntidadeDossie, EntidadeResumo } from '../types/documento';

// Fase 10 do plano de 20 fases — "visão de dossiê da entidade".
export async function listarEntidades(): Promise<EntidadeResumo[]> {
  return apiGet<EntidadeResumo[]>('/api/entidades');
}

export async function obterDossieEntidade(id: number): Promise<EntidadeDossie> {
  return apiGet<EntidadeDossie>(`/api/entidades/${id}`);
}
