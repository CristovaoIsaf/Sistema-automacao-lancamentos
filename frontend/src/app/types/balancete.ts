export interface BalanceteLinha {
  conta: string;
  nome: string;
  debitoAcumulado: number;
  creditoAcumulado: number;
  saldoDevedor: number;
  saldoCredor: number;
}

export interface BalanceteResponse {
  linhas: BalanceteLinha[];
  totalDebito: number;
  totalCredito: number;
  totalSaldoDevedor: number;
  totalSaldoCredor: number;
  equilibrado: boolean;
}
