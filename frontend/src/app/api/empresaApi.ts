import { apiGet, apiPut } from './client';
import type { Empresa } from '../types/empresa';

export async function obterEmpresa(): Promise<Empresa> {
  return apiGet<Empresa>('/api/empresa');
}

// PUT /api/empresa exige papel ADMINISTRADOR no backend (ver
// EmpresaController) — a rota /configuracoes já é só-Administrador (ver
// auth/permissoesRotas.ts), por isso não há verificação extra aqui.
export async function atualizarEmpresa(dados: Empresa): Promise<Empresa> {
  return apiPut<Empresa>('/api/empresa', dados);
}
