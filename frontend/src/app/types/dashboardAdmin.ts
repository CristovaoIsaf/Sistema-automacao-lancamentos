// Fase 15 do plano de 20 fases — "o administrador deve possuir visão da
// empresa". Espelham AtividadeUtilizadorDTO/PendenciaDTO/
// DashboardAdministradorDTO (backend, ver DashboardAdministradorService).

export interface AtividadeUtilizador {
  utilizador: string;
  perfil?: string | null;
  totalAcoes: number;
  ultimaAcao?: string | null;
}

export interface Pendencia {
  sugestaoId: number;
  documentoId?: number | null;
  descricao: string;
  entidade?: string | null;
  valor: string;
  dataCriacao?: string | null;
}

export interface DashboardAdministrador {
  atividadePorUtilizador: AtividadeUtilizador[];
  pendencias: Pendencia[];
}
