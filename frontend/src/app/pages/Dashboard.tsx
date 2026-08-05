import { useEffect, useState } from 'react';
import {
  TrendingUp, TrendingDown, AlertCircle,
  ChevronRight, Eye, Edit2,
  FileStack, Sparkles, CheckCircle2, Target,
} from 'lucide-react';
import {
  lancamentosMock, formatarKwanza, dadosGraficoMensal,
} from '../data/mockData';
import { obterDashboard } from '../api/dashboardApi';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, Area, AreaChart,
} from 'recharts';

// ── Tipos ──────────────────────────────────────────────────────────────────

type Origem = 'ia' | 'manual';
type Estado = 'aprovado' | 'pendente' | 'rejeitado';

interface LinhaLancamento {
  id: number;
  data: string;
  doc: string;
  contas: string;
  valor: number;
  origem: Origem;
  estado: Estado;
}

interface Kpis {
  documentosImportados: number;
  sugestoesPendentes: number;
  lancamentosAprovados: number;
  precisaoIA: number;
}

const ESTADOS_VALIDOS: Estado[] = ['aprovado', 'pendente', 'rejeitado'];

function normalizarEstado(valor: string): Estado {
  return ESTADOS_VALIDOS.includes(valor as Estado) ? (valor as Estado) : 'pendente';
}

// ── Micro-componentes partilhados ────────────────────────────────────────────

