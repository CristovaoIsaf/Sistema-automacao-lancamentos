import { useEffect, useState } from 'react';
import { Search, Download, Eye, Edit2, Plus, CheckCircle2, Clock, XCircle, X, Check, Loader2 } from 'lucide-react';
import { formatarKwanza } from '../data/mockData';
import * as Dialog from '@radix-ui/react-dialog';
import { toast } from 'sonner';
import { useAuth, permissoes } from '../auth/AuthContext';
import { criarLancamento, exportarLancamentos, listarLancamentos } from '../api/lancamentoApi';
import { listarContas } from '../api/contaApi';
import type { LancamentoResponse } from '../types/lancamento';
import type { ContaResumo } from '../types/categoriaConta';

function Badge({ variant, children }: { variant: 'aprovado' | 'pendente' | 'rejeitado' | 'ia' | 'manual'; children: React.ReactNode }) {
  const styles = {
    aprovado: 'bg-[#ECFDF5] text-[#059669]',
    pendente:  'bg-[#FFFBEB] text-[#D97706]',
    rejeitado: 'bg-[#FEF2F2] text-[#DC2626]',
    ia:        'bg-[#F5F3FF] text-[#7C3AED]',
    manual:    'bg-[#EFF6FF] text-[#2563EB]',
  };
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium ${styles[variant]}`}>
      {children}
    </span>
  );
}

type StatusFilter = 'todos' | 'PENDENTE' | 'VALIDADO' | 'CANCELADO';

const estadoBadge: Record<string, { variant: 'aprovado' | 'pendente' | 'rejeitado'; label: string }> = {
  VALIDADO: { variant: 'aprovado', label: 'Validado' },
  PENDENTE: { variant: 'pendente', label: 'Pendente' },
  CANCELADO: { variant: 'rejeitado', label: 'Cancelado' },
};

function contaDebito(l: LancamentoResponse): string {
  return l.linhas.find(linha => Number(linha.debito) > 0)?.conta ?? '—';
}

function contaCredito(l: LancamentoResponse): string {
  return l.linhas.find(linha => Number(linha.credito) > 0)?.conta ?? '—';
}

function valorTotal(l: LancamentoResponse): number {
  return l.linhas.reduce((soma, linha) => soma + Number(linha.debito ?? 0), 0);
}

function hoje(): string {
  return new Date().toISOString().slice(0, 10);
}

function inicioDoAno(): string {
  return `${new Date().getFullYear()}-01-01`;
}

function NovoLancamentoDialog({ open, onClose, onCriado, contas }: { open: boolean; onClose: () => void; onCriado: () => void; contas: ContaResumo[] }) {
  const [form, setForm] = useState({ data: hoje(), documento: '', historico: '', contaD: '', contaC: '', valor: '' });
  const [salvando, setSalvando] = useState(false);
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm(prev => ({ ...prev, [k]: e.target.value }));

  const handleSave = async () => {

    if (!form.contaD || !form.contaC || !form.valor) {
        toast.error("Preencha todos os campos");
        return;
    }

    try {
        setSalvando(true);

        await criarLancamento({
            data: form.data,
            descricao: form.historico,
            linhas: [
                {
                    conta: form.contaD,
                    debito: Number(form.valor),
                    credito: 0,
                    descricao: form.historico
                },
                {
                    conta: form.contaC,
                    debito: 0,
                    credito: Number(form.valor),
                    descricao: form.historico
                }
            ]
        });

        toast.success("Lançamento registado com sucesso");
        onCriado();
        onClose();

    } catch (error) {

        console.error(error);
        toast.error("Erro ao guardar lançamento");

    } finally {
        setSalvando(false);
    }
};
  return (
    <Dialog.Root open={open} onOpenChange={v => !v && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/30 z-50" />
        <Dialog.Content className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-[480px] bg-white border border-[#E2E8F0] rounded-xl shadow-sm" style={{ fontFamily: 'Inter, system-ui, sans-serif' }}>
          <div className="flex items-center justify-between px-5 py-3.5 border-b border-[#E2E8F0]">
            <Dialog.Title className="text-[14px] font-semibold text-[#0F172A]">Novo Lançamento</Dialog.Title>
            <button onClick={onClose} className="w-6 h-6 flex items-center justify-center text-[#94A3B8] hover:text-[#0F172A] hover:bg-[#F1F5F9] rounded transition-colors">
              <X style={{ width: 14, height: 14 }} />
            </button>
          </div>

          <div className="p-5 space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Data</label>
                <input type="date" value={form.data} onChange={set('data')}
                  className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all" />
              </div>
              <div>
                <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Nº Documento</label>
                <input type="text" value={form.documento} onChange={set('documento')} placeholder="FT 001/2026"
                  className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all" />
              </div>
            </div>

            <div>
              <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Histórico</label>
              <input type="text" value={form.historico} onChange={set('historico')} placeholder="Descrição do lançamento..."
                className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all" />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Conta Débito</label>
                <select value={form.contaD} onChange={set('contaD')}
                  className="w-full h-8 px-2 text-[12px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all"
                  style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                  <option value="">Seleccionar...</option>
                  {contas.map(c => <option key={c.codigo} value={c.codigo}>{c.codigo} — {c.nome}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Conta Crédito</label>
                <select value={form.contaC} onChange={set('contaC')}
                  className="w-full h-8 px-2 text-[12px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all"
                  style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                  <option value="">Seleccionar...</option>
                  {contas.map(c => <option key={c.codigo} value={c.codigo}>{c.codigo} — {c.nome}</option>)}
                </select>
              </div>
            </div>

            <div>
              <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Valor (AOA)</label>
              <input type="number" value={form.valor} onChange={set('valor')} placeholder="0.00"
                className="w-full h-8 px-3 text-[13px] text-right border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
                style={{ fontFamily: 'JetBrains Mono, monospace' }} />
            </div>
          </div>

          <div className="flex items-center justify-end gap-2 px-5 py-3.5 border-t border-[#E2E8F0]">
            <button onClick={onClose} className="h-8 px-3 bg-white border border-[#E2E8F0] hover:bg-[#F8FAFC] text-[#0F172A] text-[13px] font-medium rounded-md transition-colors">
              Cancelar
            </button>
            <button onClick={handleSave} disabled={salvando} className="flex items-center gap-1.5 h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-[13px] font-medium rounded-md transition-colors disabled:opacity-50">
              {salvando ? <Loader2 className="animate-spin" style={{ width: 13, height: 13 }} /> : <Check style={{ width: 13, height: 13 }} />} Registar
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

export function Lancamentos() {
  const { perfil } = useAuth();
  const podeEscrever = permissoes(perfil).podeEscreverLancamentos; // RN010: Auditor não escreve
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('todos');
  const [origemFilter, setOrigemFilter] = useState('todos');
  const [contaFilter, setContaFilter] = useState('');
  const [utilizadorFilter, setUtilizadorFilter] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [lancamentos, setLancamentos] = useState<LancamentoResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [exportando, setExportando] = useState(false);
  const [periodo, setPeriodo] = useState({ inicio: inicioDoAno(), fim: hoje() });
  const [contas, setContas] = useState<ContaResumo[]>([]);

  useEffect(() => {
    listarContas()
      .then(setContas)
      .catch(err => console.error('Erro ao carregar plano de contas:', err));
  }, []);

  const carregar = async () => {
    try {
      setLoading(true);
      setErro(null);
      const dados = await listarLancamentos();
      setLancamentos(dados);
    } catch (err) {
      console.error(err);
      setErro('Não foi possível carregar os lançamentos.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregar();
  }, []);

  const handleExportar = async () => {
    try {
      setExportando(true);
      await exportarLancamentos(
        periodo.inicio,
        periodo.fim,
        statusFilter !== 'todos' ? statusFilter : undefined
      );
      toast.success('Ficheiro Excel gerado');
    } catch (err) {
      console.error(err);
      toast.error('Erro ao exportar lançamentos');
    } finally {
      setExportando(false);
    }
  };

  // Fase 9 do plano de 20 fases — histórico com pesquisa/filtro por
  // período, conta, entidade, origem, utilizador e estado. Antes desta
  // fase, o período dos inputs acima só era usado na exportação Excel —
  // a tabela mostrava sempre todos os lançamentos, sem filtrar por data.
  const utilizadoresDisponiveis = Array.from(
    new Set(lancamentos.map(l => l.validadoPorNome).filter((nome): nome is string => !!nome))
  ).sort();

  const filtered = lancamentos.filter(l => {
    const termo = search.toLowerCase();
    const matchSearch = search === '' ||
      l.descricao.toLowerCase().includes(termo) ||
      (l.entidadeNome ?? '').toLowerCase().includes(termo);
    const matchStatus = statusFilter === 'todos' || l.estado === statusFilter;
    const matchOrigem = origemFilter === 'todos' ||
      (origemFilter === 'ia' ? l.origem === 'AUTOMATICO' : l.origem === 'MANUAL');
    const matchPeriodo = (!periodo.inicio || l.data >= periodo.inicio) && (!periodo.fim || l.data <= periodo.fim);
    const matchConta = contaFilter === '' || l.linhas.some(linha => linha.conta === contaFilter);
    const matchUtilizador = utilizadorFilter === '' || l.validadoPorNome === utilizadorFilter;
    return matchSearch && matchStatus && matchOrigem && matchPeriodo && matchConta && matchUtilizador;
  });

  const counts = {
    total: lancamentos.length,
    aprovados: lancamentos.filter(l => l.estado === 'VALIDADO').length,
    pendentes: lancamentos.filter(l => l.estado === 'PENDENTE').length,
    rejeitados: lancamentos.filter(l => l.estado === 'CANCELADO').length,
  };

  return (
    <div className="max-w-[1200px] space-y-4">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-[18px] font-semibold text-[#0F172A]">Histórico de Lançamentos</h1>
          <p className="text-[13px] text-[#475569] mt-0.5">{counts.total} lançamentos</p>
        </div>
        <div className="flex items-center gap-2">
          <input
            type="date"
            value={periodo.inicio}
            onChange={e => setPeriodo(prev => ({ ...prev, inicio: e.target.value }))}
            className="h-8 px-2 text-[12px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A]"
          />
          <span className="text-[#94A3B8] text-[12px]">até</span>
          <input
            type="date"
            value={periodo.fim}
            onChange={e => setPeriodo(prev => ({ ...prev, fim: e.target.value }))}
            className="h-8 px-2 text-[12px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A]"
          />
          <button
            onClick={handleExportar}
            disabled={exportando}
            className="flex items-center gap-1.5 h-8 px-3 bg-white border border-[#E2E8F0] hover:bg-[#F8FAFC] text-[#0F172A] text-[13px] font-medium rounded-md transition-colors disabled:opacity-50"
          >
            {exportando ? <Loader2 className="animate-spin" style={{ width: 13, height: 13 }} /> : <Download style={{ width: 13, height: 13 }} />}
            Exportar Excel
          </button>
          {podeEscrever && (
            <button
              onClick={() => setDialogOpen(true)}
              className="flex items-center gap-1.5 h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-[13px] font-medium rounded-md transition-colors"
            >
              <Plus style={{ width: 13, height: 13 }} /> Novo Lançamento
            </button>
          )}
        </div>
      </div>

      {/* Summary pills */}
      <div className="flex items-center gap-3 text-[12px]">
        <div className="flex items-center gap-1.5 text-[#059669]">
          <CheckCircle2 style={{ width: 12, height: 12 }} />
          <span>{counts.aprovados} validados</span>
        </div>
        <div className="flex items-center gap-1.5 text-[#D97706]">
          <Clock style={{ width: 12, height: 12 }} />
          <span>{counts.pendentes} pendentes</span>
        </div>
        <div className="flex items-center gap-1.5 text-[#DC2626]">
          <XCircle style={{ width: 12, height: 12 }} />
          <span>{counts.rejeitados} cancelados</span>
        </div>
      </div>

      {/* Filters inline */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg px-4 py-3 flex items-center gap-3 flex-wrap">
        <div className="relative flex-1 min-w-[180px]">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 text-[#94A3B8]" style={{ width: 13, height: 13 }} />
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Pesquisar descrição ou entidade..."
            className="w-full h-8 pl-8 pr-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
          />
        </div>
        <select
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value as StatusFilter)}
          className="h-8 px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all"
        >
          <option value="todos">Todos os estados</option>
          <option value="VALIDADO">Validado</option>
          <option value="PENDENTE">Pendente</option>
          <option value="CANCELADO">Cancelado</option>
        </select>
        <select
          value={origemFilter}
          onChange={e => setOrigemFilter(e.target.value)}
          className="h-8 px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all"
        >
          <option value="todos">Todas as origens</option>
          <option value="ia">IA</option>
          <option value="manual">Manual</option>
        </select>
        <select
          value={contaFilter}
          onChange={e => setContaFilter(e.target.value)}
          className="h-8 px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all"
          style={{ fontFamily: 'JetBrains Mono, monospace' }}
        >
          <option value="" style={{ fontFamily: 'Inter, system-ui, sans-serif' }}>Todas as contas</option>
          {contas.map(c => <option key={c.codigo} value={c.codigo}>{c.codigo} — {c.nome}</option>)}
        </select>
        {utilizadoresDisponiveis.length > 0 && (
          <select
            value={utilizadorFilter}
            onChange={e => setUtilizadorFilter(e.target.value)}
            className="h-8 px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all"
          >
            <option value="">Todos os utilizadores</option>
            {utilizadoresDisponiveis.map(nome => <option key={nome} value={nome}>{nome}</option>)}
          </select>
        )}
      </div>

      {/* Table */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                {['Data', 'Descrição', 'Débito', 'Crédito', 'Valor (AOA)', 'Origem', 'Estado', 'Acções'].map(h => (
                  <th key={h} className={`px-4 py-2.5 text-[11px] font-medium text-[#475569] uppercase tracking-wide whitespace-nowrap ${h === 'Valor (AOA)' || h === 'Acções' ? 'text-right' : 'text-left'}`}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} className="px-4 py-8 text-center text-[13px] text-[#94A3B8]">
                    <Loader2 className="animate-spin inline-block mr-2" size={14} /> A carregar...
                  </td>
                </tr>
              ) : erro ? (
                <tr>
                  <td colSpan={8} className="px-4 py-8 text-center text-[13px] text-[#DC2626]">
                    {erro}
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-4 py-8 text-center text-[13px] text-[#94A3B8]">
                    Nenhum lançamento encontrado
                  </td>
                </tr>
              ) : filtered.map(l => {
                const estadoInfo = estadoBadge[l.estado] ?? { variant: 'pendente' as const, label: l.estado };
                return (
                <tr key={l.id} className="border-b border-[#F1F5F9] hover:bg-[#F8FAFC] transition-colors">
                  <td className="px-4 py-2.5 text-[13px] text-[#475569] whitespace-nowrap">
                    {new Date(l.data).toLocaleDateString('pt-AO')}
                  </td>
                  <td className="px-4 py-2.5 text-[13px] text-[#475569] max-w-[200px] truncate">{l.descricao}</td>
                  <td className="px-4 py-2.5 text-[13px] text-[#475569] whitespace-nowrap" style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                    {contaDebito(l)}
                  </td>
                  <td className="px-4 py-2.5 text-[13px] text-[#475569] whitespace-nowrap" style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                    {contaCredito(l)}
                  </td>
                  <td className="px-4 py-2.5 text-[13px] font-medium text-[#0F172A] text-right whitespace-nowrap" style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                    {formatarKwanza(valorTotal(l))}
                  </td>
                  <td className="px-4 py-2.5 whitespace-nowrap">
                    <div className="flex items-center gap-1">
                      <Badge variant={l.origem === 'AUTOMATICO' ? 'ia' : 'manual'}>
                        {l.origem === 'AUTOMATICO' ? '✦ IA' : 'Manual'}
                      </Badge>
                      {l.origem === 'AUTOMATICO' && l.editadoManualmente && (
                        <Badge variant="pendente">Editado</Badge>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-2.5 whitespace-nowrap">
                    <Badge variant={estadoInfo.variant}>
                      {estadoInfo.label}
                    </Badge>
                  </td>
                  <td className="px-4 py-2.5 text-right">
                    <div className="flex items-center justify-end gap-1">
                      <button title="Ver" className="w-6 h-6 flex items-center justify-center text-[#64748B] hover:text-[#0F172A] hover:bg-[#F1F5F9] rounded transition-colors">
                        <Eye style={{ width: 13, height: 13 }} />
                      </button>
                      {podeEscrever && (
                        <button title="Editar" className="w-6 h-6 flex items-center justify-center text-[#64748B] hover:text-[#0F172A] hover:bg-[#F1F5F9] rounded transition-colors">
                          <Edit2 style={{ width: 13, height: 13 }} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              );})}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between px-4 py-3 border-t border-[#E2E8F0]">
          <p className="text-[12px] text-[#475569]">Mostrando {filtered.length} de {counts.total} lançamentos</p>
        </div>
      </div>

      <NovoLancamentoDialog open={dialogOpen} onClose={() => setDialogOpen(false)} onCriado={carregar} contas={contas} />
    </div>
  );
}
