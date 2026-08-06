export interface ContaResumo {
  codigo: string;
  nome: string;
}

export interface CategoriaConta {
  nome: string;
  principais: ContaResumo[];
  ocasionais: ContaResumo[];
}
