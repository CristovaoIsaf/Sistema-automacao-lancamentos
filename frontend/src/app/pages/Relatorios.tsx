import { useEffect, useState } from 'react';
import { Download, BarChart3, BookOpen, FileText, Scale } from 'lucide-react';
import { formatarKwanza, dadosGraficoMensal } from '../data/mockData';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line, Legend } from 'recharts';

type TabKey = 'balancete' | 'razao' | 'dre' | 'balanco';

const tabs: { key: TabKey; label: string; icon: React.ElementType }[] = [
  { key: 'balancete', label: 'Balancete', icon: Scale },
  { key: 'razao',     label: 'Livro Razão', icon: BookOpen },
  { key: 'dre',       label: 'DRE', icon: BarChart3 },
  { key: 'balanco',   label: 'Balanço', icon: FileText },
];

const dreData = dadosGraficoMensal.slice(-6).map(d => ({
  mes: d.mes,
  receitas: d.receitas,
  despesas: d.despesas,
  resultado: d.receitas - d.despesas,
}));

export function Relatorios() {
  const [activeTab, setActiveTab] = useState<TabKey>('dre');
  const [periodo, setPeriodo] = useState('mes-atual');

  return (
    <div className="max-w-[1200px] space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-[18px] font-semibold text-[#0F172A]">Relatórios Financeiros</h1>
          <p className="text-[13px] text-[#475569] mt-0.5">PGCA · Decreto n.º 82/01</p>
        </div>
        <button className="flex items-center gap-1.5 h-8 px-3 bg-white border border-[#E2E8F0] hover:bg-[#F8FAFC] text-[#0F172A] text-[13px] font-medium rounded-md transition-colors">
          <Download style={{ width: 13, height: 13 }} /> Exportar PDF
        </button>
      </div>

      {/* Period + Tab bar */}
      <div className="flex items-center gap-4 flex-wrap">
        <div className="flex items-center gap-1">
          {['mes-atual', 'trimestre', 'ano'].map(p => (
            <button
              key={p}
              onClick={() => setPeriodo(p)}
              className={`h-7 px-3 text-[12px] font-medium rounded-md transition-colors ${periodo === p ? 'bg-[#2563EB] text-white' : 'bg-white border border-[#E2E8F0] text-[#475569] hover:bg-[#F8FAFC]'}`}
            >
              {p === 'mes-atual' ? 'Mês actual' : p === 'trimestre' ? 'Trimestre' : 'Ano'}
            </button>
          ))}
        </div>
        <div className="flex items-center border border-[#E2E8F0] rounded-lg overflow-hidden bg-white">
          {tabs.map(tab => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`flex items-center gap-1.5 px-3 py-2 text-[12px] font-medium transition-colors ${
                  activeTab === tab.key
                    ? 'bg-[#EFF6FF] text-[#2563EB] border-b-2 border-[#2563EB]'
                    : 'text-[#475569] hover:bg-[#F8FAFC]'
                }`}
              >
                <Icon style={{ width: 13, height: 13 }} />
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Content */}
      {activeTab === 'dre' && (
        <div className="space-y-4">
          {/* DRE summary */}
          <div className="grid grid-cols-3 gap-3">
            {[
              { label: 'Receitas Totais', value: 4529000, color: 'text-[#059669]' },
              { label: 'Despesas Totais', value: 3600000, color: 'text-[#DC2626]' },
              { label: 'Resultado Líquido', value: 929000, color: 'text-[#2563EB]' },
            ].map(kpi => (
              <div key={kpi.label} className="bg-white border border-[#E2E8F0] rounded-lg p-4">
                <p className="text-[12px] font-medium text-[#475569] mb-2">{kpi.label}</p>
                <p className={`text-[22px] font-bold ${kpi.color}`} style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                  {formatarKwanza(kpi.value)}
                </p>
              </div>
            ))}
          </div>

          {/* Chart */}
          <div className="bg-white border border-[#E2E8F0] rounded-lg p-4">
            <h2 className="text-[13px] font-semibold text-[#0F172A] mb-4">Evolução — Receitas vs Despesas vs Resultado</h2>
            <ResponsiveContainer width="100%" height={240}>
              <LineChart data={dreData} margin={{ top: 0, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                <XAxis dataKey="mes" tick={{ fontSize: 11, fill: '#94A3B8' }} axisLine={false} tickLine={false} />
                <YAxis tickFormatter={v => `${(v / 1000000).toFixed(0)}M`} tick={{ fontSize: 11, fill: '#94A3B8' }} axisLine={false} tickLine={false} />
                <Tooltip formatter={(v: number) => formatarKwanza(v)} contentStyle={{ fontSize: 12, border: '1px solid #E2E8F0', borderRadius: 6, boxShadow: 'none' }} />
                <Legend wrapperStyle={{ fontSize: 12 }} />
                <Line type="monotone" dataKey="receitas" stroke="#059669" strokeWidth={1.5} dot={false} name="Receitas" />
                <Line type="monotone" dataKey="despesas" stroke="#DC2626" strokeWidth={1.5} dot={false} name="Despesas" />
                <Line type="monotone" dataKey="resultado" stroke="#2563EB" strokeWidth={1.5} dot={false} name="Resultado" strokeDasharray="4 2" />
              </LineChart>
            </ResponsiveContainer>
          </div>

          {/* DRE Table */}
          <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
            <div className="px-4 py-3 border-b border-[#E2E8F0]">
              <h2 className="text-[13px] font-semibold text-[#0F172A]">Demonstração de Resultados por Exercício</h2>
            </div>
            <table className="w-full">
              <thead>
                <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                  <th className="px-4 py-2.5 text-left text-[11px] font-medium text-[#475569] uppercase tracking-wide">Rubrica</th>
                  <th className="px-4 py-2.5 text-right text-[11px] font-medium text-[#475569] uppercase tracking-wide">Mês Actual</th>
                  <th className="px-4 py-2.5 text-right text-[11px] font-medium text-[#475569] uppercase tracking-wide">Acumulado</th>
                </tr>
              </thead>
              <tbody>
                {[
                  { rubrica: 'Vendas de Mercadorias', mes: 1029000, acum: 4200000, type: 'receita' },
                  { rubrica: 'Prestação de Serviços', mes: 0, acum: 329000, type: 'receita' },
                  { rubrica: 'TOTAL RECEITAS', mes: 1029000, acum: 4529000, type: 'subtotal' },
                  { rubrica: 'Custo das Mercadorias', mes: 600000, acum: 2400000, type: 'despesa' },
                  { rubrica: 'Salários e Ordenados', mes: 300000, acum: 900000, type: 'despesa' },
                  { rubrica: 'Outros gastos', mes: 100000, acum: 300000, type: 'despesa' },
                  { rubrica: 'TOTAL DESPESAS', mes: 1000000, acum: 3600000, type: 'subtotal' },
                  { rubrica: 'RESULTADO LÍQUIDO', mes: 29000, acum: 929000, type: 'total' },
                ].map(row => (
                  <tr key={row.rubrica} className={`border-b ${row.type === 'total' ? 'bg-[#EFF6FF] border-[#BFDBFE]' : row.type === 'subtotal' ? 'bg-[#F8FAFC] border-[#E2E8F0]' : 'border-[#F1F5F9] hover:bg-[#F8FAFC]'} transition-colors`}>
                    <td className={`px-4 py-2.5 text-[13px] ${row.type === 'total' || row.type === 'subtotal' ? 'font-semibold text-[#0F172A]' : 'text-[#475569]'} ${row.type !== 'subtotal' && row.type !== 'total' ? 'pl-8' : ''}`}>{row.rubrica}</td>
                    <td className={`px-4 py-2.5 text-[13px] text-right ${row.type === 'total' ? 'text-[#2563EB] font-semibold' : row.type === 'subtotal' ? 'font-semibold text-[#0F172A]' : row.type === 'receita' ? 'text-[#059669]' : 'text-[#DC2626]'}`} style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                      {row.mes ? formatarKwanza(row.mes) : '—'}
                    </td>
                    <td className={`px-4 py-2.5 text-[13px] text-right ${row.type === 'total' ? 'text-[#2563EB] font-semibold' : row.type === 'subtotal' ? 'font-semibold text-[#0F172A]' : row.type === 'receita' ? 'text-[#059669]' : 'text-[#DC2626]'}`} style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                      {formatarKwanza(row.acum)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {activeTab !== 'dre' && (
        <div className="bg-white border border-[#E2E8F0] rounded-lg flex items-center justify-center py-16">
          <div className="text-center">
            <p className="text-[14px] text-[#475569] font-medium">
              {tabs.find(t => t.key === activeTab)?.label} em preparação
            </p>
            <p className="text-[12px] text-[#94A3B8] mt-1">Seleccione DRE para ver o relatório completo</p>
          </div>
        </div>
      )}
    </div>
  );
}
