// Tipos alinhados com os DTOs/entidades reais do backend
// (DocumentoController + AnaliseController).

export interface Documento {
  id: number;
  nomeFicheiro: string;
  tipoConteudo: string;
  dataUpload: string;
  entidadeId?: number | null;
  entidadeNome?: string | null;
  tamanho?: number;
  estado?: 'Pendente' | 'Analisado' | 'Aprovado' | 'Rejeitado';
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
  entidade?: string | null;
  nif?: string | null;
  descricao: string;
  // Lista de LinhaSugeridaDTO (conta/nome/debito/credito) serializada em
  // JSON — usar JSON.parse antes de mostrar/editar como linhas de
  // lançamento (ver LancamentoDiario.tsx).
  linhasJson?: string | null;
  fundamentacao?: string | null;
  // Resultado do motor de validação determinística (Fase 3 — ver
  // fastapi/app/services/document_validation.py), serializado em JSON.
  // Usar parseValidacao (components/ValidacaoDocumento.tsx) antes de
  // mostrar — null para sugestões antigas, gravadas antes desta fase.
  validacaoJson?: string | null;
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
