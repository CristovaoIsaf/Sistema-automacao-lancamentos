import { apiDelete, apiGet, apiPost, apiPut } from './client';
import type { Perfil, Utilizador } from '../types/contabilidade';

// Espelha UserResponseDTO/UserRequestDTO do backend — "papel"/"status" em vez
// de "perfil"/"ativo", e sem ultimoAcesso (não é rastreado ainda).
interface UtilizadorBackend {
  id: number;
  nome: string;
  email: string;
  nif: string;
  status: string;
  papel: Perfil;
}

export interface NovoUtilizadorRequest {
  nome: string;
  email: string;
  nif: string;
  senha: string;
  perfil: Perfil;
}

function converterParaUtilizador(dados: UtilizadorBackend): Utilizador {
  return {
    id: String(dados.id),
    nome: dados.nome,
    email: dados.email,
    nif: dados.nif,
    perfil: dados.papel,
    ativo: dados.status === 'ATIVO',
    ultimoAcesso: '—',
  };
}

export async function listarUtilizadores(): Promise<Utilizador[]> {
  const dados = await apiGet<UtilizadorBackend[]>('/api/utilizadores');
  return dados.map(converterParaUtilizador);
}

export async function obterUtilizador(id: string): Promise<Utilizador> {
  const dados = await apiGet<UtilizadorBackend>(`/api/utilizadores/${id}`);
  return converterParaUtilizador(dados);
}

export async function criarUtilizador(dados: NovoUtilizadorRequest): Promise<Utilizador> {
  const criado = await apiPost<UtilizadorBackend>('/api/utilizadores', {
    nome: dados.nome,
    email: dados.email,
    nif: dados.nif,
    senha: dados.senha,
    papel: dados.perfil,
  });
  return converterParaUtilizador(criado);
}

export async function atualizarUtilizador(
  id: string,
  dados: Partial<Pick<Utilizador, 'nome' | 'email' | 'nif' | 'perfil' | 'ativo'>>
): Promise<Utilizador> {
  const atualizado = await apiPut<UtilizadorBackend>(`/api/utilizadores/${id}`, {
    nome: dados.nome,
    email: dados.email,
    nif: dados.nif,
    papel: dados.perfil,
    status: dados.ativo === undefined ? undefined : (dados.ativo ? 'ATIVO' : 'INATIVO'),
  });
  return converterParaUtilizador(atualizado);
}

// Auditoria C09 — suspender/reativar sem reescrever os restantes campos:
// o backend (UserService.atualizar) sobrescreve nome/email/nif/papel a
// partir do corpo do pedido sem verificação de nulo, por isso este envia
// sempre os valores atuais do utilizador, só trocando o status.
export async function alternarEstadoUtilizador(utilizador: Utilizador): Promise<Utilizador> {
  return atualizarUtilizador(utilizador.id, {
    nome: utilizador.nome,
    email: utilizador.email,
    nif: utilizador.nif,
    perfil: utilizador.perfil,
    ativo: !utilizador.ativo,
  });
}

export async function apagarUtilizador(id: string): Promise<void> {
  await apiDelete(`/api/utilizadores/${id}`);
}
