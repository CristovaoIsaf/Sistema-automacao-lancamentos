import { useState } from 'react';
import { Sparkles, Play, Plus, Edit2, TrendingUp, Target } from 'lucide-react';
import { toast } from 'sonner';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const categorias = [
  { id: '1', nome: 'Venda à Vista', conta: '4.1.1', keywords: ['venda', 'FT', 'fatura'], precisao: 98, usos: 143 },
  { id: '2', nome: 'Compra a Fornecedor', conta: '2.1.1', keywords: ['compra', 'fornecedor', 'FC'], precisao: 95, usos: 87 },
  { id: '3', nome: 'Salários', conta: '5.1.2', keywords: ['salário', 'ordenado', 'folha'], precisao: 99, usos: 36 },
  { id: '4', nome: 'IVA Liquidado', conta: '2.1.3', keywords: ['IVA', 'iva', 'imposto'], precisao: 97, usos: 210 },
  { id: '5', nome: 'Energia', conta: '5.1.4', keywords: ['energia', 'ENDE', 'electricidade'], precisao: 92, usos: 12 },
];

const chartData = categorias.map(c => ({ nome: c.nome.length > 12 ? c.nome.slice(0, 12) + '…' : c.nome, usos: c.usos, precisao: c.precisao }));

export function IACategorizacao() {
  const [testInput, setTestInput] = useState('');
  const [testResult, setTestResult] = useState<{ categoria: string; conta: string; confianca: number } | null>(null);
  const [testing, setTesting] = useState(false);

  const handleTest = () => {
    if (!testInput.trim()) return;
    setTesting(true);
    setTimeout(() => {
      setTestResult({ categoria: 'Venda à Vista', conta: '4.1.1', confianca: 97.2 });
      setTesting(false);
    }, 800);
  };

  return (
    <div className="max-w-[1200px] space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-[18px] font-semibold text-[#0F172A]">IA — Categorização</h1>
          <p className="text-[13px] text-[#475569] mt-0.5">Gestão de categorias · Métricas de desempenho</p>
        </div>
        <button
          onClick={() => toast.info('Formulário de nova categoria')}
          className="flex items-center gap-1.5 h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-[13px] font-medium rounded-md transition-colors"
        >
          <Plus style={{ width: 13, height: 13 }} /> Nova Categoria
        </button>
      </div>

      {/* KPIs */}
      <div className="grid grid-cols-3 gap-3">
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-4">
          <p className="text-[12px] font-medium text-[#475569] mb-1">Taxa de Acerto Geral</p>
          <p className="text-[24px] font-bold text-[#0F172A]">96,4%</p>
          <div className="flex items-center gap-1 text-[#059669] text-[12px] mt-1"><TrendingUp style={{ width: 12, height: 12 }} /> +0,8 pp vs mês ant.</div>
        </div>
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-4">
          <p className="text-[12px] font-medium text-[#475569] mb-1">Documentos Categorizados</p>
          <p className="text-[24px] font-bold text-[#0F172A]">488</p>
          <p className="text-[12px] text-[#475569] mt-1">Este mês</p>
        </div>
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-4">
          <p className="text-[12px] font-medium text-[#475569] mb-1">Categorias Activas</p>
          <p className="text-[24px] font-bold text-[#0F172A]">{categorias.length}</p>
          <div className="flex items-center gap-1 text-[#7C3AED] text-[12px] mt-1"><Sparkles style={{ width: 12, height: 12 }} /> Modelo v2.1</div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Test panel */}
        <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
          <div className="px-4 py-3 border-b border-[#E2E8F0] flex items-center gap-2">
            <Sparkles style={{ width: 14, height: 14 }} className="text-[#7C3AED]" />
            <h2 className="text-[13px] font-semibold text-[#0F172A]">Testar Categorização</h2>
          </div>
          <div className="p-4 space-y-3">
            <div>
              <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Descrição do documento</label>
              <textarea
                value={testInput}
                onChange={e => setTestInput(e.target.value)}
                placeholder="Ex: Venda de mercadorias — Fatura FT 001/2026"
                rows={3}
                className="w-full px-3 py-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all resize-none"
              />
            </div>
            <button
              onClick={handleTest}
              disabled={testing || !testInput.trim()}
              className="w-full flex items-center justify-center gap-1.5 h-8 bg-[#7C3AED] hover:bg-[#6D28D9] disabled:opacity-40 text-white text-[13px] font-medium rounded-md transition-colors"
            >
              {testing ? <><div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" /> A analisar...</> : <><Play style={{ width: 13, height: 13 }} /> Testar IA</>}
            </button>
            {testResult && (
              <div className="bg-[#F5F3FF] border border-[#DDD6FE] rounded-md p-3 space-y-2">
                <div className="flex items-center gap-1.5 text-[#7C3AED] text-[12px] font-medium">
                  <Sparkles style={{ width: 12, height: 12 }} /> Resultado
                </div>
                <div className="space-y-1 text-[12px]">
                  <div className="flex justify-between">
                    <span className="text-[#475569]">Categoria</span>
                    <span className="font-medium text-[#0F172A]">{testResult.categoria}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-[#475569]">Conta PGCA</span>
                    <span className="font-medium text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{testResult.conta}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-[#475569]">Confiança</span>
                    <span className="font-semibold text-[#7C3AED]">{testResult.confianca}%</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Chart */}
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-4 lg:col-span-2">
          <h2 className="text-[13px] font-semibold text-[#0F172A] mb-4">Uso por Categoria</h2>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={chartData} margin={{ top: 0, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
              <XAxis dataKey="nome" tick={{ fontSize: 11, fill: '#94A3B8' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: '#94A3B8' }} axisLine={false} tickLine={false} />
              <Tooltip contentStyle={{ fontSize: 12, border: '1px solid #E2E8F0', borderRadius: 6, boxShadow: 'none' }} />
              <Bar dataKey="usos" fill="#7C3AED" radius={[3, 3, 0, 0]} name="Utilizações" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Categories table */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
        <div className="px-4 py-3 border-b border-[#E2E8F0]">
          <h2 className="text-[13px] font-semibold text-[#0F172A]">Categorias Configuradas</h2>
        </div>
        <table className="w-full">
          <thead>
            <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
              {['Categoria', 'Conta PGCA', 'Keywords', 'Precisão', 'Usos', 'Acções'].map(h => (
                <th key={h} className={`px-4 py-2.5 text-[11px] font-medium text-[#475569] uppercase tracking-wide ${['Precisão','Usos'].includes(h) ? 'text-right' : 'text-left'}`}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {categorias.map(cat => (
              <tr key={cat.id} className="border-b border-[#F1F5F9] hover:bg-[#F8FAFC] transition-colors">
                <td className="px-4 py-2.5 text-[13px] font-medium text-[#0F172A]">{cat.nome}</td>
                <td className="px-4 py-2.5 text-[13px] text-[#7C3AED]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{cat.conta}</td>
                <td className="px-4 py-2.5">
                  <div className="flex flex-wrap gap-1">
                    {cat.keywords.map(kw => (
                      <span key={kw} className="px-1.5 py-0.5 bg-[#F1F5F9] text-[#475569] text-[11px] rounded">{kw}</span>
                    ))}
                  </div>
                </td>
                <td className="px-4 py-2.5 text-[13px] font-medium text-right" style={{ color: cat.precisao >= 95 ? '#059669' : '#D97706' }}>
                  {cat.precisao}%
                </td>
                <td className="px-4 py-2.5 text-[13px] text-[#475569] text-right" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{cat.usos}</td>
                <td className="px-4 py-2.5">
                  <button className="w-6 h-6 flex items-center justify-center text-[#64748B] hover:text-[#0F172A] hover:bg-[#F1F5F9] rounded transition-colors">
                    <Edit2 style={{ width: 13, height: 13 }} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
