// Fase 13 do plano de 20 fases — espelham DREResponseDTO/BalancoResponseDTO
// (backend, ver DemonstracoesFinanceirasService) exatamente como devolvidos
// pela API; substituem tipos antigos (SecaoDRE/SecaoBalanco com
// valorAtual/valorAnterior) que nunca tiveram nenhum backend real a
// suportá-los.

export interface LinhaDemonstracao {
  conta: string;
  nome: string;
  valor: number;
}

export interface RelatorioDRE {
  inicio?: string | null;
  fim?: string | null;
  receitas: LinhaDemonstracao[];
  totalReceitas: number;
  gastos: LinhaDemonstracao[];
  totalGastos: number;
  resultadoLiquido: number;
}

export interface RelatorioBalanco {
  inicio?: string | null;
  fim?: string | null;
  ativo: LinhaDemonstracao[];
  totalAtivo: number;
  passivo: LinhaDemonstracao[];
  totalPassivo: number;
  // Fase 19 do plano de 20 fases — Capital e Reservas (classe 5, Decreto
  // 82/01) + uma linha sintética "88 — Resultado do Exercício" (resultado
  // líquido do mesmo período, reaproveitado da DRE — nunca uma conta
  // lançada, ver backend).
  capitalProprio: LinhaDemonstracao[];
  totalCapitalProprio: number;
  // totalAtivo - (totalPassivo + totalCapitalProprio) — este PGC-AO
  // reduzido ainda não modela Ativo Não Corrente, por isso pode não
  // fechar exatamente a zero (ver backend).
  diferenca: number;
}

// Fase 17 do plano de 20 fases — espelha MovimentoCaixaDTO/
// FluxoCaixaResponseDTO (backend, ver FluxoCaixaService). Método direto,
// sem classificação operacional/investimento/financiamento — ver decisão
// documentada no backend.
export interface MovimentoCaixa {
  lancamentoId: number;
  data?: string | null;
  descricao?: string | null;
  conta: string;
  nomeConta: string;
  contraConta?: string | null;
  tipo: 'ENTRADA' | 'SAIDA';
  valor: number;
}

export interface RelatorioFluxoCaixa {
  inicio?: string | null;
  fim?: string | null;
  movimentos: MovimentoCaixa[];
  totalEntradas: number;
  totalSaidas: number;
  saldoPeriodo: number;
}
