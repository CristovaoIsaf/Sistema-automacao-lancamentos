import { apiGet } from './client';
import type { ItemBalancete } from '../types/contabilidade';

export interface BalanceteItemExtendido extends ItemBalancete {
  saldoAnterior: number;
  acumuladoDebito: number;
  acumuladoCredito: number;
  saldoAcumulado: number;
}

export async function obterBalancete(
  periodo: string,
  inicio?: string,
  fim?: string
): Promise<BalanceteItemExtendido[]> {
  const params = new URLSearchParams({ periodo });
  if (inicio) params.append('inicio', inicio);
  if (fim) params.append('fim', fim);
  return apiGet<BalanceteItemExtendido[]>(`/api/balancete?${params.toString()}`);
}
