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
  '/lancamentos': TODOS,
  '/lancamento-diario': ['ADMINISTRADOR', 'CONTABILISTA'],
  '/balancetes': TODOS,
  '/livro-razao': TODOS,
  '/livros-fiscais': TODOS,
  '/saft': ['ADMINISTRADOR', 'CONTABILISTA'],
  '/relatorios': TODOS,
  '/notas-contas': TODOS,
  '/ia-categorizacao': ['ADMINISTRADOR', 'CONTABILISTA'],
  '/plano-contas': ['ADMINISTRADOR'],
  // Fase 16 do plano de 20 fases ("auditor: utilizadores") — o backend já
  // permitia GET /api/utilizadores a AUDITOR (UserController), mas a rota
  // aqui ainda bloqueava o acesso à página. Utilizadores.tsx já esconde
  // "Novo Utilizador"/"Editar" com podeGerirUtilizadores (ADMINISTRADOR),
  // por isso alargar aqui dá visão sem dar escrita.
  '/utilizadores': ['ADMINISTRADOR', 'AUDITOR'],
  '/auditoria': ['ADMINISTRADOR', 'AUDITOR'],
  '/configuracoes': ['ADMINISTRADOR'],
  // Segurança da própria conta (2FA) — qualquer perfil autenticado gere a
  // sua própria, não é um privilégio de Administrador (ver
  // TwoFactorController.java, sem @PreAuthorize).
  '/minha-conta': TODOS,
};
