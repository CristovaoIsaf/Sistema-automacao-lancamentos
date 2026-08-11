// Fase 18 do plano de 20 fases — espelha MovimentoRazaoDTO/
// LivroRazaoResponseDTO (backend, ver LivroRazaoService). saldoAcumulado
// começa sempre em zero no início do intervalo pedido — este sistema
// ainda não tem períodos contabilísticos fechados (mesma simplificação
// já assumida no Balancete).
export interface MovimentoRazao {
  lancamentoId: number;
  data?: string | null;
  descricao?: string | null;
  debito?: number | null;
  credito?: number | null;
  saldoAcumulado: number;
}

export interface LivroRazaoResponse {
  conta: string;
  nomeConta: string;
  inicio?: string | null;
  fim?: string | null;
  movimentos: MovimentoRazao[];
  totalDebito: number;
  totalCredito: number;
  saldoFinal: number;
}
