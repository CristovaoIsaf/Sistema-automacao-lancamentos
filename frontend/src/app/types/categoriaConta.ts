// Fase 6 do plano de 20 fases: espelha o shape real devolvido por GET
// /api/contas (backend) / GET /pgc/contas (FastAPI, fonte única do plano
// de contas — ver fastapi/app/services/pgc.py `plano_de_contas()`).
// classe/subconta/natureza podem vir null consoante a conta.
export interface ContaResumo {
  codigo: string;
  nome: string;
  classe?: string | null;
  subconta?: string | null;
  natureza?: 'DEVEDORA' | 'CREDORA' | null;
}

export interface CategoriaConta {
  nome: string;
  principais: ContaResumo[];
  ocasionais: ContaResumo[];
}
