export interface EntradaLivroDiario {
  id: string;
  data: string;
  documento: string;
  descricao: string;
  contaDebito: string;
  contaCredito: string;
  valor: number;
}

export interface LivroDiario {
  periodo: string;
  entradas: EntradaLivroDiario[];
  totalDebito: number;
  totalCredito: number;
}

export interface EntradaLivroRazao {
  data: string;
  documento: string;
  historico: string;
  debito: number;
  credito: number;
  saldo: number;
}

export interface LivroRazao {
  conta: string;
  nomeConta: string;
  periodo: string;
  entradas: EntradaLivroRazao[];
  saldoFinal: number;
}

export interface EntradaIVA {
  mes: string;
  ivLiquidado: number;
  ivDedutivel: number;
  saldo: number;
}

export interface LivroIVA {
  periodo: string;
  entradas: EntradaIVA[];
  totalLiquidado: number;
  totalDedutivel: number;
  saldoFinal: number;
}

export type TipoLivro = 'diario' | 'razao' | 'iva';
export type FormatoExportacaoLivro = 'PDF' | 'EXCEL' | 'CSV';

export interface ExportarLivroParams {
  tipo: TipoLivro;
  formato: FormatoExportacaoLivro;
  periodo: string;
  conta?: string;
}
