import { apiGet } from './client';
import type { ContaResumo } from '../types/categoriaConta';

// Fase 6 do plano de 20 fases: só GET existe no backend (ContaController) —
// o plano de contas é definido em fastapi/app/services/pgc.py (fonte
// única), não é editável por aqui. Antes havia obterConta/criarConta/
// atualizarConta/toggleContaAtiva a apontar para endpoints que nunca
// existiram no ContaController real (herdados do modelo Conta genérico
// de mockData.ts, já removido) — nenhum tinha utilização real.
export async function listarContas(): Promise<ContaResumo[]> {
  return apiGet<ContaResumo[]>('/api/contas');
}
