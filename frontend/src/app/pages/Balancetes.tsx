import { useEffect, useState } from 'react';
import { Download, CheckCircle2, AlertCircle, Calendar } from 'lucide-react';
import { planoContasAngolano, formatarKwanza } from '../data/mockData';

const balanceteData = [
  { conta: '1.1.1', nome: 'Caixa', debAnt: 0, credAnt: 0, debPer: 2500000, credPer: 1800000, debAcum: 2500000, credAcum: 1800000, saldoD: 700000, saldoC: 0 },
  { conta: '1.1.2', nome: 'Bancos Conta Movimento', debAnt: 5000000, credAnt: 0, debPer: 3200000, credPer: 2100000, debAcum: 8200000, credAcum: 2100000, saldoD: 6100000, saldoC: 0 },
  { conta: '1.1.3', nome: 'Clientes', debAnt: 1200000, credAnt: 0, debPer: 1500000, credPer: 900000, debAcum: 2700000, credAcum: 900000, saldoD: 1800000, saldoC: 0 },
  { conta: '2.1.1', nome: 'Fornecedores', debAnt: 0, credAnt: 800000, debPer: 600000, credPer: 1200000, debAcum: 600000, credAcum: 2000000, saldoD: 0, saldoC: 1400000 },
  { conta: '2.1.3', nome: 'IVA a Pagar', debAnt: 0, credAnt: 150000, debPer: 0, credPer: 178000, debAcum: 0, credAcum: 328000, saldoD: 0, saldoC: 328000 },
  { conta: '4.1.1', nome: 'Vendas de Mercadorias', debAnt: 0, credAnt: 3500000, debPer: 0, credPer: 1029000, debAcum: 0, credAcum: 4529000, saldoD: 0, saldoC: 4529000 },
  { conta: '5.1.1', nome: 'CMV', debAnt: 1800000, credAnt: 0, debPer: 600000, credPer: 0, debAcum: 2400000, credAcum: 0, saldoD: 2400000, saldoC: 0 },
  { conta: '5.1.2', nome: 'Salários e Ordenados', debAnt: 900000, credAnt: 0, debPer: 300000, credPer: 0, debAcum: 1200000, credAcum: 0, saldoD: 1200000, saldoC: 0 },
];

const totalDebAcum = balanceteData.reduce((s, r) => s + r.debAcum, 0);
const totalCredAcum = balanceteData.reduce((s, r) => s + r.credAcum, 0);
const totalSaldoD = balanceteData.reduce((s, r) => s + r.saldoD, 0);
const totalSaldoC = balanceteData.reduce((s, r) => s + r.saldoC, 0);
const equilibrado = Math.abs(totalDebAcum - totalCredAcum) < 1 && Math.abs(totalSaldoD - totalSaldoC) < 1;

