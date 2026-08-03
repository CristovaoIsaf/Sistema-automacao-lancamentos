export interface LinhaDRE {
  conta: string;
  codigo: string;
  descricao: string;
  valorAtual: number;
  valorAnterior: number;
}

export interface SecaoDRE {
  titulo: string;
  linhas: LinhaDRE[];
  subtotal: number;
}

export interface RelatorioDRE {
  periodo: string;
  sec: SecaoDRE[];
  resultadoLiquido: number;
}

export interface LinhaBalanco {
  conta: string;
  codigo: string;
  descricao: string;
  valorAtual: number;
  valorAnterior: number;
}

export interface SecaoBalanco {
  titulo: string;
  linhas: LinhaBalanco[];
  subtotal: number;
}

export interface RelatorioBalanco {
  periodo: string;
  ativo: SecaoBalanco[];
  passivo: SecaoBalanco[];
  patrimonioLiquido: SecaoBalanco[];
}

export interface ItemRelatorioBalancete {
  conta: string;
  codigo: string;
  nome: string;
  debito: number;
  credito: number;
  saldoDevedor: number;
  saldoCredor: number;
}

export interface RelatorioBalancete {
  periodo: string;
  itens: ItemRelatorioBalancete[];
  totalDebito: number;
  totalCredito: number;
}

export interface ItemLivroRazao {
  data: string;
  documento: string;
  historico: string;
 debito: number;
  credito: number;
  saldo: number;
}

export interface RelatorioRazao {
  conta: string;
  nomeConta: string;
  periodo: string;
  itens: ItemLivroRazao[];
  saldoFinal: number;
}

export type FormatoExportacao = 'PDF' | 'EXCEL' | 'CSV';
export type TipoRelatorio = 'dre' | 'balanco' | 'balancete' | 'razao';

export interface ExportarRelatorioParams {
  formato: FormatoExportacao;
  tipo: TipoRelatorio;
  periodo: string;
  inicio?: string;
  fim?: string;
}
