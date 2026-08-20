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

// Registo de auditoria (UC010, RF017) — Fase 15 do plano de 20 fases:
// perfil/dataHora podem vir null (utilizador entretanto apagado, ou
// lançamento anterior à Fase 15 sem timestamp — ver AuditoriaService).
export interface LogAuditoria {
  id: string;
  utilizador: string;
  perfil: Perfil | null;
  acao: string;
  entidade: string;
  dataHora: string | null;
  // Auditoria C06/C07 — só preenchidos para eventos vindos da tabela
  // AuditLog dedicada (login, gestão de utilizadores, empresa, rejeição
  // de sugestões); eventos derivados de lançamentos/documentos não têm.
  resultado?: string | null;
  motivo?: string | null;
  ip?: string | null;
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