export function Balancetes() {
  const [periodo, setPeriodo] = useState('mes-atual');

  return (
    <div className="max-w-[1200px] space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-[18px] font-semibold text-[#0F172A]">Balancete de Verificação</h1>
          <p className="text-[13px] text-[#475569] mt-0.5">PGCA · Decreto n.º 82/01</p>
        </div>
        <div className="flex items-center gap-2">
          <div className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-medium ${equilibrado ? 'bg-[#ECFDF5] text-[#059669]' : 'bg-[#FEF2F2] text-[#DC2626]'}`}>
            {equilibrado
              ? <><CheckCircle2 style={{ width: 12, height: 12 }} /> Balancete Equilibrado</>
              : <><AlertCircle style={{ width: 12, height: 12 }} /> Desequilibrado</>}
          </div>
          <button className="flex items-center gap-1.5 h-8 px-3 bg-white border border-[#E2E8F0] hover:bg-[#F8FAFC] text-[#0F172A] text-[13px] font-medium rounded-md transition-colors">
            <Download style={{ width: 13, height: 13 }} /> Exportar
          </button>
        </div>
      </div>

      {/* Period selector */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg px-4 py-3 flex items-center gap-3">
        <Calendar style={{ width: 14, height: 14 }} className="text-[#94A3B8]" />
        <label className="text-[12px] font-medium text-[#475569]">Período:</label>
        {['mes-atual', 'trimestre', 'ano', 'personalizado'].map(p => (
          <button
            key={p}
            onClick={() => setPeriodo(p)}
            className={`h-7 px-3 text-[12px] font-medium rounded-md transition-colors ${periodo === p ? 'bg-[#2563EB] text-white' : 'bg-white border border-[#E2E8F0] text-[#475569] hover:bg-[#F8FAFC]'}`}
          >
            {p === 'mes-atual' ? 'Mês actual' : p === 'trimestre' ? 'Trimestre' : p === 'ano' ? 'Ano' : 'Personalizado'}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-[12px]">
            <thead>
              <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                <th className="px-3 py-2.5 text-left text-[11px] font-medium text-[#475569] uppercase tracking-wide" rowSpan={2}>Conta</th>
                <th className="px-3 py-2.5 text-left text-[11px] font-medium text-[#475569] uppercase tracking-wide" rowSpan={2}>Designação</th>
                <th className="px-3 py-2 text-center text-[11px] font-medium text-[#475569] uppercase tracking-wide border-l border-[#E2E8F0]" colSpan={2}>Saldo Anterior</th>
                <th className="px-3 py-2 text-center text-[11px] font-medium text-[#475569] uppercase tracking-wide border-l border-[#E2E8F0]" colSpan={2}>Movimento Período</th>
                <th className="px-3 py-2 text-center text-[11px] font-medium text-[#475569] uppercase tracking-wide border-l border-[#E2E8F0]" colSpan={2}>Acumulado</th>
                <th className="px-3 py-2 text-center text-[11px] font-medium text-[#475569] uppercase tracking-wide border-l border-[#E2E8F0]" colSpan={2}>Saldo Final</th>
              </tr>
              <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                {['Débito', 'Crédito', 'Débito', 'Crédito', 'Débito', 'Crédito', 'Débito', 'Crédito'].map((h, i) => (
                  <th key={i} className={`px-3 py-1.5 text-right text-[10px] font-medium text-[#94A3B8] uppercase tracking-wide ${i === 0 || i === 2 || i === 4 || i === 6 ? 'border-l border-[#E2E8F0]' : ''}`}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {balanceteData.map((row, i) => (
                <tr key={row.conta} className={`border-b border-[#F1F5F9] hover:bg-[#F8FAFC] transition-colors ${i % 2 === 1 ? 'bg-white' : ''}`}>
                  <td className="px-3 py-2.5 font-medium text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.conta}</td>
                  <td className="px-3 py-2.5 text-[#475569]">{row.nome}</td>
                  <td className="px-3 py-2.5 text-right text-[#0F172A] border-l border-[#F1F5F9]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.debAnt ? formatarKwanza(row.debAnt) : '—'}</td>
                  <td className="px-3 py-2.5 text-right text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.credAnt ? formatarKwanza(row.credAnt) : '—'}</td>
                  <td className="px-3 py-2.5 text-right text-[#0F172A] border-l border-[#F1F5F9]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.debPer ? formatarKwanza(row.debPer) : '—'}</td>
                  <td className="px-3 py-2.5 text-right text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.credPer ? formatarKwanza(row.credPer) : '—'}</td>
                  <td className="px-3 py-2.5 text-right text-[#0F172A] border-l border-[#F1F5F9]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.debAcum ? formatarKwanza(row.debAcum) : '—'}</td>
                  <td className="px-3 py-2.5 text-right text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.credAcum ? formatarKwanza(row.credAcum) : '—'}</td>
                  <td className="px-3 py-2.5 text-right text-[#059669] border-l border-[#F1F5F9]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.saldoD ? formatarKwanza(row.saldoD) : '—'}</td>
                  <td className="px-3 py-2.5 text-right text-[#7C3AED]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{row.saldoC ? formatarKwanza(row.saldoC) : '—'}</td>
                </tr>
              ))}
              {/* Totals */}
              <tr className="bg-[#F8FAFC] border-t-2 border-[#E2E8F0] font-semibold">
                <td className="px-3 py-2.5 text-[#0F172A]" colSpan={2}>TOTAIS</td>
                <td className="px-3 py-2.5 text-right border-l border-[#E2E8F0]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>—</td>
                <td className="px-3 py-2.5 text-right" style={{ fontFamily: 'JetBrains Mono, monospace' }}>—</td>
                <td className="px-3 py-2.5 text-right border-l border-[#E2E8F0]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>—</td>
                <td className="px-3 py-2.5 text-right" style={{ fontFamily: 'JetBrains Mono, monospace' }}>—</td>
                <td className="px-3 py-2.5 text-right border-l border-[#E2E8F0] text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(totalDebAcum)}</td>
                <td className="px-3 py-2.5 text-right text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(totalCredAcum)}</td>
                <td className="px-3 py-2.5 text-right border-l border-[#E2E8F0] text-[#059669]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(totalSaldoD)}</td>
                <td className="px-3 py-2.5 text-right text-[#7C3AED]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(totalSaldoC)}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
