import { apiGet, apiPost, apiPostVoid } from "./client";

export interface TwoFactorStatus {
  ativo: boolean;
}

export interface TwoFactorSetup {
  secret: string;
  otpauthUrl: string;
}

export interface TwoFactorConfirmResposta {
  codigosRecuperacao: string[];
}

export function obterEstado2FA(): Promise<TwoFactorStatus> {
  return apiGet<TwoFactorStatus>("/api/2fa/status");
}

export function iniciarSetup2FA(): Promise<TwoFactorSetup> {
  return apiPost<TwoFactorSetup>("/api/2fa/setup", {});
}

export function confirmarSetup2FA(codigo: string): Promise<TwoFactorConfirmResposta> {
  return apiPost<TwoFactorConfirmResposta>("/api/2fa/confirmar", { codigo });
}

export function desativar2FA(password: string): Promise<void> {
  return apiPostVoid("/api/2fa/desativar", { password });
}
