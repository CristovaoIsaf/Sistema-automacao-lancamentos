import { useEffect, useState } from 'react';
import { Download, CheckCircle2, AlertCircle, Calendar, Loader2 } from 'lucide-react';
import { formatarKwanza } from '../data/mockData';
import { obterBalancete } from '../api/balanceteApi';
import type { BalanceteResponse } from '../types/balancete';

export function Balancetes() {
  const [periodo, setPeriodo] = useState('mes-atual');
  const [balancete, setBalancete] = useState<BalanceteResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let cancelado = false;
    setLoading(true);
    setErro(null);

    const { inicio, fim } = intervaloDoPeriodo(periodo);

    obterBalancete(inicio, fim)
      .then((dados) => {
        if (!cancelado) setBalancete(dados);
      })
      .catch((e) => {
        if (cancelado) return;
        console.error('Erro ao carregar balancete:', e);
        setErro('Não foi possível carregar o balancete.');
      })
      .finally(() => {
        if (!cancelado) setLoading(false);
      });

    return () => {
      cancelado = true;
    };
  }, [periodo]);

  return (
    <div className="max-w-[1200px] space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-[18px] font-semibold text-[#0F172A]">Balancete de Verificação</h1>
          <p className="text-[13px] text-[#475569] mt-0.5">PGCA · Decreto n.º 82/01</p>
        </div>
        <div className="flex items-center gap-2">
          {balancete && (
            <div className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-medium ${balancete.equilibrado ? 'bg-[#ECFDF5] text-[#059669]' : 'bg-[#FEF2F2] text-[#DC2626]'}`}>
              {balancete.equilibrado
                ? <><CheckCircle2 style={{ width: 12, height: 12 }} /> Balancete Equilibrado</>
                : <><AlertCircle style={{ width: 12, height: 12 }} /> Desequilibrado</>}
            </div>
          )}
          <button className="flex items-center gap-1.5 h-8 px-3 bg-white border border-[#E2E8F0] hover:bg-[#F8FAFC] text-[#0F172A] text-[13px] font-medium rounded-md transition-colors">
            <Download style={{ width: 13, height: 13 }} /> Exportar
          </button>
        </div>
      </div>

      {/* Period selector */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg px-4 py-3 flex items-center gap-3">
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
            <Loader2 className="animate-spin" size={16} /> A carregar balancete...
          </div>
        ) : !balancete || balancete.linhas.length === 0 ? (
          <div className="py-10 text-center text-[13px] text-[#94A3B8]">
            Ainda não há lançamentos validados para gerar o balancete.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-[12px]">
              <thead>
                <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                  <th className="px-3 py-2.5 text-left text-[11px] font-medium text-[#475569] uppercase tracking-wide">Conta</th>
                  <th className="px-3 py-2.5 text-left text-[11px] font-medium text-[#475569] uppercase tracking-wide">Designação</th>
                  <th className="px-3 py-2.5 text-right text-[11px] font-medium text-[#475569] uppercase tracking-wide border-l border-[#E2E8F0]">Débito Acumulado</th>
                  <th className="px-3 py-2.5 text-right text-[11px] font-medium text-[#475569] uppercase tracking-wide">Crédito Acumulado</th>
                  <th className="px-3 py-2.5 text-right text-[11px] font-medium text-[#475569] uppercase tracking-wide border-l border-[#E2E8F0]">Saldo Devedor</th>
                  <th className="px-3 py-2.5 text-right text-[11px] font-medium text-[#475569] uppercase tracking-wide">Saldo Credor</th>
                </tr>
              </thead>
              <tbody>
                {balancete.linhas.map((row, i) => (
                  <tr key={row.conta} className={`border-b border-[#F1F5F9] hover:bg-[#F8FAFC] transition-colors ${i % 2 === 1 ? 'bg-white' : ''}`}>
                    <td className="px-3 py-2.5 font-medium text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.conta}</td>
                    <td className="px-3 py-2.5 text-[#475569]">{row.nome}</td>
                    <td className="px-3 py-2.5 text-right text-[#0F172A] border-l border-[#F1F5F9]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.debitoAcumulado ? formatarKwanza(row.debitoAcumulado) : '—'}</td>
                    <td className="px-3 py-2.5 text-right text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.creditoAcumulado ? formatarKwanza(row.creditoAcumulado) : '—'}</td>
                    <td className="px-3 py-2.5 text-right text-[#059669] border-l border-[#F1F5F9]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.saldoDevedor ? formatarKwanza(row.saldoDevedor) : '—'}</td>
                    <td className="px-3 py-2.5 text-right text-[#7C3AED]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.saldoCredor ? formatarKwanza(row.saldoCredor) : '—'}</td>
                  </tr>
                ))}
                <tr className="bg-[#F8FAFC] border-t-2 border-[#E2E8F0] font-semibold">
                  <td className="px-3 py-2.5 text-[#0F172A]" colSpan={2}>TOTAIS</td>
                  <td className="px-3 py-2.5 text-right border-l border-[#E2E8F0] text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(balancete.totalDebito)}</td>
                  <td className="px-3 py-2.5 text-right text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(balancete.totalCredito)}</td>
                  <td className="px-3 py-2.5 text-right border-l border-[#E2E8F0] text-[#059669]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(balancete.totalSaldoDevedor)}</td>
                  <td className="px-3 py-2.5 text-right text-[#7C3AED]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(balancete.totalSaldoCredor)}</td>
                </tr>
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

function intervaloDoPeriodo(periodo: string): { inicio?: string; fim?: string } {
  const hoje = new Date();
  const fim = hoje.toISOString().slice(0, 10);

  switch (periodo) {
    case 'mes-atual': {
      const inicio = new Date(hoje.getFullYear(), hoje.getMonth(), 1).toISOString().slice(0, 10);
      return { inicio, fim };
    }
    case 'trimestre': {
      const inicio = new Date(hoje.getFullYear(), hoje.getMonth() - 2, 1).toISOString().slice(0, 10);
      return { inicio, fim };
    }
    case 'ano': {
      const inicio = new Date(hoje.getFullYear(), 0, 1).toISOString().slice(0, 10);
      return { inicio, fim };
    }
    default:
      return {};
  }
}
