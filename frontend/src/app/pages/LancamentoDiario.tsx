import { useEffect, useMemo, useState } from 'react';
import { Plus, Trash2, CheckCircle2, AlertCircle, Loader2, Percent, LayoutGrid } from 'lucide-react';
import { formatarKwanza } from '../data/mockData';
import { criarLancamento } from '../api/lancamentoApi';
import { listarContas } from '../api/contaApi';
import { listarCategoriasConta } from '../api/categoriaContaApi';
import type { CategoriaConta, ContaResumo } from '../types/categoriaConta';
import { toast } from 'sonner';

interface Linha {
  id: string;
  conta: string;
  descricao: string;
  debito: string;
  credito: string;
}

// Plano de contas real do PGC-AO (Decreto n.º 82/01), o mesmo usado pela
// classificação automática (services/pgc.py) — antes esta página usava um
// plano de contas genérico de exemplo (mockData.planoContasAngolano), com
// numeração diferente da que os lançamentos reais realmente usam (ex. sem
// as contas de IVA 34.5.1/34.5.2, ou as sub-contas de FSE como Água/
// Electricidade), o que na prática impedia registar um lançamento com
// linhas para essas contas.
type ContaSimples = ContaResumo;

const hoje = () => new Date().toISOString().slice(0, 10);

const novaLinha = (): Linha => ({ id: crypto.randomUUID(), conta: '', descricao: '', debito: '', credito: '' });

// Taxas de IVA em vigor em Angola: 14% (taxa geral) e 7% (taxa reduzida,
// bens de primeira necessidade) — ver services/pgc.py (TAXA_IVA) no lado
// da classificação automática, que hoje só aplica a taxa geral. Aqui, no
// lançamento manual, o utilizador escolhe qual das duas se aplica.
type TaxaIva = 'nenhuma' | '14' | '7';
type TipoIva = 'venda' | 'compra';

