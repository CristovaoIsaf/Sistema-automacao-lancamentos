import { apiGet, apiPut } from './client';
import type {
  ConfiguracaoCompleta,
  ConfiguracaoGeral,
  ConfiguracaoNotificacoes,
  ConfiguracaoSeguranca,
  ConfiguracaoIntegracao
} from '../types/configuracao';

export async function obterConfiguracoes(): Promise<ConfiguracaoCompleta> {
  return apiGet<ConfiguracaoCompleta>('/api/configuracoes');
}

export async function atualizarConfiguracoesGeral(dados: ConfiguracaoGeral): Promise<void> {
  await apiPut('/api/configuracoes/geral', dados);
}

export async function atualizarConfiguracoesNotificacoes(dados: ConfiguracaoNotificacoes): Promise<void> {
  await apiPut('/api/configuracoes/notificacoes', dados);
}

export async function atualizarConfiguracoesSeguranca(dados: ConfiguracaoSeguranca): Promise<void> {
  await apiPut('/api/configuracoes/seguranca', dados);
}

export async function atualizarConfiguracoesIntegracao(dados: ConfiguracaoIntegracao): Promise<void> {
  await apiPut('/api/configuracoes/integracao', dados);
}

export async function obterApiKey(): Promise<string> {
  const resp = await apiGet<{ apiKey: string }>('/api/configuracoes/apikey');
  return resp.apiKey;
}
