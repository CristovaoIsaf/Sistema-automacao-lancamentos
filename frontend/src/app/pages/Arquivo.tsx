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
} from 'lucide-react';
import { toast } from 'sonner';
import { abrirDocumento, descarregarDocumento, exportarZipDocumentos, listarDocumentos } from '../api/documentoApi';
import type { Documento } from '../types/documento';

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

export function Arquivo() {
  const [documentos, setDocumentos] = useState<Documento[]>([]);
  const [loading, setLoading] = useState(true);
  const [pesquisa, setPesquisa] = useState('');
  const [gruposFechados, setGruposFechados] = useState<Set<string>>(new Set());
  const [exportando, setExportando] = useState(false);
  const [aAbrir, setAAbrir] = useState<number | null>(null);

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

  const grupos = useMemo(() => {
    const termo = pesquisa.trim().toLowerCase();
    const filtrados = documentos.filter((doc) => {
      if (!termo) return true;
      return (
        doc.nomeFicheiro.toLowerCase().includes(termo) ||
        (doc.entidadeNome ?? '').toLowerCase().includes(termo)
      );
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
        docs: docs.sort((a, b) => new Date(b.dataUpload).getTime() - new Date(a.dataUpload).getTime()),
      }));
  }, [documentos, pesquisa]);

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

      <div className="relative">
        <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#94A3B8]" />
        <input
          value={pesquisa}
          onChange={(e) => setPesquisa(e.target.value)}
          placeholder="Pesquisar por ficheiro ou entidade..."
          className="w-full rounded-md border border-[#E2E8F0] bg-white pl-9 pr-3 py-2 text-[13px] text-[#0F172A] placeholder:text-[#94A3B8] focus:outline-none focus:ring-2 focus:ring-[#2563EB]/30 focus:border-[#2563EB]"
        />
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
          {grupos.map(({ nome, docs }) => {
            const fechado = gruposFechados.has(nome);
            const semIdentificacao = nome === SEM_IDENTIFICACAO;
            return (
              <div key={nome} className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
                <button
                  onClick={() => alternarGrupo(nome)}
                  className="w-full flex items-center gap-2 px-3 sm:px-4 py-2.5 hover:bg-[#F8FAFC] transition-colors text-left"
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
    </div>
  );
}
