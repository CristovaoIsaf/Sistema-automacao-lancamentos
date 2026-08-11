import { useEffect, useMemo, useState } from 'react';
import {
  Search,
  Download,
  Eye,
  ChevronDown,
  ChevronRight,
  FolderOpen,
  FileArchive,
  FileText,
  Image as ImageIcon,
  Loader2,
  Inbox,
  IdCard,
  X,
  AlertCircle,
  AlertTriangle,
} from 'lucide-react';
import * as Dialog from '@radix-ui/react-dialog';
import { toast } from 'sonner';
import { abrirDocumento, descarregarDocumento, exportarZipDocumentos, listarDocumentos } from '../api/documentoApi';
import { obterDossieEntidade } from '../api/entidadeApi';
import { parseValidacao } from '../components/ValidacaoDocumento';
import type { Documento, EntidadeDossie } from '../types/documento';

const TIPO_ENTIDADE_LABEL: Record<string, string> = {
  CLIENTE: 'Cliente',
  FORNECEDOR: 'Fornecedor',
  DESCONHECIDO: 'Sem identificação',
};

const SEM_IDENTIFICACAO = 'Sem identificação';

const ESTADO_ESTILO: Record<string, string> = {
  Pendente: 'bg-[#F1F5F9] text-[#475569]',
  Analisado: 'bg-[#EDE9FE] text-[#7C3AED]',
  Aprovado: 'bg-[#D1FAE5] text-[#059669]',
  Rejeitado: 'bg-[#FEE2E2] text-[#B91C1C]',
};

function iconePorTipo(tipoConteudo: string) {
  if (tipoConteudo?.startsWith('image/')) return ImageIcon;
  return FileText;
}

