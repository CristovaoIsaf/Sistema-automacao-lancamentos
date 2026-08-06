import { apiPost, apiPostVoid } from './client';
import type { Sugestao } from '../types/documento';
import type { LancamentoResponse, LinhaLancamento } from '../types/lancamento';

export async function analisarDocumento(documentoId: number): Promise<Sugestao> {
  return apiPost<Sugestao>('/analises', { documentoId });
}

// validadoPor já não é enviado pelo cliente — o backend resolve-o a partir
// do utilizador autenticado (JWT). `linhas` são as mesmas mostradas no
// ecrã de revisão (LancamentoDiario.tsx); `editado` distingue "aprovado tal
// como a IA sugeriu" de "contabilista alterou algo antes de aprovar".
export async function aprovarSugestao(
  sugestaoId: number,
  linhas: LinhaLancamento[],
  editado: boolean
): Promise<LancamentoResponse> {
  return apiPost<LancamentoResponse>(`/analises/${sugestaoId}/aprovar`, { linhas, editado });
}

export async function rejeitarSugestao(sugestaoId: number): Promise<void> {
  return apiPostVoid(`/analises/${sugestaoId}/rejeitar`);
}
