import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import {
  Upload, FileText, CheckCircle2, AlertCircle, Sparkles, Loader2, ArrowRight,
  Pencil, Check, X, ZoomIn, ZoomOut, RotateCw, ShieldCheck, ShieldAlert, ShieldQuestion,
} from 'lucide-react';
import { toast } from 'sonner';
import { uploadDocumento, corrigirClassificacaoDocumento } from '../api/documentoApi';
import { analisarDocumento } from '../api/sugestaoApi';
import type { ProblemaValidacao, Sugestao } from '../types/documento';
import { ValidacaoDocumento, parseValidacao } from '../components/ValidacaoDocumento';
import { TIPO_DOCUMENTO_LABEL } from '../data/tiposDocumento';

// Nomenclatura usada pelo motor de validação determinística
// (fastapi/app/services/document_validation.py — regra_formato_nif,
// regra_valor_total, etc.): campos de DadosFatura em snake_case Python,
// diferente dos nomes do domínio Java/frontend. Mapeados aqui só para
// ligar um ProblemaValidacao ao campo correspondente nesta UI — sem isto
// não há forma de saber a que campo um "NIF do emitente tem formato
// inválido" se refere.
const CAMPOS_VALIDACAO: Record<string, string[]> = {
  tipoDocumento: ['tipo_documento'],
  numeroDocumento: ['numero_fatura'],
  entidade: ['emitente_nome', 'adquirente_nome'],
  nif: ['emitente_nif', 'adquirente_nif'],
  valor: ['valor_total_aoa'],
};

type NivelConfianca = 'alta' | 'media' | 'baixa';

// Não existe confiança POR CAMPO no backend (só Sugestao.confianca, um
// único valor agregado para a análise inteira) — esta função aproxima
// isso combinando o que existe de verdade: (1) o campo está mesmo
// preenchido, (2) o motor de validação determinística encontrou algum
// problema especificamente NESTE campo (esse sim é por campo, ver
// document_validation.py), (3) como último recurso, a confiança geral.
function confiancaDoCampo(
  chaveCampo: keyof typeof CAMPOS_VALIDACAO,
  valor: string | null | undefined,
  problemas: ProblemaValidacao[],
  confiancaGeral: number | null | undefined
): { nivel: NivelConfianca; mensagem?: string } {
  if (!valor || !valor.trim()) {
    return { nivel: 'baixa', mensagem: 'Não encontrado no documento' };
  }
  const nomesPython = CAMPOS_VALIDACAO[chaveCampo] ?? [];
  const problema = problemas.find(p => nomesPython.includes(p.campo));
  if (problema?.gravidade === 'erro') {
    return { nivel: 'baixa', mensagem: problema.mensagem };
  }
  if (problema?.gravidade === 'aviso') {
    return { nivel: 'media', mensagem: problema.mensagem };
  }
  if (confiancaGeral != null && confiancaGeral < 60) {
    return { nivel: 'media' };
  }
  return { nivel: 'alta' };
}

const CORES_NIVEL: Record<NivelConfianca, { borda: string; texto: string; Icone: typeof ShieldCheck }> = {
  alta: { borda: '#059669', texto: '#059669', Icone: ShieldCheck },
  media: { borda: '#D97706', texto: '#D97706', Icone: ShieldQuestion },
  baixa: { borda: '#DC2626', texto: '#DC2626', Icone: ShieldAlert },
};

