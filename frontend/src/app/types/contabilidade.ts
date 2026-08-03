// Tipos e interfaces do sistema contábil

// Perfis de acesso (RBAC) — o TFC define exactamente 3 (Administrador, Contabilista, Auditor)
export type Perfil = 'ADMINISTRADOR' | 'CONTABILISTA' | 'AUDITOR';

export interface Utilizador {
  id: string;
  nome: string;
  email: string;
  perfil: Perfil;
  nif: string;
  ativo: boolean;
  ultimoAcesso: string;
}

// Registo de auditoria (UC010, RF017)
export interface LogAuditoria {
  id: string;
  utilizador: string;
  perfil: Perfil;
  acao: string;
  entidade: string;
  dataHora: string;
}

export interface Conta {
  id: string;
  codigo: string;
  nome: string;
  tipo: 'ATIVO' | 'PASSIVO' | 'RECEITA' | 'DESPESA' | 'PATRIMONIO_LIQUIDO';
  natureza: 'DEVEDORA' | 'CREDORA';
  nivel: number;
  contaPai?: string;
  ativa: boolean;
}

export interface Lancamento {
  id: string;
  data: string;
  descricao: string;
  valor: number;
  contaDebito: string;
  contaDebitoNome: string;
  contaCredito: string;
  contaCreditoNome: string;
  documento?: string;
  historico: string;
  status: 'PENDENTE' | 'APROVADO' | 'REJEITADO';
  categoriaIA?: string;
  confiancaIA?: number;
  criadoPor: string;
  criadoEm: string;
}

export interface ResumoFinanceiro {
  totalAtivos: number;
  totalPassivos: number;
  totalReceitas: number;
  totalDespesas: number;
  resultado: number;
}

export interface ItemBalancete {
  conta: string;
  codigo: string;
  debito: number;
  credito: number;
  saldo: number;
}

export interface CategoriaIA {
  id: string;
  nome: string;
  palavrasChave: string[];
  contaDebito: string;
  contaCredito: string;
  confianca: number;
  usosRecentes: number;
}
