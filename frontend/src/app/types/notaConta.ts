// Fase 14 do plano de 20 fases — "Notas às Contas". Espelham
// NotaContaResponseDTO/GrupoEntidadeDTO/MovimentoContaDTO/RedacaoNotaDTO
// (backend, ver NotaContaService/RedacaoNotaClient).

export interface MovimentoConta {
  lancamentoId: number;
  data: string;
  descricao: string;
  documentoId?: number | null;
  documentoNome?: string | null;
  debito?: number | null;
  credito?: number | null;
}

export interface GrupoEntidade {
  entidade: string;
  // CLIENTE / FORNECEDOR / DESCONHECIDO / null (lançamento manual, sem documento de origem)
  tipo?: string | null;
  movimentos: MovimentoConta[];
  subtotalDebito: number;
  subtotalCredito: number;
}

export interface NotaConta {
  conta: string;
  nomeConta?: string | null;
  natureza?: 'DEVEDORA' | 'CREDORA' | null;
  inicio?: string | null;
  fim?: string | null;
  porEntidade: GrupoEntidade[];
  totalDebito: number;
  totalCredito: number;
  saldo: number;
}

export interface RedacaoNota {
  texto: string;
  // "ia" | "template" — o contabilista deve saber se está a rever um
  // rascunho gerado por IA (mais atenção) ou puramente determinístico.
  fonte: 'ia' | 'template';
}