function CampoExtraido({
  label, valor, nivel, mensagem, valorOriginal,
}: {
  label: string;
  valor: string;
  nivel: NivelConfianca;
  mensagem?: string;
  valorOriginal?: string | null;
}) {
  const cor = CORES_NIVEL[nivel];
  const foiEditado = valorOriginal != null && valorOriginal !== valor;
  return (
    <div className="py-2 pl-2.5" style={{ borderLeft: `3px solid ${cor.borda}` }}>
      <div className="flex items-center justify-between gap-2">
        <span className="text-[11px] font-medium text-[#94A3B8] uppercase tracking-wide">{label}</span>
        <div className="flex items-center gap-1.5 flex-shrink-0">
          {foiEditado && (
            <span
              title={`Valor original da IA: ${valorOriginal}`}
              className="text-[10px] text-[#7C3AED] bg-[#F5F3FF] px-1.5 py-0.5 rounded-full"
            >
              editado
            </span>
          )}
          <cor.Icone style={{ width: 12, height: 12, color: cor.texto }} />
        </div>
      </div>
      <p className="text-[13px] font-medium text-[#0F172A] mt-0.5 break-words">{valor || '—'}</p>
      {mensagem && <p className="text-[11px] mt-0.5" style={{ color: cor.texto }}>{mensagem}</p>}
    </div>
  );
}

