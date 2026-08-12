// Tipos alinhados com os DTOs/entidades reais do backend
// (DocumentoController + AnaliseController).

export interface Documento {
  id: number;
  nomeFicheiro: string;
  tipoConteudo: string;
  dataUpload: string;
  entidadeId?: number | null;
  entidadeNome?: string | null;
  // Fase 10 do plano de 20 fases ("pesquisa: entidade; NIF; número; série;
  // tipo; data; valor") — vindos da Sugestao mais recente ligada a este
  // documento (podem ser null se ainda não foi analisado).
  entidadeNif?: string | null;
  tipoDocumento?: string | null;
  numeroDocumento?: string | null;
  valor?: string | null;
  tamanho?: number;
  estado?: 'Pendente' | 'Analisado' | 'Aprovado' | 'Rejeitado';
  // Fase 16 do plano de 20 fases ("auditor: inconsistências") — mesmo JSON
  // de Sugestao.validacaoJson, usar parseValidacao (ValidacaoDocumento.tsx).
  // null se o documento ainda não foi analisado ou é anterior à Fase 3.
  validacaoJson?: string | null;
}

// Espelha Entidade (backend) — Fase 10, GET /api/entidades.
export interface EntidadeResumo {
  id: number;
  nome: string;
  nif: string;
  tipo: 'CLIENTE' | 'FORNECEDOR' | 'DESCONHECIDO';
}

// Espelha PerfilEntidadeDTO (backend) — Fase 12, conhecimento acumulado
// sobre o comportamento habitual de uma entidade (ver
// fastapi/app/services/entity_profile.py). Pode vir null no dossiê
// quando o FastAPI está indisponível ou ainda não há histórico.
export interface PerfilEntidade {
  nif: string;
  totalDocumentos: number;
  tipoDominante: string | null;
  distribuicaoTiposDocumento: Record<string, number>;
}

// Espelha EntidadeDossieDTO (backend) — GET /api/entidades/{id}.
export interface EntidadeDossie {
  id: number;
  nome: string;
  nif: string;
  tipo: 'CLIENTE' | 'FORNECEDOR' | 'DESCONHECIDO';
  documentos: Documento[];
  perfil?: PerfilEntidade | null;
}

export interface UploadDocumentoResponse {
  id: number;
  nomeFicheiro: string;
  tipoConteudo: string;
  dataUpload: string;
}

export interface Sugestao {
  id: number;
  documentoId?: number;
  tipoDocumento: string;
  categoriaContabil: string;
  categoria?: string | null;
  valor: string;
  // Série+número da fatura (ex. "FT 2026/001") — ver Sugestao.numeroDocumento
  // no backend. Corrigível via PATCH /documentos/{id}/classificacao.
  numeroDocumento?: string | null;
  entidade?: string | null;
  nif?: string | null;
  descricao: string;
  // Lista de LinhaSugeridaDTO (conta/nome/debito/credito) serializada em
  // JSON — usar JSON.parse antes de mostrar/editar como linhas de
  // lançamento (ver LancamentoDiario.tsx).
  linhasJson?: string | null;
  fundamentacao?: string | null;
  // Confiança (0-100) da classificação — Fase 8 do plano de 20 fases:
  // já era calculada pelo FastAPI mas nunca tinha sido persistida nem
  // exposta ao frontend. null para sugestões antigas.
  confianca?: number | null;
  // Contexto usado nesta classificação (Fase 3 — Context Engine),
  // serializado em JSON. Usar parseContexto
  // (components/ContextoIdentificado.tsx) antes de mostrar — null para
  // sugestões antigas, gravadas antes dessa fase.
  contextoJson?: string | null;
  // Resultado do motor de validação determinística (Fase 3 — ver
  // fastapi/app/services/document_validation.py), serializado em JSON.
  // Usar parseValidacao (components/ValidacaoDocumento.tsx) antes de
  // mostrar — null para sugestões antigas, gravadas antes desta fase.
  validacaoJson?: string | null;
  // Fase 4 do plano de 20 fases — "contextualização assistida", serializada
  // em JSON. Usar parsePerguntaContextualizacao
  // (components/ContextualizacaoAssistida.tsx) — null quando a
  // classificação já foi confiante e não houve pergunta nenhuma a fazer.
  perguntaContextualizacaoJson?: string | null;
  lancamentoId?: number | null;
  estado: string;
}

export interface ProblemaValidacao {
  campo: string;
  codigo: string;
  mensagem: string;
  gravidade: 'erro' | 'aviso';
}

export interface ResultadoValidacao {
  valido: boolean;
  versao: string;
  problemas: ProblemaValidacao[];
}

// Espelha LinhaSugeridaDTO (backend) — mesma forma usada em Sugestao.linhasJson.
export interface LinhaSugeridaContexto {
  conta: string;
  nome?: string;
  debito?: string | null;
  credito?: string | null;
  descricao?: string;
}

export interface OpcaoContextualizacao {
  valor: string;
  rotulo: string;
  tipo: string;
  categoria?: string | null;
  linhas: LinhaSugeridaContexto[];
}

export interface PerguntaContextualizacao {
  pergunta: string;
  opcoes: OpcaoContextualizacao[];
}

// Espelha ContextoClassificacaoDTO (backend, Fase 3 — Context Engine).
export interface ContextoClassificacao {
  versao: string;
  empresaAtividadeEconomica?: string | null;
  empresaNaturezaNegocio?: string | null;
  empresaMoeda?: string | null;
  exercicioAtualInicio?: string | null;
  exercicioAtualFim?: string | null;
  entidadeId?: number | null;
  entidadeNome?: string | null;
  entidadeNif?: string | null;
  entidadeTipo?: string | null;
  historicoTiposOperacaoRecentes: string[];
}