function formatarTamanho(bytes?: number): string {
  if (!bytes) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// Fase 16 do plano de 20 fases ("auditor: inconsistências") — indicador
// compacto por documento, reutilizando o mesmo parseValidacao já usado no
// fluxo de upload (ValidacaoDocumento.tsx); aqui só a contagem, o detalhe
// completo (mensagens) fica no title (tooltip) para não sobrecarregar a
// linha da tabela.
function BadgeInconsistencias({ validacaoJson }: { validacaoJson?: string | null }) {
  const resultado = parseValidacao(validacaoJson);
  if (!resultado || resultado.problemas.length === 0) return null;

  const erros = resultado.problemas.filter(p => p.gravidade === 'erro');
  const avisos = resultado.problemas.filter(p => p.gravidade === 'aviso');
  const titulo = resultado.problemas.map(p => p.mensagem).join('\n');

  if (erros.length > 0) {
    return (
      <span
        title={titulo}
        className="inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full shrink-0 bg-[#FEF2F2] text-[#DC2626]"
      >
        <AlertCircle size={11} /> {erros.length} erro{erros.length > 1 ? 's' : ''}
      </span>
    );
  }

  return (
    <span
      title={titulo}
      className="inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full shrink-0 bg-[#FFFBEB] text-[#D97706]"
    >
      <AlertTriangle size={11} /> {avisos.length} aviso{avisos.length > 1 ? 's' : ''}
    </span>
  );
}

export function Arquivo() {
  const [documentos, setDocumentos] = useState<Documento[]>([]);
  const [loading, setLoading] = useState(true);
  const [pesquisa, setPesquisa] = useState('');
  const [dataInicio, setDataInicio] = useState('');
  const [dataFim, setDataFim] = useState('');
  const [gruposFechados, setGruposFechados] = useState<Set<string>>(new Set());
  const [exportando, setExportando] = useState(false);
  const [aAbrir, setAAbrir] = useState<number | null>(null);
  const [dossieEntidadeId, setDossieEntidadeId] = useState<number | null>(null);
  const [dossie, setDossie] = useState<EntidadeDossie | null>(null);
  const [carregandoDossie, setCarregandoDossie] = useState(false);

  useEffect(() => {
    carregar();
  }, []);

  const carregar = async () => {
    try {
      setLoading(true);
      const dados = await listarDocumentos();
      setDocumentos(dados);
    } catch (err) {
      console.error(err);
      toast.error('Não foi possível carregar os documentos');
    } finally {
      setLoading(false);
    }
  };

  const verDossie = async (entidadeId: number) => {
    setDossieEntidadeId(entidadeId);
    setCarregandoDossie(true);
    try {
      const dados = await obterDossieEntidade(entidadeId);
      setDossie(dados);
    } catch (err) {
      toast.error('Não foi possível carregar o dossiê da entidade');
      setDossieEntidadeId(null);
    } finally {
      setCarregandoDossie(false);
    }
  };

  const fecharDossie = () => {
    setDossieEntidadeId(null);
    setDossie(null);
  };

  // Fase 10 do plano de 20 fases — pesquisa por entidade, NIF, número,
  // tipo e valor (além do ficheiro/data já existentes), e filtro por
  // período de upload.
  const grupos = useMemo(() => {
    const termo = pesquisa.trim().toLowerCase();
    const filtrados = documentos.filter((doc) => {
      const matchTermo = !termo ||
        doc.nomeFicheiro.toLowerCase().includes(termo) ||
        (doc.entidadeNome ?? '').toLowerCase().includes(termo) ||
        (doc.entidadeNif ?? '').toLowerCase().includes(termo) ||
        (doc.numeroDocumento ?? '').toLowerCase().includes(termo) ||
        (doc.tipoDocumento ?? '').toLowerCase().includes(termo) ||
        (doc.valor ?? '').toLowerCase().includes(termo);

      const dataDoc = doc.dataUpload.slice(0, 10);
      const matchInicio = !dataInicio || dataDoc >= dataInicio;
      const matchFim = !dataFim || dataDoc <= dataFim;

      return matchTermo && matchInicio && matchFim;
    });

    const porEntidade = new Map<string, Documento[]>();
    for (const doc of filtrados) {
      const chave = doc.entidadeNome ?? SEM_IDENTIFICACAO;
      const lista = porEntidade.get(chave) ?? [];
      lista.push(doc);
      porEntidade.set(chave, lista);
    }

    return Array.from(porEntidade.entries())
      .sort(([a], [b]) => {
        if (a === SEM_IDENTIFICACAO) return 1;
        if (b === SEM_IDENTIFICACAO) return -1;
        return a.localeCompare(b);
      })
      .map(([nome, docs]) => ({
        nome,
        entidadeId: docs[0]?.entidadeId ?? null,
        docs: docs.sort((a, b) => new Date(b.dataUpload).getTime() - new Date(a.dataUpload).getTime()),
      }));
  }, [documentos, pesquisa, dataInicio, dataFim]);

  const alternarGrupo = (nome: string) => {
    setGruposFechados((atual) => {
      const novo = new Set(atual);
      if (novo.has(nome)) novo.delete(nome);
      else novo.add(nome);
      return novo;
    });
  };

  const verDocumento = async (doc: Documento) => {
    try {
      setAAbrir(doc.id);
      await abrirDocumento(doc.id);
    } catch (err) {
      toast.error('Não foi possível abrir o documento');
    } finally {
      setAAbrir(null);
    }
  };

  const descarregar = async (doc: Documento) => {
    try {
      await descarregarDocumento(doc.id, doc.nomeFicheiro);
    } catch (err) {
      toast.error('Não foi possível descarregar o documento');
    }
  };

  const exportarTudo = async () => {
    try {
      setExportando(true);
      await exportarZipDocumentos();
      toast.success('Arquivo .zip organizado por entidade descarregado');
    } catch (err) {
      toast.error('Não foi possível exportar o arquivo');
    } finally {
      setExportando(false);
    }
  };

  return (
    <div className="w-full max-w-[1400px] mx-auto space-y-4 px-2 sm:px-4 lg:px-6">
      <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-3 px-1 sm:px-0">
        <div>
          <h1 className="text-[18px] sm:text-[20px] font-semibold text-[#0F172A]">Documentos</h1>
          <p className="text-[13px] text-[#475569] mt-0.5">Arquivo organizado por entidade — clientes, fornecedores e por classificar</p>
        </div>
        <button
          onClick={exportarTudo}
          disabled={exportando || documentos.length === 0}
          className="inline-flex items-center gap-2 rounded-md bg-[#0F172A] hover:bg-[#1E293B] px-3 py-2 text-white text-[13px] font-medium disabled:opacity-50 shrink-0"
        >
          {exportando ? <Loader2 className="animate-spin" size={14} /> : <FileArchive size={14} />}
          Exportar tudo (.zip organizado)
        </button>
      </div>

      <div className="flex flex-col sm:flex-row gap-2">
        <div className="relative flex-1">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#94A3B8]" />
          <input
            value={pesquisa}
            onChange={(e) => setPesquisa(e.target.value)}
            placeholder="Pesquisar por ficheiro, entidade, NIF, número ou tipo..."
            className="w-full rounded-md border border-[#E2E8F0] bg-white pl-9 pr-3 py-2 text-[13px] text-[#0F172A] placeholder:text-[#94A3B8] focus:outline-none focus:ring-2 focus:ring-[#2563EB]/30 focus:border-[#2563EB]"
          />
        </div>
        <div className="flex items-center gap-1.5 shrink-0">
          <input
            type="date"
            value={dataInicio}
            onChange={(e) => setDataInicio(e.target.value)}
            className="h-[34px] px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:ring-2 focus:ring-[#2563EB]/30 focus:border-[#2563EB]"
          />
          <span className="text-[#94A3B8] text-[12px]">até</span>
          <input
            type="date"
            value={dataFim}
            onChange={(e) => setDataFim(e.target.value)}
            className="h-[34px] px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:ring-2 focus:ring-[#2563EB]/30 focus:border-[#2563EB]"
          />
        </div>
      </div>

      {loading ? (
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-8 flex items-center justify-center gap-2 text-[13px] text-[#94A3B8]">
          <Loader2 className="animate-spin" size={16} /> A carregar documentos...
        </div>
      ) : documentos.length === 0 ? (
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-10 flex flex-col items-center justify-center text-center gap-2">
          <Inbox size={28} className="text-[#CBD5E1]" />
          <p className="text-[13px] text-[#475569] font-medium">Ainda não há documentos no arquivo</p>
          <p className="text-[12px] text-[#94A3B8]">Carrega o primeiro documento em "Novo Documento"</p>
        </div>
      ) : grupos.length === 0 ? (
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-8 text-center text-[13px] text-[#94A3B8]">
          Nenhum documento corresponde à pesquisa.
        </div>
      ) : (
        <div className="space-y-3">
          {grupos.map(({ nome, entidadeId, docs }) => {
            const fechado = gruposFechados.has(nome);
            const semIdentificacao = nome === SEM_IDENTIFICACAO;
            return (
              <div key={nome} className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
                <div className="w-full flex items-center gap-2 px-3 sm:px-4 py-2.5 hover:bg-[#F8FAFC] transition-colors">
                  <button
                    onClick={() => alternarGrupo(nome)}
                    className="flex items-center gap-2 flex-1 min-w-0 text-left"
                  >
                    {fechado ? (
                      <ChevronRight size={15} className="text-[#94A3B8] shrink-0" />
                    ) : (
                      <ChevronDown size={15} className="text-[#94A3B8] shrink-0" />
                    )}
                    <FolderOpen size={15} className={semIdentificacao ? 'text-[#94A3B8] shrink-0' : 'text-[#2563EB] shrink-0'} />
                    <span className="text-[13px] font-semibold text-[#0F172A] truncate">{nome}</span>
                    <span className="text-[12px] text-[#94A3B8] ml-1">({docs.length})</span>
                  </button>
                  {!semIdentificacao && entidadeId != null && (
                    <button
                      onClick={() => verDossie(entidadeId)}
                      title="Ver dossiê da entidade"
                      className="flex items-center gap-1 h-7 px-2 text-[11px] font-medium text-[#2563EB] hover:bg-[#EFF6FF] rounded-md transition-colors shrink-0"
                    >
                      <IdCard size={13} /> Dossiê
                    </button>
                  )}
                </div>

                {!fechado && (
                  <div className="border-t border-[#F1F5F9] divide-y divide-[#F1F5F9]">
                    {docs.map((doc) => {
                      const Icone = iconePorTipo(doc.tipoConteudo);
                      const estilo = ESTADO_ESTILO[doc.estado ?? 'Pendente'];
                      return (
                        <div key={doc.id} className="flex items-center gap-3 px-3 sm:px-4 py-2.5">
                          <Icone size={16} className="text-[#94A3B8] shrink-0" />
                          <div className="min-w-0 flex-1">
                            <p className="text-[13px] text-[#0F172A] truncate">{doc.nomeFicheiro}</p>
                            <p className="text-[11px] text-[#94A3B8]">
                              {new Date(doc.dataUpload).toLocaleString('pt-AO')} · {formatarTamanho(doc.tamanho)}
                            </p>
                          </div>
                          <span className={`text-[11px] font-medium px-2 py-0.5 rounded-full shrink-0 ${estilo}`}>
                            {doc.estado ?? 'Pendente'}
                          </span>
                          <BadgeInconsistencias validacaoJson={doc.validacaoJson} />
                          <div className="flex items-center gap-1 shrink-0">
                            <button
                              onClick={() => verDocumento(doc)}
                              disabled={aAbrir === doc.id}
                              title="Ver documento"
                              className="h-7 w-7 inline-flex items-center justify-center rounded-md text-[#475569] hover:bg-[#F1F5F9] hover:text-[#2563EB] disabled:opacity-50"
                            >
                              {aAbrir === doc.id ? <Loader2 className="animate-spin" size={14} /> : <Eye size={14} />}
                            </button>
                            <button
                              onClick={() => descarregar(doc)}
                              title="Descarregar"
                              className="h-7 w-7 inline-flex items-center justify-center rounded-md text-[#475569] hover:bg-[#F1F5F9] hover:text-[#2563EB]"
                            >
                              <Download size={14} />
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      <Dialog.Root open={dossieEntidadeId !== null} onOpenChange={(v) => !v && fecharDossie()}>
        <Dialog.Portal>
          <Dialog.Overlay className="fixed inset-0 bg-black/30 z-50" />
          <Dialog.Content className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-[560px] max-h-[80vh] flex flex-col bg-white border border-[#E2E8F0] rounded-xl shadow-sm">
            <div className="flex items-center justify-between px-5 py-3.5 border-b border-[#E2E8F0] shrink-0">
              <Dialog.Title className="text-[14px] font-semibold text-[#0F172A]">Dossiê da entidade</Dialog.Title>
              <button onClick={fecharDossie} className="w-6 h-6 flex items-center justify-center text-[#94A3B8] hover:text-[#0F172A] hover:bg-[#F1F5F9] rounded transition-colors">
                <X size={14} />
              </button>
            </div>

            <div className="p-5 overflow-y-auto">
              {carregandoDossie ? (
                <div className="flex items-center justify-center gap-2 text-[13px] text-[#94A3B8] py-8">
                  <Loader2 className="animate-spin" size={16} /> A carregar dossiê...
                </div>
              ) : dossie ? (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <p className="text-[11px] font-medium text-[#94A3B8] uppercase tracking-wide mb-0.5">Nome</p>
                      <p className="text-[13px] font-medium text-[#0F172A]">{dossie.nome}</p>
                    </div>
                    <div>
                      <p className="text-[11px] font-medium text-[#94A3B8] uppercase tracking-wide mb-0.5">NIF</p>
                      <p className="text-[13px] font-medium text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{dossie.nif}</p>
                    </div>
                    <div>
                      <p className="text-[11px] font-medium text-[#94A3B8] uppercase tracking-wide mb-0.5">Tipo</p>
                      <p className="text-[13px] font-medium text-[#0F172A]">{TIPO_ENTIDADE_LABEL[dossie.tipo] ?? dossie.tipo}</p>
                    </div>
                    <div>
                      <p className="text-[11px] font-medium text-[#94A3B8] uppercase tracking-wide mb-0.5">Documentos</p>
                      <p className="text-[13px] font-medium text-[#0F172A]">{dossie.documentos.length}</p>
                    </div>
                  </div>

                  {/* Fase 12 do plano de 20 fases — "Perfil de Entidade":
                      conhecimento acumulado sobre o comportamento habitual
                      desta entidade, já usado internamente para poupar
                      chamadas de IA, agora também visível aqui. Omitido
                      quando ainda não há histórico (nunca inventa dados). */}
                  {dossie.perfil && dossie.perfil.totalDocumentos > 0 && (
                    <div className="border-t border-[#E2E8F0] pt-3 space-y-1.5">
                      <p className="text-[11px] font-medium text-[#94A3B8] uppercase tracking-wide mb-0.5">
                        Perfil (conhecimento acumulado)
                      </p>
                      {dossie.perfil.tipoDominante && (
                        <p className="text-[12px] text-[#0F172A]">
                          Tipo habitual: <span className="font-medium" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{dossie.perfil.tipoDominante}</span>
                        </p>
                      )}
                      <div className="flex flex-wrap gap-1.5">
                        {Object.entries(dossie.perfil.distribuicaoTiposDocumento).map(([tipo, contagem]) => (
                          <span
                            key={tipo}
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium bg-[#F1F5F9] text-[#475569]"
                            style={{ fontFamily: 'JetBrains Mono, monospace' }}
                          >
                            {tipo} × {contagem}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  <div className="border-t border-[#E2E8F0] pt-3 space-y-1.5">
                    {dossie.documentos.length === 0 ? (
                      <p className="text-[12px] text-[#94A3B8]">Nenhum documento arquivado.</p>
                    ) : (
                      dossie.documentos
                        .sort((a, b) => new Date(b.dataUpload).getTime() - new Date(a.dataUpload).getTime())
                        .map((doc) => (
                          <div key={doc.id} className="flex items-center justify-between gap-2 text-[12px] py-1">
                            <span className="text-[#0F172A] truncate">{doc.nomeFicheiro}</span>
                            <span className="text-[#94A3B8] shrink-0">{new Date(doc.dataUpload).toLocaleDateString('pt-AO')}</span>
                          </div>
                        ))
                    )}
                  </div>
                </div>
              ) : null}
            </div>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>
    </div>
  );
}
