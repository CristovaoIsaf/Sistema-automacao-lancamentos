import { apiPost } from './client';
import type { LoginResposta } from '../types/auth';

// Fase 20 do plano de 20 fases — POST /auth/registo, agora um endpoint
// real (ver AuthController.registar). Só disponível enquanto não existir
// nenhum utilizador nesta instalação — o backend devolve 409 nesse caso
// (ver registar() em Cadastro.tsx).
export interface CadastroRequest {
  nome: string;
  nif: string;
  email: string;
  senha: string;
  nomeEmpresa: string;
  nifEmpresa: string;
}

export async function registar(dados: CadastroRequest): Promise<LoginResposta> {
  return apiPost<LoginResposta>('/auth/registo', dados);
}
