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
  tipoDocumento: string;
  categoriaContabil: string;
  valor: string;
  descricao: string;
  estado: string;
}
