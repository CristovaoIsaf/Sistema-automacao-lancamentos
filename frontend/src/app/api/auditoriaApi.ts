import { apiGet } from './client';
import type { LogAuditoria } from '../types/contabilidade';

export async function listarLogsAuditoria(
  page?: number,
  limite?: number
): Promise<{ itens: LogAuditoria[]; total: number }> {
  const params = new URLSearchParams();
  if (page !== undefined) params.append('page', String(page));
  if (limite !== undefined) params.append('limite', String(limite));
  const query = params.toString();
  return apiGet(`/api/auditoria/logs${query ? `?${query}` : ''}`);
}

export async function obterLogAuditoria(id: string): Promise<LogAuditoria> {
  return apiGet<LogAuditoria>(`/api/auditoria/logs/${id}`);
}