export function LancamentoDiario() {
  const [data, setData] = useState(hoje());
  const [nfDoc, setNfDoc] = useState('');
  const [nif, setNif] = useState('');
  const [tipoDoc, setTipoDoc] = useState('Fatura');
  const [historico, setHistorico] = useState('');
  const [linhas, setLinhas] = useState<Linha[]>([novaLinha(), novaLinha()]);
  const [aGuardar, setAGuardar] = useState(false);
  const [contas, setContas] = useState<ContaSimples[]>([]);
  const [categorias, setCategorias] = useState<CategoriaConta[]>([]);
  const [categoriaSelecionada, setCategoriaSelecionada] = useState('');

  useEffect(() => {
    listarContas()
      .then(dados => setContas(dados as unknown as ContaSimples[]))
      .catch(err => {
        console.error('Erro ao carregar plano de contas:', err);
        toast.error('Não foi possível carregar o plano de contas');
      });

    listarCategoriasConta()
      .then(setCategorias)
      .catch(err => console.error('Erro ao carregar categorias de conta:', err));
    // Falha a carregar categorias não é bloqueante — a pesquisa global de
    // contas (sem agrupamento) continua disponível.
  }, []);

  const categoriaAtual = categorias.find(c => c.nome === categoriaSelecionada);

  // Grupos para o <select> de cada linha: com categoria escolhida,
  // Principais/Ocasionais primeiro, depois "Outras contas" com o resto do
  // plano — nenhuma conta fica escondida, só reordenada visualmente.
  const gruposDeConta = useMemo(() => {
    if (!categoriaAtual) {
      return [{ titulo: '', contas }];
    }
    const codigosUsados = new Set([
      ...categoriaAtual.principais.map(c => c.codigo),
      ...categoriaAtual.ocasionais.map(c => c.codigo),
    ]);
    const outras = contas.filter(c => !codigosUsados.has(c.codigo));
    return [
      { titulo: 'Principais', contas: categoriaAtual.principais },
      { titulo: 'Ocasionais', contas: categoriaAtual.ocasionais },
      { titulo: 'Outras contas', contas: outras },
    ].filter(g => g.contas.length > 0);
  }, [categoriaAtual, contas]);

  // Assistente de IVA — só calcula e acrescenta uma linha às já existentes,
  // não substitui a edição manual das restantes.
  const [baseIva, setBaseIva] = useState('');
  const [taxaIva, setTaxaIva] = useState<TaxaIva>('nenhuma');
  const [tipoIva, setTipoIva] = useState<TipoIva>('venda');

  const totalD = linhas.reduce((s, l) => s + (parseFloat(l.debito) || 0), 0);
  const totalC = linhas.reduce((s, l) => s + (parseFloat(l.credito) || 0), 0);
  const equilibrado = Math.abs(totalD - totalC) < 0.01 && totalD > 0;

  const addLinha = () => setLinhas(prev => [...prev, novaLinha()]);
  const removeLinha = (id: string) => setLinhas(prev => prev.filter(l => l.id !== id));
  const updateLinha = (id: string, field: keyof Linha, value: string) =>
    setLinhas(prev => prev.map(l => l.id === id ? { ...l, [field]: value } : l));

  const valorIvaCalculado = taxaIva !== 'nenhuma' && baseIva
    ? (parseFloat(baseIva) || 0) * (Number(taxaIva) / 100)
    : 0;

  const adicionarLinhaDeIva = () => {
    if (taxaIva === 'nenhuma' || !baseIva || valorIvaCalculado <= 0) {
      toast.error('Indica a base tributável e a taxa de IVA');
      return;
    }
    const contaIva = tipoIva === 'venda' ? '34.5.2' : '34.5.1';
    const nomeIva = tipoIva === 'venda' ? 'IVA liquidado' : 'IVA dedutível';
    const linha: Linha = {
      id: crypto.randomUUID(),
      conta: contaIva,
      descricao: `${nomeIva} (${taxaIva}%) — extensão do projeto, Decreto 82/01 não define conta oficial`,
      debito: tipoIva === 'compra' ? valorIvaCalculado.toFixed(2) : '',
      credito: tipoIva === 'venda' ? valorIvaCalculado.toFixed(2) : '',
    };
    setLinhas(prev => [...prev, linha]);
    toast.success(`Linha de IVA (${taxaIva}%) adicionada — ${formatarKwanza(valorIvaCalculado)}`);
    setBaseIva('');
  };

  const handleSave = async () => {
    if (!equilibrado) {
      toast.error('Lançamento desequilibrado');
      return;
    }
    if (!historico.trim()) {
      toast.error('Preenche o histórico do lançamento');
      return;
    }

    setAGuardar(true);
    try {
      await criarLancamento({
        data,
        descricao: historico,
        linhas: linhas
          .filter(l => l.conta && (l.debito || l.credito))
          .map(l => ({
            conta: l.conta,
            debito: parseFloat(l.debito) || 0,
            credito: parseFloat(l.credito) || 0,
            descricao: l.descricao || historico,
          })),
      });
      toast.success('Lançamento diário registado');
      setLinhas([novaLinha(), novaLinha()]);
      setNfDoc('');
      setNif('');
      setHistorico('');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Erro ao registar lançamento');
    } finally {
      setAGuardar(false);
    }
  };

  return (
    <div className="max-w-[900px] space-y-4">
      <div>
        <h1 className="text-[18px] font-semibold text-[#0F172A]">Lançamento Diário</h1>
        <p className="text-[13px] text-[#475569] mt-0.5">Registo manual · Método das partidas dobradas</p>
      </div>

      <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
        <div className="px-4 py-3 border-b border-[#E2E8F0]">
          <h2 className="text-[13px] font-semibold text-[#0F172A]">Dados do Documento</h2>
        </div>
        <div className="p-4 grid grid-cols-2 lg:grid-cols-4 gap-3">
          <div>
            <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Data *</label>
            <input type="date" value={data} onChange={e => setData(e.target.value)}
              className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all" />
          </div>
          <div>
            <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Nº Documento *</label>
            <input type="text" value={nfDoc} onChange={e => setNfDoc(e.target.value)} placeholder="FT 001/2026"
              className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all" />
          </div>
          <div>
            <label className="block text-[12px] font-medium text-[#475569] mb-1.5">NIF Emitente *</label>
            <input type="text" value={nif} onChange={e => setNif(e.target.value)} placeholder="5000123456LA"
              className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
              style={{ fontFamily: 'JetBrains Mono, monospace' }} />
          </div>
          <div>
            <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Tipo de Documento</label>
            <select value={tipoDoc} onChange={e => setTipoDoc(e.target.value)}
              className="w-full h-8 px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all">
              {['Fatura', 'Fatura-recibo', 'Nota de crédito', 'Recibo', 'Outro'].map(t => (
                <option key={t}>{t}</option>
              ))}
            </select>
          </div>
          <div className="col-span-2 lg:col-span-4">
            <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Histórico *</label>
            <input type="text" value={historico} onChange={e => setHistorico(e.target.value)}
              placeholder="Descrição do lançamento..."
              className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all" />
          </div>
        </div>
      </div>

      {/* Categoria — filtra/agrupa as contas mostradas nas linhas abaixo;
          a pesquisa pelo plano de contas completo continua sempre disponível
          em "Outras contas". */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
        <div className="px-4 py-3 border-b border-[#E2E8F0] flex items-center gap-2">
          <LayoutGrid style={{ width: 14, height: 14 }} className="text-[#2563EB]" />
          <h2 className="text-[13px] font-semibold text-[#0F172A]">Categoria da operação</h2>
        </div>
        <div className="p-4">
          <select
            value={categoriaSelecionada}
            onChange={e => setCategoriaSelecionada(e.target.value)}
            className="w-full sm:w-64 h-8 px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all"
          >
            <option value="">Todas as contas (sem filtro)</option>
            {categorias.map(c => <option key={c.nome} value={c.nome}>{c.nome}</option>)}
          </select>
          <p className="text-[11px] text-[#94A3B8] mt-1.5">
            Escolhe uma categoria para veres primeiro as contas mais usadas nesse tipo de operação.
          </p>
        </div>
      </div>

      {/* Assistente de IVA */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
        <div className="px-4 py-3 border-b border-[#E2E8F0] flex items-center gap-2">
          <Percent style={{ width: 14, height: 14 }} className="text-[#7C3AED]" />
          <h2 className="text-[13px] font-semibold text-[#0F172A]">Calcular linha de IVA</h2>
        </div>
        <div className="p-4 grid grid-cols-2 lg:grid-cols-4 gap-3 items-end">
          <div>
            <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Base tributável (sem IVA)</label>
            <input
              type="number"
              value={baseIva}
              onChange={e => setBaseIva(e.target.value)}
              placeholder="0"
              className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
              style={{ fontFamily: 'JetBrains Mono, monospace' }}
            />
          </div>
          <div>
            <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Taxa de IVA</label>
            <select value={taxaIva} onChange={e => setTaxaIva(e.target.value as TaxaIva)}
              className="w-full h-8 px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all">
              <option value="nenhuma">Sem IVA</option>
              <option value="14">14% (taxa geral)</option>
              <option value="7">7% (taxa reduzida)</option>
            </select>
          </div>
          <div>
            <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Operação</label>
            <select value={tipoIva} onChange={e => setTipoIva(e.target.value as TipoIva)}
              className="w-full h-8 px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all">
              <option value="venda">Venda (34.5.2 · IVA liquidado)</option>
              <option value="compra">Compra (34.5.1 · IVA dedutível)</option>
            </select>
          </div>
          <button
            type="button"
            onClick={adicionarLinhaDeIva}
            className="h-8 px-3 bg-[#F5F3FF] hover:bg-[#EDE9FE] text-[#7C3AED] text-[13px] font-medium rounded-md transition-colors"
          >
            {valorIvaCalculado > 0 ? `Adicionar ${formatarKwanza(valorIvaCalculado)}` : 'Adicionar linha de IVA'}
          </button>
        </div>
      </div>

      {/* Lines */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
        <div className="px-4 py-3 border-b border-[#E2E8F0] flex items-center justify-between">
          <h2 className="text-[13px] font-semibold text-[#0F172A]">Linhas de Lançamento</h2>
          <button onClick={addLinha} className="flex items-center gap-1 text-[12px] text-[#2563EB] hover:text-[#1D4ED8] transition-colors">
            <Plus style={{ width: 13, height: 13 }} /> Adicionar linha
          </button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                {['Conta', 'Descrição', 'Débito (AOA)', 'Crédito (AOA)', ''].map(h => (
                  <th key={h} className={`px-3 py-2.5 text-[11px] font-medium text-[#475569] uppercase tracking-wide ${['Débito (AOA)','Crédito (AOA)'].includes(h) ? 'text-right' : 'text-left'}`}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {linhas.map(linha => (
                <tr key={linha.id} className="border-b border-[#F1F5F9]">
                  <td className="px-3 py-2 w-48">
                    <select
                      value={linha.conta}
                      onChange={e => updateLinha(linha.id, 'conta', e.target.value)}
                      className="w-full h-8 px-2 text-[12px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all"
                      style={{ fontFamily: 'JetBrains Mono, monospace' }}
                    >
                      <option value="">Seleccionar conta</option>
                      {gruposDeConta.map(grupo => grupo.titulo ? (
                        <optgroup key={grupo.titulo} label={grupo.titulo}>
                          {grupo.contas.map(c => <option key={c.codigo} value={c.codigo}>{c.codigo} — {c.nome}</option>)}
                        </optgroup>
                      ) : (
                        grupo.contas.map(c => <option key={c.codigo} value={c.codigo}>{c.codigo} — {c.nome}</option>)
                      ))}
                    </select>
                  </td>
                  <td className="px-3 py-2">
                    <input
                      value={linha.descricao}
                      onChange={e => updateLinha(linha.id, 'descricao', e.target.value)}
                      placeholder="Descrição da linha..."
                      className="w-full h-8 px-2 text-[12px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] transition-all"
                    />
                  </td>
                  <td className="px-3 py-2 w-36">
                    <input
                      value={linha.debito}
                      onChange={e => updateLinha(linha.id, 'debito', e.target.value)}
                      placeholder="0"
                      type="number"
                      className="w-full h-8 px-2 text-[12px] text-right border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] transition-all"
                      style={{ fontFamily: 'JetBrains Mono, monospace' }}
                    />
                  </td>
                  <td className="px-3 py-2 w-36">
                    <input
                      value={linha.credito}
                      onChange={e => updateLinha(linha.id, 'credito', e.target.value)}
                      placeholder="0"
                      type="number"
                      className="w-full h-8 px-2 text-[12px] text-right border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] transition-all"
                      style={{ fontFamily: 'JetBrains Mono, monospace' }}
                    />
                  </td>
                  <td className="px-3 py-2 w-10">
                    {linhas.length > 2 && (
                      <button onClick={() => removeLinha(linha.id)} className="w-6 h-6 flex items-center justify-center text-[#94A3B8] hover:text-[#DC2626] hover:bg-[#FEF2F2] rounded transition-colors">
                        <Trash2 style={{ width: 13, height: 13 }} />
                      </button>
                    )}
                  </td>
                </tr>
              ))}

              {/* Totals row */}
              <tr className="bg-[#F8FAFC] border-t border-[#E2E8F0]">
                <td className="px-3 py-2.5 text-[12px] font-semibold text-[#475569]" colSpan={2}>
                  <div className="flex items-center gap-2">
                    <span>Totais</span>
                    {totalD > 0 && (equilibrado
                      ? <span className="inline-flex items-center gap-1 text-[#059669] text-[11px]"><CheckCircle2 style={{ width: 11, height: 11 }} /> Equilibrado</span>
                      : <span className="inline-flex items-center gap-1 text-[#DC2626] text-[11px]"><AlertCircle style={{ width: 11, height: 11 }} /> Desequilibrado</span>
                    )}
                  </div>
                </td>
                <td className="px-3 py-2.5 text-[13px] font-semibold text-[#059669] text-right" style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                  {totalD > 0 ? formatarKwanza(totalD) : '—'}
                </td>
                <td className="px-3 py-2.5 text-[13px] font-semibold text-[#7C3AED] text-right" style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                  {totalC > 0 ? formatarKwanza(totalC) : '—'}
                </td>
                <td />
              </tr>
            </tbody>
          </table>
        </div>

        <div className="px-4 py-3 border-t border-[#E2E8F0] flex items-center gap-2">
          <button
            onClick={handleSave}
            disabled={!equilibrado || aGuardar}
            className="flex items-center gap-1.5 h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] disabled:opacity-40 text-white text-[13px] font-medium rounded-md transition-colors"
          >
            {aGuardar ? <Loader2 className="animate-spin" size={13} /> : null}
            Registar Lançamento
          </button>
          <button
            onClick={() => { setLinhas([novaLinha(), novaLinha()]); setNfDoc(''); setNif(''); setHistorico(''); }}
            className="flex items-center gap-1.5 h-8 px-3 bg-white border border-[#E2E8F0] hover:bg-[#F8FAFC] text-[#0F172A] text-[13px] font-medium rounded-md transition-colors"
          >
            Limpar
          </button>
        </div>
      </div>
    </div>
  );
}
