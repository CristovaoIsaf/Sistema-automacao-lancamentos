export interface ExportacaoSAFT {
  id: string;
  dataGeracao: string;
  periodoInicio: string;
  periodoFim: string;
  estado: 'GERADO' | 'ENVIADO' | 'ERRO';
  utilizador: string;
  tamanhoFicheiro: number;
  hashSHA256: string;
  urlDownload: string;
}

export interface GerarSAFTRequest {
  inicio: string;
  fim: string;
  incluir: string[];
}

export interface GerarSAFTResponse {
  id: string;
  estado: string;
  mensagem: string;
}
