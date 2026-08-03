import { useEffect, useState } from 'react';
import { Download } from 'lucide-react';
import { lancamentosMock, formatarKwanza } from '../data/mockData';

type Tab = 'diario' | 'razao' | 'iva';

export function LivrosFiscais() {
  const [activeTab, setActiveTab] = useState<Tab>('diario');

  const tabs: { key: Tab; label: string }[] = [
    { key: 'diario', label: 'Livro Diário' },
    { key: 'razao',  label: 'Livro Razão' },
    { key: 'iva',    label: 'Mapa de IVA' },
  ];

  const ivaData = [
    { mes: 'Jan', ivaDedutivel: 120000, ivaLiquidado: 340000, saldo: 220000 },
    { mes: 'Fev', ivaDedutivel: 95000,  ivaLiquidado: 280000, saldo: 185000 },
    { mes: 'Mar', ivaDedutivel: 180000, ivaLiquidado: 420000, saldo: 240000 },
    { mes: 'Abr', ivaDedutivel: 110000, ivaLiquidado: 310000, saldo: 200000 },
    { mes: 'Mai', ivaDedutivel: 145000, ivaLiquidado: 390000, saldo: 245000 },
    { mes: 'Jun', ivaDedutivel: 160000, ivaLiquidado: 450000, saldo: 290000 },
    { mes: 'Jul', ivaDedutivel: 178000, ivaLiquidado: 178000, saldo: 0 },
  ];

  return (
    <div className="max-w-[1200px] space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-[18px] font-semibold text-[#0F172A]">Livros Fiscais</h1>
          <p className="text-[13px] text-[#475569] mt-0.5">Livro Diário · Livro Razão · Mapa de IVA</p>
        </div>
        <button className="flex items-center gap-1.5 h-8 px-3 bg-white border border-[#E2E8F0] hover:bg-[#F8FAFC] text-[#0F172A] text-[13px] font-medium rounded-md transition-colors">
          <Download style={{ width: 13, height: 13 }} /> Exportar
        </button>
      </div>

      {/* Tabs */}
      <div className="flex items-center border-b border-[#E2E8F0]">
        {tabs.map(tab => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2.5 text-[13px] font-medium transition-colors border-b-2 -mb-px ${
              activeTab === tab.key
                ? 'border-[#2563EB] text-[#2563EB]'
                : 'border-transparent text-[#475569] hover:text-[#0F172A]'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Livro Diário */}
      {activeTab === 'diario' && (
        <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                  {['Nº', 'Data', 'Documento', 'Descrição', 'Débito', 'Crédito', 'Valor (AOA)'].map(h => (
                    <th key={h} className={`px-4 py-2.5 text-[11px] font-medium text-[#475569] uppercase tracking-wide ${h === 'Valor (AOA)' ? 'text-right' : 'text-left'}`}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {lancamentosMock.map((l, i) => (
                  <tr key={l.id} className="border-b border-[#F1F5F9] hover:bg-[#F8FAFC] transition-colors">
                    <td className="px-4 py-2.5 text-[13px] text-[#94A3B8]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{String(i + 1).padStart(3, '0')}</td>
                    <td className="px-4 py-2.5 text-[13px] text-[#475569] whitespace-nowrap">{new Date(l.data).toLocaleDateString('pt-AO')}</td>
                    <td className="px-4 py-2.5 text-[13px] font-medium text-[#0F172A]">{l.documento}</td>
                    <td className="px-4 py-2.5 text-[13px] text-[#475569] max-w-[200px] truncate">{l.descricao}</td>
                    <td className="px-4 py-2.5 text-[13px] text-[#475569]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{l.contaDebito}</td>
                    <td className="px-4 py-2.5 text-[13px] text-[#475569]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{l.contaCredito}</td>
                    <td className="px-4 py-2.5 text-[13px] font-medium text-[#0F172A] text-right" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(l.valor)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Livro Razão */}
      {activeTab === 'razao' && (
        <div className="space-y-3">
          {['1.1.1', '1.1.2', '2.1.1', '4.1.1'].map(conta => {
            const nomes: Record<string, string> = { '1.1.1': 'Caixa', '1.1.2': 'Bancos Conta Movimento', '2.1.1': 'Fornecedores', '4.1.1': 'Vendas de Mercadorias' };
            const movs = lancamentosMock.filter(l => l.contaDebito === conta || l.contaCredito === conta);
            return (
              <div key={conta} className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
                <div className="px-4 py-2.5 border-b border-[#E2E8F0] flex items-center gap-2 bg-[#F8FAFC]">
                  <span className="text-[13px] font-semibold text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{conta}</span>
                  <span className="text-[13px] text-[#475569]">— {nomes[conta]}</span>
                </div>
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-[#F1F5F9]">
                      {['Data', 'Documento', 'Descrição', 'Débito', 'Crédito', 'Saldo'].map(h => (
                        <th key={h} className={`px-4 py-2 text-[11px] font-medium text-[#475569] uppercase tracking-wide ${['Débito','Crédito','Saldo'].includes(h) ? 'text-right' : 'text-left'}`}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {movs.slice(0, 3).map(l => (
                      <tr key={l.id} className="border-b border-[#F1F5F9] hover:bg-[#F8FAFC]">
                        <td className="px-4 py-2 text-[12px] text-[#475569]">{new Date(l.data).toLocaleDateString('pt-AO')}</td>
                        <td className="px-4 py-2 text-[12px] font-medium text-[#0F172A]">{l.documento}</td>
                        <td className="px-4 py-2 text-[12px] text-[#475569] max-w-[180px] truncate">{l.descricao}</td>
                        <td className="px-4 py-2 text-[12px] text-right text-[#059669]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{l.contaDebito === conta ? formatarKwanza(l.valor) : '—'}</td>
                        <td className="px-4 py-2 text-[12px] text-right text-[#DC2626]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{l.contaCredito === conta ? formatarKwanza(l.valor) : '—'}</td>
                        <td className="px-4 py-2 text-[12px] text-right font-medium text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(l.valor)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            );
          })}
        </div>
      )}

      {/* Mapa IVA */}
      {activeTab === 'iva' && (
        <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
          <div className="px-4 py-3 border-b border-[#E2E8F0]">
            <h2 className="text-[13px] font-semibold text-[#0F172A]">Mapa de IVA — Taxa Normal 17%</h2>
          </div>
          <table className="w-full">
            <thead>
              <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                {['Mês', 'IVA Dedutível', 'IVA Liquidado', 'Saldo IVA a Pagar'].map(h => (
                  <th key={h} className={`px-4 py-2.5 text-[11px] font-medium text-[#475569] uppercase tracking-wide ${h !== 'Mês' ? 'text-right' : 'text-left'}`}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {ivaData.map(row => (
                <tr key={row.mes} className="border-b border-[#F1F5F9] hover:bg-[#F8FAFC]">
                  <td className="px-4 py-2.5 text-[13px] font-medium text-[#0F172A]">{row.mes} 2026</td>
                  <td className="px-4 py-2.5 text-[13px] text-right text-[#059669]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(row.ivaDedutivel)}</td>
                  <td className="px-4 py-2.5 text-[13px] text-right text-[#DC2626]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(row.ivaLiquidado)}</td>
                  <td className="px-4 py-2.5 text-[13px] text-right font-medium" style={{ fontFamily: 'JetBrains Mono, monospace', color: row.saldo > 0 ? '#DC2626' : '#059669' }}>
                    {formatarKwanza(row.saldo)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
