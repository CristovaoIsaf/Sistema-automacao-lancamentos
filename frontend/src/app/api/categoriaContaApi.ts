import { apiGet } from './client';
import type { CategoriaConta } from '../types/categoriaConta';

export async function listarCategoriasConta(): Promise<CategoriaConta[]> {
  return apiGet<CategoriaConta[]>('/api/categorias-conta');
}
