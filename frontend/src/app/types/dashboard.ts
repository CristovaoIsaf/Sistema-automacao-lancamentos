export interface KPIDashboard {
  documentosImportados: number;
  sugestoesPendentes: number;
  lancamentosAprovados: number;
  precisaoIA: number;
}

export interface DadoGraficoMensal {
  mes: string;
  receitas: number;
  despesas: number;
}

export interface DocumentoRecente {
  id: string;
  nome: string;
  tipo: string;
  data: string;
  estado: string;
}

export interface DashboardDados {
  kpis: KPIDashboard;
  graficoMensal: DadoGraficoMensal[];
  documentosRecentes: DocumentoRecente[];
}
