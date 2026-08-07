import { apiGet, apiGetBlob, getToken } from './client';
import type { Documento, UploadDocumentoResponse } from '../types/documento';

const API_BASE =
  (import.meta as any).env?.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function listarDocumentos(): Promise<Documento[]> {
  return apiGet<Documento[]>('/documentos');
}

export async function uploadDocumento(file: File): Promise<UploadDocumentoResponse> {
  const formData = new FormData();
  formData.append('file', file);

  const token = getToken();
  const response = await fetch(`${API_BASE}/documentos`, {
    method: 'POST',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: formData
  });

  if (!response.ok) {
    // 409 (documento duplicado) e outros erros do backend vêm com
    // { message: "..." } — sem isto, o utilizador só via "Erro 409"
    // genérico em vez de saber que o documento já tinha sido carregado.
    const mensagem = await response.json().then(body => body?.message).catch(() => null);
    throw new Error(mensagem || `Erro ${response.status}`);
  }

  return response.json();
}

function descarregarBlob(blob: Blob, nomeFicheiro: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = nomeFicheiro;
  link.click();
  URL.revokeObjectURL(url);
}

// Abre o ficheiro original numa nova aba (pré-visualização de PDF/imagem).
export async function abrirDocumento(id: number): Promise<void> {
  const blob = await apiGetBlob(`/documentos/${id}/download`);
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank');
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

export async function descarregarDocumento(id: number, nomeFicheiro: string): Promise<void> {
  const blob = await apiGetBlob(`/documentos/${id}/download`);
  descarregarBlob(blob, nomeFicheiro);
}

// Arquivo organizado por entidade (Cliente/Fornecedor) em .zip, com manifesto.csv.
export async function exportarZipDocumentos(inicio?: string, fim?: string): Promise<void> {
  const params = new URLSearchParams();
  if (inicio) params.append('inicio', inicio);
  if (fim) params.append('fim', fim);
  const qs = params.toString();
  const blob = await apiGetBlob(`/documentos/export-zip${qs ? `?${qs}` : ''}`);
  descarregarBlob(blob, 'documentos.zip');
}