function Badge({ variant, children }: { variant: Estado | Origem; children: React.ReactNode }) {
  const styles: Record<Estado | Origem, string> = {
    aprovado: 'bg-[#ECFDF5] text-[#059669]',
    pendente: 'bg-[#FFFBEB] text-[#D97706]',
    rejeitado: 'bg-[#FEF2F2] text-[#DC2626]',
    ia: 'bg-[#F5F3FF] text-[#7C3AED]',
    manual: 'bg-[#EFF6FF] text-[#2563EB]',
  };
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium ${styles[variant]}`}>
      {children}
    </span>
  );
}

type AcentoKpi = 'info' | 'ai' | 'success' | 'neutral';

const ACENTO_KPI: Record<AcentoKpi, { bar: string; iconBg: string; iconColor: string }> = {
  info: { bar: '#2563EB', iconBg: '#EFF6FF', iconColor: '#2563EB' },
  ai: { bar: '#7C3AED', iconBg: '#F5F3FF', iconColor: '#7C3AED' },
  success: { bar: '#059669', iconBg: '#ECFDF5', iconColor: '#059669' },
  neutral: { bar: '#94A3B8', iconBg: '#F8FAFC', iconColor: '#64748B' },
};

function KpiCard({ label, value, sub, trend, trendUp, icon: Icon, acento = 'neutral' }: {
  label: string;
  value: string;
  sub?: string;
  trend?: string;
  trendUp?: boolean;
  icon: React.ElementType;
  acento?: AcentoKpi;
}) {
  const cores = ACENTO_KPI[acento];
  return (
    <div className="group relative bg-white border border-[#E2E8F0] rounded-lg p-4 overflow-hidden transition-shadow hover:shadow-[0_4px_16px_-4px_rgba(15,23,42,0.08)]">
      <div className="absolute top-0 left-0 right-0 h-[3px]" style={{ backgroundColor: cores.bar }} />
      <div className="flex items-start justify-between mb-3">
        <p className="text-[12px] font-medium text-[#475569]">{label}</p>
        <div
          className="w-7 h-7 rounded-md flex items-center justify-center shrink-0"
          style={{ backgroundColor: cores.iconBg }}
        >
          <Icon style={{ width: 14, height: 14, color: cores.iconColor }} />
        </div>
      </div>
      <p className="text-[26px] font-bold text-[#0F172A] leading-none mb-2 tabular-nums">{value}</p>
      {trend && (
        <div className={`flex items-center gap-1 text-[12px] font-medium ${trendUp ? 'text-[#059669]' : 'text-[#DC2626]'}`}>
          {trendUp ? <TrendingUp style={{ width: 12, height: 12 }} /> : <TrendingDown style={{ width: 12, height: 12 }} />}
          <span>{trend}</span>
        </div>
      )}
      {sub && !trend && <p className="text-[12px] text-[#475569]">{sub}</p>}
    </div>
  );
}

function EstadoLabel({ estado }: { estado: Estado }) {
  const labels: Record<Estado, string> = {
    aprovado: 'Aprovado',
    pendente: 'Pendente',
    rejeitado: 'Rejeitado',
  };
  return <Badge variant={estado}>{labels[estado]}</Badge>;
}

// ── Dados de fallback (usados enquanto a API não responde ou falha) ─────────

const documentosRecentesFallback: LinhaLancamento[] = [
  { id: 1, data: '08/07/2026', doc: 'FT 001/2026', contas: '1.1.1 → 4.1.1', valor: 850000, origem: 'ia', estado: 'aprovado' },
  { id: 2, data: '07/07/2026', doc: 'FR 047/2026', contas: '2.1.1 → 1.1.2', valor: 1250000, origem: 'ia', estado: 'pendente' },
  { id: 3, data: '07/07/2026', doc: 'NC 003/2026', contas: '4.1.1 → 1.1.3', valor: 45000, origem: 'manual', estado: 'aprovado' },
  { id: 4, data: '06/07/2026', doc: 'RC 012/2026', contas: '1.1.2 → 2.1.3', valor: 320000, origem: 'ia', estado: 'pendente' },
  { id: 5, data: '05/07/2026', doc: 'FT 089/2026', contas: '5.1.1 → 2.1.1', valor: 1500000, origem: 'ia', estado: 'aprovado' },
  { id: 6, data: '04/07/2026', doc: 'FT 088/2026', contas: '1.1.1 → 4.1.2', valor: 680000, origem: 'manual', estado: 'rejeitado' },
];

const kpisFallback: Kpis = {
  documentosImportados: 23,
  sugestoesPendentes: 7,
  lancamentosAprovados: 143,
  precisaoIA: 96.4,
};

// ── Dashboard ────────────────────────────────────────────────────────────────

export function Dashboard() {
  const [chartData, setChartData] = useState(dadosGraficoMensal.slice(-6));
  const [lancamentos, setLancamentos] = useState<LinhaLancamento[]>(documentosRecentesFallback);
  const [kpis, setKpis] = useState<Kpis>(kpisFallback);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let cancelado = false;

    obterDashboard()
      .then((dados) => {
        if (cancelado) return;

        setChartData(dados.graficoMensal.slice(-6));
        setKpis(dados.kpis);
        setLancamentos(
          dados.documentosRecentes.map((d) => ({
            id: Number(d.id),
            data: d.data,
            doc: d.nome,
            contas: d.tipo,
            valor: d.valor ?? 0,
            origem: (d.origem as Origem) ?? 'ia',
            estado: normalizarEstado(d.estado),
          }))
        );
      })
      .catch((e) => {
        if (cancelado) return;
        console.error('Erro ao carregar dashboard:', e);
        setErro('Não foi possível carregar os dados mais recentes. A mostrar dados anteriores.');
      })
      .finally(() => {
        if (!cancelado) setLoading(false);
      });

    return () => {
      cancelado = true;
    };
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-[60vh]">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-2 border-[#2563EB] border-t-transparent rounded-full animate-spin" />
          <p className="text-[13px] text-[#475569]">A carregar dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-5 max-w-[1200px]">

      {/* Cabeçalho da página */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-1 h-9 rounded-full bg-[#2563EB]" />
          <div>
            <h1 className="text-[19px] font-semibold text-[#0F172A] tracking-tight">Dashboard</h1>
            <p className="text-[13px] text-[#475569] mt-0.5">Visão geral · Julho 2026</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1.5 bg-[#FEF2F2] border border-[#FECACA] text-[#DC2626] px-2.5 py-1.5 rounded-md text-[11px] font-medium">
            <AlertCircle style={{ width: 12, height: 12 }} />
            5 faturas · prazo excedido · PD 71/25
          </div>
        </div>
      </div>

      {/* Aviso de erro (não bloqueia a UI, apenas informa) */}
      {erro && (
        <div className="flex items-center gap-2 bg-[#FFFBEB] text-[#92400E] px-3 py-2 rounded-md text-[12px]">
          <AlertCircle style={{ width: 14, height: 14 }} />
          {erro}
        </div>
      )}

      {/* KPIs — agora ligados ao estado real, não a valores fixos */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard
          label="Documentos importados hoje"
          value={String(kpis.documentosImportados)}
          trend="+4 vs ontem"
          trendUp
          icon={FileStack}
          acento="info"
        />
        <KpiCard
          label="Sugestões IA pendentes"
          value={String(kpis.sugestoesPendentes)}
          sub="aguardam validação"
          icon={Sparkles}
          acento="ai"
        />
        <KpiCard
          label="Lançamentos aprovados (mês)"
          value={String(kpis.lancamentosAprovados)}
          trend="+12% vs mês ant."
          trendUp
          icon={CheckCircle2}
          acento="success"
        />
        <KpiCard
          label="Taxa de acerto IA"
          value={`${kpis.precisaoIA.toFixed(1).replace('.', ',')}%`}
          trend="+0,8 pp vs mês ant."
          trendUp
          icon={Target}
          acento="success"
        />
      </div>

      {/* Tabela: Lançamentos Recentes */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
        <div className="flex items-center justify-between px-4 py-3 border-b border-[#E2E8F0]">
          <h2 className="text-[14px] font-semibold text-[#0F172A]">Lançamentos Recentes</h2>
          <a
            href="/lancamentos"
            className="flex items-center gap-1 text-[12px] text-[#2563EB] hover:text-[#1D4ED8] transition-colors"
          >
            Ver todos <ChevronRight style={{ width: 12, height: 12 }} />
          </a>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                {['Data', 'Documento', 'Contas', 'Valor (AOA)', 'Origem', 'Estado', 'Acções'].map((h) => (
                  <th
                    key={h}
                    className={`px-4 py-2.5 text-[11px] font-medium text-[#475569] uppercase tracking-wide whitespace-nowrap ${
                      h === 'Valor (AOA)' || h === 'Acções' ? 'text-right' : 'text-left'
                    }`}
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {lancamentos.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-6 text-center text-[13px] text-[#94A3B8]">
                    Sem lançamentos recentes.
                  </td>
                </tr>
              ) : (
                lancamentos.map((row) => (
                  <tr key={row.id} className="border-b border-[#F1F5F9] hover:bg-[#F8FAFC] transition-colors">
                    <td className="px-4 py-2.5 text-[13px] text-[#475569] whitespace-nowrap">{row.data}</td>
                    <td className="px-4 py-2.5 text-[13px] text-[#0F172A] font-medium whitespace-nowrap">{row.doc}</td>
                    <td
                      className="px-4 py-2.5 text-[13px] text-[#475569] whitespace-nowrap"
                      style={{ fontFamily: 'JetBrains Mono, monospace' }}
                    >
                      {row.contas}
                    </td>
                    <td
                      className="px-4 py-2.5 text-[13px] text-[#0F172A] text-right whitespace-nowrap"
                      style={{ fontFamily: 'JetBrains Mono, monospace' }}
                    >
                      {formatarKwanza(row.valor)}
                    </td>
                    <td className="px-4 py-2.5">
                      <Badge variant={row.origem}>{row.origem === 'ia' ? '✦ IA' : 'Manual'}</Badge>
                    </td>
                    <td className="px-4 py-2.5">
                      <EstadoLabel estado={row.estado} />
                    </td>
                    <td className="px-4 py-2.5 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          type="button"
                          className="w-6 h-6 flex items-center justify-center text-[#64748B] hover:text-[#0F172A] hover:bg-[#F1F5F9] rounded transition-colors"
                        >
                          <Eye style={{ width: 13, height: 13 }} />
                        </button>
                        <button
                          type="button"
                          className="w-6 h-6 flex items-center justify-center text-[#64748B] hover:text-[#0F172A] hover:bg-[#F1F5F9] rounded transition-colors"
                        >
                          <Edit2 style={{ width: 13, height: 13 }} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Gráficos */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-4">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-[14px] font-semibold text-[#0F172A]">Receitas vs Despesas</h2>
            <div className="flex items-center gap-3 text-[11px] text-[#475569]">
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-[#059669]" /> Receitas
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-[#DC2626]" /> Despesas
              </span>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <AreaChart data={chartData} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="corReceitas" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#059669" stopOpacity={0.18} />
                  <stop offset="100%" stopColor="#059669" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="corDespesas" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#DC2626" stopOpacity={0.14} />
                  <stop offset="100%" stopColor="#DC2626" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" vertical={false} />
              <XAxis dataKey="mes" tick={{ fontSize: 11, fill: '#94A3B8' }} axisLine={false} tickLine={false} />
              <YAxis
                tickFormatter={(v) => `${(v / 1000000).toFixed(0)}M`}
                tick={{ fontSize: 11, fill: '#94A3B8' }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                formatter={(v: number) => formatarKwanza(v)}
                contentStyle={{ fontSize: 12, border: '1px solid #E2E8F0', borderRadius: 8, boxShadow: '0 4px 16px -4px rgba(15,23,42,0.12)' }}
              />
              <Area type="monotone" dataKey="receitas" stroke="#059669" strokeWidth={2} fill="url(#corReceitas)" name="Receitas" />
              <Area type="monotone" dataKey="despesas" stroke="#DC2626" strokeWidth={2} fill="url(#corDespesas)" name="Despesas" />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-white border border-[#E2E8F0] rounded-lg p-4">
          <h2 className="text-[14px] font-semibold text-[#0F172A] mb-4">Resultado Mensal</h2>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart
              data={chartData.map((d) => ({ mes: d.mes, resultado: d.receitas - d.despesas }))}
              margin={{ top: 0, right: 8, left: 0, bottom: 0 }}
            >
              <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
              <XAxis dataKey="mes" tick={{ fontSize: 11, fill: '#94A3B8' }} axisLine={false} tickLine={false} />
              <YAxis
                tickFormatter={(v) => `${(v / 1000000).toFixed(0)}M`}
                tick={{ fontSize: 11, fill: '#94A3B8' }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                formatter={(v: number) => formatarKwanza(v)}
                contentStyle={{ fontSize: 12, border: '1px solid #E2E8F0', borderRadius: 6, boxShadow: 'none' }}
              />
              <Bar dataKey="resultado" fill="#2563EB" radius={[3, 3, 0, 0]} name="Resultado" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}