export function UploadDocumentos() {
  const navigate = useNavigate();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [zoom, setZoom] = useState(1);
  const [rotacao, setRotacao] = useState(0);
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [documentId, setDocumentId] = useState<number | null>(null);
  const [analise, setAnalise] = useState<Sugestao | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Correção rápida da extração (tipo/nº/valor) sem sair do ecrã de
  // upload — pedido explícito do contabilista: rever e ajustar o que a IA
  // extraiu logo aqui, antes de "Rever e Aprovar" (que só edita as linhas
  // de débito/crédito, ver LancamentoDiario.tsx), acelera o fluxo.
  //
  // Os 3 campos editam-se em conjunto (não campo a campo): PATCH
  // /documentos/{id}/classificacao aceita os três de uma vez só — não há
  // 3 endpoints independentes, por isso a UI reflecte isso mesmo em vez
  // de fingir uma gravação campo a campo que o backend não suporta.
  const [aEditar, setAEditar] = useState(false);
  const [edTipo, setEdTipo] = useState('');
  const [edNumero, setEdNumero] = useState('');
  const [edValor, setEdValor] = useState('');
  const [aGuardarCorrecao, setAGuardarCorrecao] = useState(false);

  useEffect(() => {
    if (!selectedFile) {
      setPreviewUrl(null);
      return;
    }
    const url = URL.createObjectURL(selectedFile);
    setPreviewUrl(url);
    setZoom(1);
    setRotacao(0);
    return () => URL.revokeObjectURL(url);
  }, [selectedFile]);

  const reset = () => {
    setAnalise(null);
    setError(null);
    setAEditar(false);
  };

  const iniciarEdicao = () => {
    if (!analise) return;
    setEdTipo(analise.tipoDocumento);
    setEdNumero(analise.numeroDocumento ?? '');
    setEdValor(analise.valor);
    setAEditar(true);
  };

  const guardarCorrecao = async () => {
    if (!documentId || !analise) return;
    try {
      setAGuardarCorrecao(true);
      await corrigirClassificacaoDocumento(documentId, {
        tipoDocumento: edTipo,
        numeroDocumento: edNumero,
        valor: edValor,
      });
      setAnalise({
        ...analise,
        tipoDocumento: edTipo,
        numeroDocumento: edNumero,
        valor: edValor,
        // Snapshot local otimista — a próxima análise/recarregamento traz
        // o valor real de *OriginalIA vindo do backend (ver Sugestao.java
        // corrigirClassificacao), isto só evita esperar por isso para o
        // selo "editado" aparecer já nesta sessão.
        tipoDocumentoOriginalIA: analise.tipoDocumentoOriginalIA ?? analise.tipoDocumento,
        numeroDocumentoOriginalIA: analise.numeroDocumentoOriginalIA ?? analise.numeroDocumento,
        valorOriginalIA: analise.valorOriginalIA ?? analise.valor,
      });
      setAEditar(false);
      toast.success('Correção guardada');
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao guardar a correção';
      toast.error(message);
    } finally {
      setAGuardarCorrecao(false);
    }
  };

  const handleFileChange = (file: File | null) => {
    reset();
    setDocumentId(null);
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

  const resultadoValidacao = useMemo(() => parseValidacao(analise?.validacaoJson), [analise?.validacaoJson]);
  const problemas = resultadoValidacao?.problemas ?? [];

  // Valor Base (sem IVA) não é um campo extraído à parte — não existe no
  // backend (Sugestao só guarda o total + o valor de IVA calculado, ver
  // PartidasDobradas.calcularValorIvaSugerido) — calculado aqui como
  // total - IVA, e assinalado como tal, para não fingir ser um dado
  // extraído do documento.
  const valorBaseCalculado = useMemo(() => {
    if (!analise) return null;
    const total = Number(String(analise.valor).replace(/[^0-9.,-]/g, '').replace(',', '.'));
    const iva = analise.valorIva ?? 0;
    if (Number.isNaN(total)) return null;
    return (total - iva).toFixed(2);
  }, [analise]);

  return (
    <div className="w-full max-w-[1400px] mx-auto space-y-4 px-2 sm:px-4 lg:px-6">
      <div className="px-1 sm:px-0 flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="text-[18px] sm:text-[20px] font-semibold text-[#0F172A]">Revisão de Documento</h1>
          <p className="text-[13px] text-[#475569] mt-0.5">Upload · Análise · Aprovação</p>
        </div>
        {analise && (
          <span
            className="text-[11px] font-medium px-2 py-1 rounded-full"
            style={{
              backgroundColor: analise.estado === 'PENDENTE' ? '#EDE9FE' : analise.estado === 'APROVADA' ? '#D1FAE5' : '#FEE2E2',
              color: analise.estado === 'PENDENTE' ? '#7C3AED' : analise.estado === 'APROVADA' ? '#059669' : '#B91C1C',
            }}
          >
            {analise.estado === 'PENDENTE' ? 'Em Revisão' : analise.estado === 'APROVADA' ? 'Aprovado' : 'Rejeitado'}
          </span>
        )}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] gap-4">
        {/* ── COLUNA ESQUERDA: documento original ──────────────────── */}
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-3 sm:p-4 flex flex-col">
          {!previewUrl ? (
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
            </div>
          ) : (
            <>
              <div className="flex items-center justify-between mb-2">
                <p className="text-[12px] text-[#475569] truncate">{selectedFile?.name}</p>
                <div className="flex items-center gap-1 flex-shrink-0">
                  <button onClick={() => setZoom(z => Math.max(0.5, z - 0.25))} title="Diminuir zoom" className="h-6 w-6 inline-flex items-center justify-center rounded text-[#475569] hover:bg-[#F1F5F9]">
                    <ZoomOut size={14} />
                  </button>
                  <span className="text-[11px] text-[#94A3B8] w-9 text-center">{Math.round(zoom * 100)}%</span>
                  <button onClick={() => setZoom(z => Math.min(2, z + 0.25))} title="Aumentar zoom" className="h-6 w-6 inline-flex items-center justify-center rounded text-[#475569] hover:bg-[#F1F5F9]">
                    <ZoomIn size={14} />
                  </button>
                  <button onClick={() => setRotacao(r => (r + 90) % 360)} title="Rodar" className="h-6 w-6 inline-flex items-center justify-center rounded text-[#475569] hover:bg-[#F1F5F9]">
                    <RotateCw size={14} />
                  </button>
                  <label className="text-[11px] text-[#2563EB] hover:text-[#1D4ED8] cursor-pointer ml-1">
                    Trocar
                    <input
                      type="file"
                      className="hidden"
                      accept=".pdf,.png,.jpg,.jpeg"
                      onChange={(e) => handleFileChange(e.target.files?.[0] ?? null)}
                    />
                  </label>
                </div>
              </div>
              <div className="flex-1 min-h-[320px] max-h-[70vh] overflow-auto rounded-md bg-[#F8FAFC] border border-[#E2E8F0] flex items-center justify-center p-2">
                {selectedFile?.type === 'application/pdf' ? (
                  <embed src={previewUrl} type="application/pdf" className="w-full h-[65vh]" />
                ) : (
                  <img
                    src={previewUrl}
                    alt="Documento carregado"
                    style={{ transform: `scale(${zoom}) rotate(${rotacao}deg)`, transition: 'transform 0.15s ease' }}
                    className="max-w-full"
                  />
                )}
              </div>
            </>
          )}

          <div className="mt-4 flex flex-wrap gap-2">
            <button onClick={uploadFile} disabled={uploading || !selectedFile || !!documentId} className="w-full sm:w-auto flex-1 min-w-[120px] rounded-md bg-[#2563EB] px-3 py-2 text-white text-sm disabled:opacity-50">
              {uploading ? <span className="inline-flex items-center justify-center gap-2"><Loader2 className="animate-spin" size={14} />A carregar...</span> : documentId ? 'Carregado ✓' : '1. Upload'}
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

        {/* ── COLUNA DIREITA: campos extraídos ─────────────────────── */}
        <div className="space-y-4 min-w-0">
          <div className="bg-white border border-[#E2E8F0] rounded-lg p-3 sm:p-4">
            <div className="flex items-center gap-2 mb-3">
              <FileText size={16} className="text-[#2563EB]" />
              <h2 className="text-[13px] font-semibold text-[#0F172A]">Estado do fluxo</h2>
            </div>
            <div className="space-y-2 text-sm text-[#475569]">
              <div>Documento ID: <span className="font-semibold text-[#0F172A]">{documentId ?? '—'}</span></div>
              <div>Análise ID: <span className="font-semibold text-[#0F172A]">{analise?.id ?? '—'}</span></div>
              {analise?.confianca != null && (
                <div>Confiança geral: <span className="font-semibold text-[#0F172A]">{analise.confianca}%</span></div>
              )}
            </div>
          </div>

          <div className="bg-white border border-[#E2E8F0] rounded-lg p-4">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Sparkles size={16} className="text-[#7C3AED]" />
                <h2 className="text-[13px] font-semibold text-[#0F172A]">Campos Extraídos</h2>
              </div>
              {analise && !aEditar && (
                <button
                  onClick={iniciarEdicao}
                  title="Corrigir tipo/número/valor extraídos"
                  className="h-6 w-6 inline-flex items-center justify-center rounded-md text-[#475569] hover:bg-[#F1F5F9] hover:text-[#2563EB]"
                >
                  <Pencil size={13} />
                </button>
              )}
            </div>
            {analise ? (
              <div className="space-y-0.5 text-sm text-[#475569]">
                {aEditar ? (
                  <div className="space-y-2.5 mb-2 pb-2 border-b border-[#F1F5F9]">
                    <div>
                      <label className="block text-[11px] font-medium text-[#475569] mb-1">Tipo de documento</label>
                      <select
                        value={edTipo}
                        onChange={e => setEdTipo(e.target.value)}
                        className="w-full h-8 px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all"
                      >
                        {Object.entries(TIPO_DOCUMENTO_LABEL).map(([valor, rotulo]) => (
                          <option key={valor} value={valor}>{rotulo}</option>
                        ))}
                      </select>
                    </div>
                    <div className="grid grid-cols-2 gap-2.5">
                      <div>
                        <label className="block text-[11px] font-medium text-[#475569] mb-1">Nº Documento</label>
                        <input
                          type="text"
                          value={edNumero}
                          onChange={e => setEdNumero(e.target.value)}
                          className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
                        />
                      </div>
                      <div>
                        <label className="block text-[11px] font-medium text-[#475569] mb-1">Valor Total</label>
                        <input
                          type="text"
                          value={edValor}
                          onChange={e => setEdValor(e.target.value)}
                          className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
                        />
                      </div>
                    </div>
                    <div className="flex items-center gap-2 pt-0.5">
                      <button
                        onClick={guardarCorrecao}
                        disabled={aGuardarCorrecao}
                        className="flex items-center gap-1.5 h-7 px-2.5 bg-[#2563EB] hover:bg-[#1D4ED8] disabled:opacity-50 text-white text-[12px] font-medium rounded-md transition-colors"
                      >
                        {aGuardarCorrecao ? <Loader2 className="animate-spin" size={12} /> : <Check size={12} />} Guardar
                      </button>
                      <button
                        onClick={() => setAEditar(false)}
                        disabled={aGuardarCorrecao}
                        className="flex items-center gap-1.5 h-7 px-2.5 bg-white border border-[#E2E8F0] hover:bg-[#F8FAFC] text-[#0F172A] text-[12px] font-medium rounded-md transition-colors"
                      >
                        <X size={12} /> Cancelar
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <CampoExtraido
                      label="Tipo de Documento"
                      valor={TIPO_DOCUMENTO_LABEL[analise.tipoDocumento] ?? analise.tipoDocumento}
                      {...confiancaDoCampo('tipoDocumento', analise.tipoDocumento, problemas, analise.confianca)}
                      valorOriginal={analise.tipoDocumentoOriginalIA ? (TIPO_DOCUMENTO_LABEL[analise.tipoDocumentoOriginalIA] ?? analise.tipoDocumentoOriginalIA) : null}
                    />
                    <CampoExtraido
                      label="Número do Documento"
                      valor={analise.numeroDocumento ?? ''}
                      {...confiancaDoCampo('numeroDocumento', analise.numeroDocumento, problemas, analise.confianca)}
                      valorOriginal={analise.numeroDocumentoOriginalIA}
                    />
                    <CampoExtraido
                      label="Fornecedor / Cliente"
                      valor={analise.entidade ?? ''}
                      {...confiancaDoCampo('entidade', analise.entidade, problemas, analise.confianca)}
                    />
                    <CampoExtraido
                      label="NIF"
                      valor={analise.nif ?? ''}
                      {...confiancaDoCampo('nif', analise.nif, problemas, analise.confianca)}
                    />
                    <CampoExtraido
                      label="Valor Total"
                      valor={analise.valor}
                      {...confiancaDoCampo('valor', analise.valor, problemas, analise.confianca)}
                      valorOriginal={analise.valorOriginalIA}
                    />
                    {analise.valorIva != null && (
                      <CampoExtraido label="Valor de IVA (calculado)" valor={String(analise.valorIva)} nivel="alta" />
                    )}
                    {valorBaseCalculado != null && (
                      <CampoExtraido label="Valor Base — sem IVA (calculado)" valor={valorBaseCalculado} nivel="alta" />
                    )}
                    {analise.categoria && (
                      <CampoExtraido label="Categoria Sugerida" valor={analise.categoria} nivel="alta" />
                    )}
                    {/* Conta contabilística: mais destaque visual (fundo
                        diferenciado), só-leitura aqui de propósito — a
                        correção real da conta acontece no passo seguinte
                        (Rever e Aprovar, LancamentoDiario.tsx), onde as
                        linhas de débito/crédito já são editáveis; não
                        existe hoje nenhum endpoint para corrigir só a
                        categoriaContabil nesta fase, por isso não finge
                        ser editável aqui. */}
                    <div className="py-2 pl-2.5 mt-1 rounded-md bg-[#EFF6FF]" style={{ borderLeft: '3px solid #2563EB' }}>
                      <span className="text-[11px] font-medium text-[#2563EB] uppercase tracking-wide">Conta Contabilística Sugerida</span>
                      <p className="text-[13px] font-semibold text-[#0F172A] mt-0.5">{analise.categoriaContabil}</p>
                      <p className="text-[11px] text-[#475569] mt-0.5">Ajustável no próximo passo (Rever e Aprovar)</p>
                    </div>
                    <div className="pt-2">
                      <span className="text-[11px] font-medium text-[#94A3B8] uppercase tracking-wide">Descrição</span>
                      <p className="text-[13px] text-[#0F172A] mt-0.5">{analise.descricao}</p>
                    </div>
                  </>
                )}
                <div className="pt-2">
                  <ValidacaoDocumento validacaoJson={analise.validacaoJson} />
                </div>
                {analise.estado === 'PENDENTE' && !aEditar && (
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
