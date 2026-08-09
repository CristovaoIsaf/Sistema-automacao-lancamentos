import type { Perfil } from '../types/contabilidade';

export const TODOS: Perfil[] = ['ADMINISTRADOR', 'CONTABILISTA', 'AUDITOR'];

// Fase 1 do plano de 20 fases — fonte única de verdade de que perfis podem
// aceder a cada rota. Usada tanto para filtrar o menu lateral
// (components/Layout.tsx) como para bloquear a navegação directa por URL
// (RotaProtegida.tsx). Antes desta fase só o menu escondia o link — digitar
// o URL directamente dava acesso a qualquer perfil autenticado a qualquer
// página, incluindo /utilizadores e /configuracoes.
//
// As restrições espelham as mesmas regras já documentadas nos comentários
// de Layout.tsx (RN010, UC004, UC010) — não inventadas de novo aqui.
export const PERFIS_POR_ROTA: Record<string, Perfil[]> = {
  '/': TODOS,
  '/upload-documentos': ['ADMINISTRADOR', 'CONTABILISTA'],
  '/documentos': TODOS,
  // Página órfã (sem link no menu), mesma regra de escrita que as outras
  // páginas de revisão de sugestões da IA.
  '/classificacao': ['ADMINISTRADOR', 'CONTABILISTA'],
  '/lancamentos': TODOS,
  '/lancamento-diario': ['ADMINISTRADOR', 'CONTABILISTA'],
  '/balancetes': TODOS,
  '/livros-fiscais': TODOS,
  '/saft': ['ADMINISTRADOR', 'CONTABILISTA'],
  '/relatorios': TODOS,
  '/ia-categorizacao': ['ADMINISTRADOR', 'CONTABILISTA'],
  '/plano-contas': ['ADMINISTRADOR'],
  '/utilizadores': ['ADMINISTRADOR'],
  '/auditoria': ['ADMINISTRADOR', 'AUDITOR'],
  '/configuracoes': ['ADMINISTRADOR'],
};
