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
  // totalAtivo - totalPassivo — este PGC-AO reduzido não modela Património
  // Líquido, por isso não fecha necessariamente a zero (ver backend).
  diferenca: number;
}
