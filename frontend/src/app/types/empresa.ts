// Espelha EmpresaDTO (backend/.../dto/EmpresaDTO.java).
//
// Fase 2 do plano de 20 fases — "contexto contabilístico da empresa":
// distinção FACTO vs CONTEXTO documentada em model/Empresa.java do
// backend. atividadeEconomica/naturezaNegocio são CONTEXTO (descrição
// qualitativa do negócio, usada para desambiguar classificações futuras);
// moeda/exercicioAtualInicio/exercicioAtualFim são FACTO. Nunca
// preenchido pela IA — só editável por um Administrador.
export interface Empresa {
  id: number;
  nome: string;
  nif: string;
  email: string;
  endereco: string;
  telefone: string;
  atividadeEconomica?: string | null;
  naturezaNegocio?: string | null;
  moeda?: string | null;
  exercicioAtualInicio?: string | null; // ISO date (yyyy-MM-dd)
  exercicioAtualFim?: string | null;
}
