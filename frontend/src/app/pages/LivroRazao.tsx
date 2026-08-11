import { useEffect, useState } from 'react';
import { Calendar, Loader2, AlertCircle, BookOpen } from 'lucide-react';
import { formatarKwanza } from '../data/mockData';
import { intervaloDoPeriodo } from '../data/periodo';
import { listarContas } from '../api/contaApi';
import { obterLivroRazao } from '../api/livroRazaoApi';
import type { ContaResumo } from '../types/categoriaConta';
import type { LivroRazaoResponse } from '../types/livroRazao';

// Fase 18 do plano de 20 fases — "auditor: livro razão" (consulta por
// conta individual). Antes desta fase não existia nenhum endpoint nem
// página para isto — Relatorios.tsx (Fase 13) documentava explicitamente
// que ficava fora do âmbito. Balancete já mostra o agregado por conta
// (ver Balancetes.tsx); esta página mostra o detalhe movimento a
// movimento de UMA conta, com saldo acumulado.
export function LivroRazao() {
  const [contas, setContas] = useState<ContaResumo[]>([]);
  const [conta, setConta] = useState<string>('');
  const [periodo, setPeriodo] = useState('mes-atual');
  const [razao, setRazao] = useState<LivroRazaoResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    listarContas()
      .then((dados) => {
        setContas(dados);
        if (dados.length > 0) setConta(dados[0].codigo);
      })
      .catch((e) => console.error('Erro ao carregar plano de contas:', e));
  }, []);

  useEffect(() => {
    if (!conta) return;
    let cancelado = false;
    setLoading(true);
    setErro(null);

    const { inicio, fim } = intervaloDoPeriodo(periodo);

    obterLivroRazao(conta, inicio, fim)
      .then((dados) => {
        if (!cancelado) setRazao(dados);
      })
      .catch((e) => {
        if (cancelado) return;
        console.error('Erro ao carregar livro razão:', e);
        setErro('Não foi possível carregar o livro razão desta conta.');
      })
      .finally(() => {
        if (!cancelado) setLoading(false);
      });

    return () => {
      cancelado = true;
    };
  }, [conta, periodo]);

  return (
    <div className="max-w-[1200px] space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-[18px] font-semibold text-[#0F172A] flex items-center gap-2">
            <BookOpen style={{ width: 16, height: 16 }} /> Livro Razão
          </h1>
          <p className="text-[13px] text-[#475569] mt-0.5">PGCA · Decreto n.º 82/01 — movimentos por conta individual</p>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg px-4 py-3 flex items-center gap-3 flex-wrap">
        <label className="text-[12px] font-medium text-[#475569]">Conta:</label>
        <select
          value={conta}
          onChange={e => setConta(e.target.value)}
          className="h-8 px-2 text-[12px] bg-white border border-[#E2E8F0] rounded-md text-[#0F172A] focus:outline-none focus:ring-1 focus:ring-[#2563EB]"
          style={{ fontFamily: 'JetBrains Mono, monospace' }}
        >
          {contas.map(c => (
            <option key={c.codigo} value={c.codigo}>{c.codigo} — {c.nome}</option>
          ))}
        </select>

        <div className="w-px h-5 bg-[#E2E8F0]" />

        <Calendar style={{ width: 14, height: 14 }} className="text-[#94A3B8]" />
        <label className="text-[12px] font-medium text-[#475569]">Período:</label>
        {['mes-atual', 'trimestre', 'ano', 'todos'].map(p => (
          <button
            key={p}
            onClick={() => setPeriodo(p)}
            className={`h-7 px-3 text-[12px] font-medium rounded-md transition-colors ${periodo === p ? 'bg-[#2563EB] text-white' : 'bg-white border border-[#E2E8F0] text-[#475569] hover:bg-[#F8FAFC]'}`}
          >
            {p === 'mes-atual' ? 'Mês actual' : p === 'trimestre' ? 'Trimestre' : p === 'ano' ? 'Ano' : 'Tudo'}
          </button>
        ))}
      </div>

      {erro && (
        <div className="flex items-center gap-2 bg-[#FFFBEB] text-[#92400E] px-3 py-2 rounded-md text-[12px]">
          <AlertCircle style={{ width: 14, height: 14 }} />
          {erro}
        </div>
      )}

      {/* Table */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center gap-2 py-10 text-[13px] text-[#94A3B8]">
            <Loader2 className="animate-spin" size={16} /> A carregar livro razão...
          </div>
        ) : !razao || razao.movimentos.length === 0 ? (
          <div className="py-10 text-center text-[13px] text-[#94A3B8]">
            Sem movimentos validados nesta conta, no período selecionado.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <div className="px-4 py-3 border-b border-[#E2E8F0]">
              <h2 className="text-[13px] font-semibold text-[#0F172A]">{razao.conta} — {razao.nomeConta}</h2>
            </div>
            <table className="w-full text-[12px]">
              <thead>
                <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                  <th className="px-3 py-2.5 text-left text-[11px] font-medium text-[#475569] uppercase tracking-wide">Data</th>
                  <th className="px-3 py-2.5 text-left text-[11px] font-medium text-[#475569] uppercase tracking-wide">Descrição</th>
                  <th className="px-3 py-2.5 text-right text-[11px] font-medium text-[#475569] uppercase tracking-wide border-l border-[#E2E8F0]">Débito</th>
                  <th className="px-3 py-2.5 text-right text-[11px] font-medium text-[#475569] uppercase tracking-wide">Crédito</th>
                  <th className="px-3 py-2.5 text-right text-[11px] font-medium text-[#475569] uppercase tracking-wide border-l border-[#E2E8F0]">Saldo Acumulado</th>
                </tr>
              </thead>
              <tbody>
                {razao.movimentos.map((m, i) => (
                  <tr key={`${m.lancamentoId}-${i}`} className="border-b border-[#F1F5F9] hover:bg-[#F8FAFC] transition-colors">
                    <td className="px-3 py-2.5 text-[#64748B]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                      {m.data ? new Date(m.data).toLocaleDateString('pt-AO') : '—'}
                    </td>
                    <td className="px-3 py-2.5 text-[#475569]">{m.descricao ?? '—'}</td>
                    <td className="px-3 py-2.5 text-right text-[#0F172A] border-l border-[#F1F5F9]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{m.debito ? formatarKwanza(m.debito) : '—'}</td>
                    <td className="px-3 py-2.5 text-right text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{m.credito ? formatarKwanza(m.credito) : '—'}</td>
                    <td className={`px-3 py-2.5 text-right font-medium border-l border-[#F1F5F9] ${m.saldoAcumulado >= 0 ? 'text-[#059669]' : 'text-[#DC2626]'}`} style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(m.saldoAcumulado)}</td>
                  </tr>
                ))}
                <tr className="bg-[#F8FAFC] border-t-2 border-[#E2E8F0] font-semibold">
                  <td className="px-3 py-2.5 text-[#0F172A]" colSpan={2}>TOTAIS</td>
                  <td className="px-3 py-2.5 text-right border-l border-[#E2E8F0] text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(razao.totalDebito)}</td>
                  <td className="px-3 py-2.5 text-right text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(razao.totalCredito)}</td>
                  <td className="px-3 py-2.5 text-right border-l border-[#E2E8F0] text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(razao.saldoFinal)}</td>
                </tr>
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
