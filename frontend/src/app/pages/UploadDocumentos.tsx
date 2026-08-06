import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { Upload, FileText, CheckCircle2, AlertCircle, Sparkles, Loader2, ArrowRight } from 'lucide-react';
import { toast } from 'sonner';
import { uploadDocumento } from '../api/documentoApi';
import { analisarDocumento } from '../api/sugestaoApi';
import type { Sugestao } from '../types/documento';

export function UploadDocumentos() {
  const navigate = useNavigate();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [documentId, setDocumentId] = useState<number | null>(null);
  const [analise, setAnalise] = useState<Sugestao | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reset = () => {
    setAnalise(null);
    setError(null);
  };

  const handleFileChange = (file: File | null) => {
    reset();
    setSelectedFile(file);
  };

  const uploadFile = async () => {
    if (!selectedFile) {
      toast.error('Escolha um ficheiro primeiro');
      return;
    }

    try {
      setUploading(true);
      setError(null);

      const data = await uploadDocumento(selectedFile);
      setDocumentId(data.id);
      toast.success(`Documento enviado com ID ${data.id}`);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro inesperado no upload';
      setError(message);
      toast.error(message);
    } finally {
      setUploading(false);
    }
  };

  const analisarDocumentoAtual = async () => {
    if (!documentId) {
      toast.error('Faça primeiro o upload do documento');
      return;
    }

    try {
      setAnalyzing(true);
      setError(null);

      const data = await analisarDocumento(documentId);
      setAnalise(data);
      toast.success('Análise concluída');
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro inesperado na análise';
      setError(message);
      toast.error(message);
    } finally {
      setAnalyzing(false);
    }
  };

  const reverEAprovar = () => {
    if (!analise) return;
    navigate('/lancamento-diario', { state: { sugestao: analise } });
  };

  return (
    <div className="w-full max-w-[1400px] mx-auto space-y-4 px-2 sm:px-4 lg:px-6">
      <div className="px-1 sm:px-0">
        <h1 className="text-[18px] sm:text-[20px] font-semibold text-[#0F172A]">Teste ponta a ponta da análise contabilística</h1>
        <p className="text-[13px] text-[#475569] mt-0.5">Upload · Análise · Aprovação</p>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] gap-4">
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-3 sm:p-4">
          <div
            className={`relative flex flex-col items-center justify-center min-h-[260px] rounded-md transition-colors ${
              dragging ? 'bg-[#EFF6FF] border-2 border-dashed border-[#2563EB]' : 'bg-[#F8FAFC] border-2 border-dashed border-[#E2E8F0]'
            }`}
            onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
            onDragLeave={() => setDragging(false)}
            onDrop={(e) => {
              e.preventDefault();
              setDragging(false);
              if (e.dataTransfer.files?.[0]) handleFileChange(e.dataTransfer.files[0]);
            }}
          >
            <Upload className="w-8 h-8 text-[#94A3B8] mx-auto mb-3" />
            <p className="text-[13px] text-[#475569] font-medium mb-1">Arraste o documento aqui</p>
            <p className="text-[12px] text-[#94A3B8] mb-4">PDF, PNG, JPG</p>
            <label className="cursor-pointer">
              <input
                type="file"
                className="hidden"
                accept=".pdf,.png,.jpg,.jpeg"
                onChange={(e) => handleFileChange(e.target.files?.[0] ?? null)}
              />
              <span className="h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-[13px] font-medium rounded-md inline-flex items-center cursor-pointer">
                Seleccionar ficheiro
              </span>
            </label>
            {selectedFile && <p className="mt-3 text-[12px] text-[#0F172A]">Ficheiro: {selectedFile.name}</p>}
          </div>

          <div className="mt-4 flex flex-wrap gap-2">
            <button onClick={uploadFile} disabled={uploading || !selectedFile} className="w-full sm:w-auto flex-1 min-w-[120px] rounded-md bg-[#2563EB] px-3 py-2 text-white text-sm disabled:opacity-50">
              {uploading ? <span className="inline-flex items-center justify-center gap-2"><Loader2 className="animate-spin" size={14} />A carregar...</span> : '1. Upload'}
            </button>
            <button onClick={analisarDocumentoAtual} disabled={analyzing || !documentId} className="w-full sm:w-auto flex-1 min-w-[120px] rounded-md bg-[#7C3AED] px-3 py-2 text-white text-sm disabled:opacity-50">
              {analyzing ? <span className="inline-flex items-center justify-center gap-2"><Loader2 className="animate-spin" size={14} />A analisar...</span> : '2. Analisar'}
            </button>
            <button onClick={reverEAprovar} disabled={!analise?.id} className="w-full sm:w-auto flex-1 min-w-[160px] inline-flex items-center justify-center gap-1.5 rounded-md bg-[#059669] px-3 py-2 text-white text-sm disabled:opacity-50">
              3. Rever e Aprovar <ArrowRight size={14} />
            </button>
          </div>

          {error && (
            <div className="mt-4 rounded-md border border-[#FECACA] bg-[#FEF2F2] p-3 text-sm text-[#B91C1C]">
              <div className="flex items-start gap-2">
                <AlertCircle size={16} className="mt-0.5" />
                <span>{error}</span>
              </div>
            </div>
          )}

          <div className="mt-4">
            <Link
              to="/documentos"
              className="inline-flex items-center gap-1.5 text-[13px] font-medium text-[#2563EB] hover:text-[#1D4ED8]"
            >
              Ver arquivo completo de documentos
              <ArrowRight size={14} />
            </Link>
          </div>
        </div>

        <div className="space-y-4 min-w-0">
          <div className="bg-white border border-[#E2E8F0] rounded-lg p-3 sm:p-4">
            <div className="flex items-center gap-2 mb-3">
              <FileText size={16} className="text-[#2563EB]" />
              <h2 className="text-[13px] font-semibold text-[#0F172A]">Estado do fluxo</h2>
            </div>
            <div className="space-y-2 text-sm text-[#475569]">
              <div>Documento ID: <span className="font-semibold text-[#0F172A]">{documentId ?? '—'}</span></div>
              <div>Análise ID: <span className="font-semibold text-[#0F172A]">{analise?.id ?? '—'}</span></div>
            </div>
          </div>

          <div className="bg-white border border-[#E2E8F0] rounded-lg p-4">
            <div className="flex items-center gap-2 mb-3">
              <Sparkles size={16} className="text-[#7C3AED]" />
              <h2 className="text-[13px] font-semibold text-[#0F172A]">Resultado da análise</h2>
            </div>
            {analise ? (
              <div className="space-y-2 text-sm text-[#475569]">
                <div><span className="font-semibold text-[#0F172A]">Tipo:</span> {analise.tipoDocumento}</div>
                {analise.categoria && (
                  <div><span className="font-semibold text-[#0F172A]">Categoria:</span> {analise.categoria}</div>
                )}
                <div><span className="font-semibold text-[#0F172A]">Conta principal:</span> {analise.categoriaContabil}</div>
                <div><span className="font-semibold text-[#0F172A]">Valor:</span> {analise.valor}</div>
                <div><span className="font-semibold text-[#0F172A]">Descrição:</span> {analise.descricao}</div>
                <div><span className="font-semibold text-[#0F172A]">Estado:</span> {analise.estado}</div>
                {analise.estado === 'PENDENTE' && (
                  <button
                    onClick={reverEAprovar}
                    className="mt-2 w-full inline-flex items-center justify-center gap-1.5 rounded-md bg-[#059669] hover:bg-[#047857] px-3 py-2 text-white text-sm font-medium transition-colors"
                  >
                    <CheckCircle2 size={14} /> Rever e Aprovar Lançamento <ArrowRight size={14} />
                  </button>
                )}
              </div>
            ) : (
              <p className="text-sm text-[#94A3B8]">Ainda não foi feita a análise.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
