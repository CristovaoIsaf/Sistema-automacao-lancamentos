import { apiPost } from './client';
import type { LoginResposta } from '../types/auth';

export interface CadastroRequest {
  empresa: string;
  nif: string;
  email: string;
  password: string;
}

export interface CadastroResponse {
  token: string;
  nome: string;
  email: string;
  papel: string;
}

export async function cadastro(dados: CadastroRequest): Promise<CadastroResponse> {
  return apiPost<CadastroResponse>('/auth/registo', dados);
}
