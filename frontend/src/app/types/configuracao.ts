export interface ConfiguracaoGeral {
  nomeEmpresa: string;
  nif: string;
  morada: string;
  telefone: string;
  email: string;
  moeda: string;
  exercicioCorrente: string;
}

export interface ConfiguracaoNotificacoes {
  emailNotificacoes: boolean;
  notificarLancamentos: boolean;
  notificarAprovacoes: boolean;
  notificarDocumentos: boolean;
  notificarRelatorios: boolean;
}

export interface ConfiguracaoSeguranca {
  sessaoExpiraMinutos: number;
  maxTentativasLogin: number;
  exigirMaiuscula: boolean;
  exigirNumero: boolean;
  exigirSimbolo: boolean;
  comprimentoMinimo: number;
}

export interface ConfiguracaoIntegracao {
  apiUrl: string;
  jdbcUrl: string;
  ativo: boolean;
}

export interface ConfiguracaoCompleta {
  geral: ConfiguracaoGeral;
  notificacoes: ConfiguracaoNotificacoes;
  seguranca: ConfiguracaoSeguranca;
  integracao: ConfiguracaoIntegracao;
  apiKey: string;
}
