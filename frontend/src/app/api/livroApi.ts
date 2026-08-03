import { apiGet } from './client';
import type {
  LivroDiario,
  LivroRazao,
  LivroIVA,
  ExportarLivroParams
} from '../types/livro';

export async function obterLivroDiario(periodo: string): Promise<LivroDiario> {
  return apiGet<LivroDiario>(`/api/livros/diario?periodo=${periodo}`);
}

export async function obterLivroRazao(conta: string, periodo: string): Promise<LivroRazao> {
  return apiGet<LivroRazao>(`/api/livros/razao?conta=${conta}&periodo=${periodo}`);
}

export async function obterLivroIVA(periodo: string): Promise<LivroIVA> {
  return apiGet<LivroIVA>(`/api/livros/iva?periodo=${periodo}`);
}

export async function exportarLivro(params: ExportarLivroParams): Promise<Blob> {
  const searchParams = new URLSearchParams({
    tipo: params.tipo,
    formato: params.formato,
    periodo: params.periodo
  });
  if (params.conta) searchParams.append('conta', params.conta);

  const response = await fetch(
    `${import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}/api/livros/exportar?${searchParams.toString()}`,
    { method: 'GET' }
  );

  if (!response.ok) {
    throw new Error(`Erro ${response.status}`);
  }

  return response.blob();
}
