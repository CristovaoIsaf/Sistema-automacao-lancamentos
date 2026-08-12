import { useEffect, useState } from 'react';
import { BarChart3, FileText, Wallet, Loader2, AlertCircle, ArrowDownLeft, ArrowUpRight } from 'lucide-react';
import { formatarKwanza } from '../data/mockData';
import { intervaloDoPeriodo } from '../data/periodo';
import { obterDRE, obterBalanco, obterFluxoCaixa } from '../api/relatorioApi';
import type { RelatorioDRE, RelatorioBalanco, RelatorioFluxoCaixa } from '../types/relatorio';

// Fase 13 do plano de 20 fases — "garantir consistência entre lançamentos
// → balancete → balanço → DRE... não criar cálculos independentes no
// frontend": antes desta fase, esta página mostrava uma DRE com números
// 100% inventados no componente, e as abas Balancete/Balanço/Razão
// diziam sempre "em preparação". Balancete já tem a sua própria página
// dedicada (/balancetes, ver Balancetes.tsx) — não duplicada aqui.
// Livro Razão nunca teve nenhum endpoint real a suportá-lo nem estava
// ligado à navegação — fica fora do âmbito desta fase (ver relatório).
//
// Fase 17 do plano de 20 fases — Fluxo de Caixa: método direto, sem
// classificação operacional/investimento/financiamento (ver decisão
// documentada em FluxoCaixaService). Antes desta fase não existia
// nenhuma visão dos movimentos reais de Caixa/Depósitos.
type TabKey = 'dre' | 'balanco' | 'fluxo-caixa';

const tabs: { key: TabKey; label: string; icon: React.ElementType }[] = [
  { key: 'dre',          label: 'DRE', icon: BarChart3 },
  { key: 'balanco',      label: 'Balanço', icon: FileText },
  { key: 'fluxo-caixa',  label: 'Fluxo de Caixa', icon: Wallet },
];

export function Relatorios() {
  const [activeTab, setActiveTab] = useState<TabKey>('dre');
  const [periodo, setPeriodo] = useState('mes-atual');

  const [dre, setDre] = useState<RelatorioDRE | null>(null);
  const [balanco, setBalanco] = useState<RelatorioBalanco | null>(null);
  const [fluxoCaixa, setFluxoCaixa] = useState<RelatorioFluxoCaixa | null>(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let cancelado = false;
    setLoading(true);
    setErro(null);

    const { inicio, fim } = intervaloDoPeriodo(periodo);

    Promise.all([obterDRE(inicio, fim), obterBalanco(inicio, fim), obterFluxoCaixa(inicio, fim)])
      .then(([dadosDre, dadosBalanco, dadosFluxoCaixa]) => {
        if (cancelado) return;
        setDre(dadosDre);
        setBalanco(dadosBalanco);
        setFluxoCaixa(dadosFluxoCaixa);
      })
      .catch((e) => {
        if (cancelado) return;
        console.error('Erro ao carregar demonstrações financeiras:', e);
        setErro('Não foi possível carregar as demonstrações financeiras.');
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
          <h1 className="text-[18px] font-semibold text-[#0F172A]">Relatórios Financeiros</h1>
          <p className="text-[13px] text-[#475569] mt-0.5">PGCA · Decreto n.º 82/01</p>
        </div>
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

      {loading ? (
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-8 flex items-center justify-center gap-2 text-[13px] text-[#94A3B8]">
          <Loader2 className="animate-spin" size={16} /> A carregar...
        </div>
      ) : erro ? (
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-8 flex items-center justify-center gap-2 text-[13px] text-[#DC2626]">
          <AlertCircle size={16} /> {erro}
        </div>
      ) : activeTab === 'dre' && dre ? (
        <div className="space-y-4">
          <div className="grid grid-cols-3 gap-3">
            {[
              { label: 'Receitas Totais', value: dre.totalReceitas, color: 'text-[#059669]' },
              { label: 'Gastos Totais', value: dre.totalGastos, color: 'text-[#DC2626]' },
              { label: 'Resultado Líquido', value: dre.resultadoLiquido, color: dre.resultadoLiquido >= 0 ? 'text-[#2563EB]' : 'text-[#DC2626]' },
            ].map(kpi => (
              <div key={kpi.label} className="bg-white border border-[#E2E8F0] rounded-lg p-4">
                <p className="text-[12px] font-medium text-[#475569] mb-2">{kpi.label}</p>
                <p className={`text-[22px] font-bold ${kpi.color}`} style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                  {formatarKwanza(kpi.value)}
                </p>
              </div>
            ))}
          </div>

          <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
            <div className="px-4 py-3 border-b border-[#E2E8F0]">
              <h2 className="text-[13px] font-semibold text-[#0F172A]">Demonstração de Resultados por Exercício</h2>
              <p className="text-[11px] text-[#94A3B8] mt-0.5">
                Proveitos (classe 6) e Custos (classe 7). Compras (classe 2 — Existências) não entra
                aqui — aparece no Balanço, porque este sistema não modela existências/CMVC.
              </p>
            </div>
            <table className="w-full">
              <thead>
                <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                  <th className="px-4 py-2.5 text-left text-[11px] font-medium text-[#475569] uppercase tracking-wide">Conta</th>
                  <th className="px-4 py-2.5 text-right text-[11px] font-medium text-[#475569] uppercase tracking-wide">Valor</th>
                </tr>
              </thead>
              <tbody>
                {dre.receitas.length === 0 && dre.gastos.length === 0 ? (
                  <tr>
                    <td colSpan={2} className="px-4 py-8 text-center text-[13px] text-[#94A3B8]">
                      Nenhum movimento no período seleccionado.
                    </td>
                  </tr>
                ) : (
                  <>
                    {dre.receitas.map(l => (
                      <tr key={l.conta} className="border-b border-[#F1F5F9]">
                        <td className="px-4 py-2.5 text-[13px] text-[#475569] pl-8">{l.conta} — {l.nome}</td>
                        <td className="px-4 py-2.5 text-[13px] text-right text-[#059669]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(l.valor)}</td>
                      </tr>
                    ))}
                    <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                      <td className="px-4 py-2.5 text-[13px] font-semibold text-[#0F172A]">TOTAL RECEITAS</td>
                      <td className="px-4 py-2.5 text-[13px] text-right font-semibold text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(dre.totalReceitas)}</td>
                    </tr>
                    {dre.gastos.map(l => (
                      <tr key={l.conta} className="border-b border-[#F1F5F9]">
                        <td className="px-4 py-2.5 text-[13px] text-[#475569] pl-8">{l.conta} — {l.nome}</td>
                        <td className="px-4 py-2.5 text-[13px] text-right text-[#DC2626]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(l.valor)}</td>
                      </tr>
                    ))}
                    <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                      <td className="px-4 py-2.5 text-[13px] font-semibold text-[#0F172A]">TOTAL GASTOS</td>
                      <td className="px-4 py-2.5 text-[13px] text-right font-semibold text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(dre.totalGastos)}</td>
                    </tr>
                    <tr className="bg-[#EFF6FF] border-[#BFDBFE]">
                      <td className="px-4 py-2.5 text-[13px] font-semibold text-[#0F172A]">RESULTADO LÍQUIDO</td>
                      <td className="px-4 py-2.5 text-[13px] text-right font-semibold text-[#2563EB]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(dre.resultadoLiquido)}</td>
                    </tr>
                  </>
                )}
              </tbody>
            </table>
          </div>
        </div>
      ) : activeTab === 'balanco' && balanco ? (
        <div className="space-y-4">
          <div className="bg-[#FFFBEB] border border-[#FDE68A] rounded-lg px-4 py-2.5 text-[12px] text-[#92400E]">
            Ativo cobre Existências, Terceiros e Meios monetários; Capital Próprio cobre Capital e Reservas (Decreto 82/01, classe 5) mais o Resultado do Exercício (reaproveitado da DRE — este sistema não tem fecho de exercício, por isso o resultado do período fica sempre "pendente de aplicação"). O plano de contas deste TFC (âmbito reduzido) ainda não modela Ativo Não Corrente (Imobilizado), por isso o Balanço pode não fechar exatamente a zero.
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
              <div className="px-4 py-3 border-b border-[#E2E8F0]">
                <h2 className="text-[13px] font-semibold text-[#0F172A]">Ativo</h2>
              </div>
              <table className="w-full">
                <tbody>
                  {balanco.ativo.length === 0 ? (
                    <tr><td className="px-4 py-6 text-center text-[13px] text-[#94A3B8]">Sem movimento</td></tr>
                  ) : balanco.ativo.map(l => (
                    <tr key={l.conta} className="border-b border-[#F1F5F9]">
                      <td className="px-4 py-2.5 text-[13px] text-[#475569]">{l.conta} — {l.nome}</td>
                      <td className="px-4 py-2.5 text-[13px] text-right text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(l.valor)}</td>
                    </tr>
                  ))}
                  <tr className="bg-[#F8FAFC]">
                    <td className="px-4 py-2.5 text-[13px] font-semibold text-[#0F172A]">TOTAL ATIVO</td>
                    <td className="px-4 py-2.5 text-[13px] text-right font-semibold text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(balanco.totalAtivo)}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
              <div className="px-4 py-3 border-b border-[#E2E8F0]">
                <h2 className="text-[13px] font-semibold text-[#0F172A]">Passivo</h2>
              </div>
              <table className="w-full">
                <tbody>
                  {balanco.passivo.length === 0 ? (
                    <tr><td className="px-4 py-6 text-center text-[13px] text-[#94A3B8]">Sem movimento</td></tr>
                  ) : balanco.passivo.map(l => (
                    <tr key={l.conta} className="border-b border-[#F1F5F9]">
                      <td className="px-4 py-2.5 text-[13px] text-[#475569]">{l.conta} — {l.nome}</td>
                      <td className="px-4 py-2.5 text-[13px] text-right text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(l.valor)}</td>
                    </tr>
                  ))}
                  <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                    <td className="px-4 py-2.5 text-[13px] font-semibold text-[#0F172A]">TOTAL PASSIVO</td>
                    <td className="px-4 py-2.5 text-[13px] text-right font-semibold text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(balanco.totalPassivo)}</td>
                  </tr>
                </tbody>
              </table>

              <div className="px-4 py-3 border-b border-t border-[#E2E8F0]">
                <h2 className="text-[13px] font-semibold text-[#0F172A]">Capital Próprio</h2>
              </div>
              <table className="w-full">
                <tbody>
                  {balanco.capitalProprio.map(l => (
                    <tr key={l.conta} className="border-b border-[#F1F5F9]">
                      <td className="px-4 py-2.5 text-[13px] text-[#475569]">{l.conta} — {l.nome}</td>
                      <td className={`px-4 py-2.5 text-[13px] text-right ${l.valor < 0 ? 'text-[#DC2626]' : 'text-[#0F172A]'}`} style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(l.valor)}</td>
                    </tr>
                  ))}
                  <tr className="bg-[#F8FAFC]">
                    <td className="px-4 py-2.5 text-[13px] font-semibold text-[#0F172A]">TOTAL CAPITAL PRÓPRIO</td>
                    <td className="px-4 py-2.5 text-[13px] text-right font-semibold text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(balanco.totalCapitalProprio)}</td>
                  </tr>
                  <tr className="bg-[#EFF6FF] border-t-2 border-[#BFDBFE]">
                    <td className="px-4 py-2.5 text-[13px] font-semibold text-[#0F172A]">TOTAL PASSIVO + CAPITAL PRÓPRIO</td>
                    <td className="px-4 py-2.5 text-[13px] text-right font-semibold text-[#2563EB]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(balanco.totalPassivo + balanco.totalCapitalProprio)}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      ) : activeTab === 'fluxo-caixa' && fluxoCaixa ? (
        <div className="space-y-4">
          <div className="bg-[#FFFBEB] border border-[#FDE68A] rounded-lg px-4 py-2.5 text-[12px] text-[#92400E]">
            Método direto: lista os movimentos reais de Caixa e Depósitos. Não classifica em atividades operacionais/investimento/financiamento — este plano de contas (Decreto 82/01, âmbito reduzido do TFC) não modela essa distinção, e inventar uma classificação não verificada seria pior do que não a ter.
          </div>

          <div className="grid grid-cols-3 gap-3">
            {[
              { label: 'Entradas', value: fluxoCaixa.totalEntradas, color: 'text-[#059669]' },
              { label: 'Saídas', value: fluxoCaixa.totalSaidas, color: 'text-[#DC2626]' },
              { label: 'Saldo do Período', value: fluxoCaixa.saldoPeriodo, color: fluxoCaixa.saldoPeriodo >= 0 ? 'text-[#2563EB]' : 'text-[#DC2626]' },
            ].map(kpi => (
              <div key={kpi.label} className="bg-white border border-[#E2E8F0] rounded-lg p-4">
                <p className="text-[12px] font-medium text-[#475569] mb-2">{kpi.label}</p>
                <p className={`text-[22px] font-bold ${kpi.color}`} style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                  {formatarKwanza(kpi.value)}
                </p>
              </div>
            ))}
          </div>

          <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
            <div className="px-4 py-3 border-b border-[#E2E8F0]">
              <h2 className="text-[13px] font-semibold text-[#0F172A]">Movimentos de Caixa e Depósitos</h2>
            </div>
            <table className="w-full">
              <thead>
                <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                  <th className="px-4 py-2.5 text-left text-[11px] font-medium text-[#475569] uppercase tracking-wide">Data</th>
                  <th className="px-4 py-2.5 text-left text-[11px] font-medium text-[#475569] uppercase tracking-wide">Descrição</th>
                  <th className="px-4 py-2.5 text-left text-[11px] font-medium text-[#475569] uppercase tracking-wide">Conta</th>
                  <th className="px-4 py-2.5 text-left text-[11px] font-medium text-[#475569] uppercase tracking-wide">Contraparte</th>
                  <th className="px-4 py-2.5 text-right text-[11px] font-medium text-[#475569] uppercase tracking-wide">Valor</th>
                </tr>
              </thead>
              <tbody>
                {fluxoCaixa.movimentos.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-[13px] text-[#94A3B8]">
                      Nenhum movimento de caixa no período selecionado.
                    </td>
                  </tr>
                ) : (
                  fluxoCaixa.movimentos.map((m, i) => (
                    <tr key={`${m.lancamentoId}-${m.conta}-${m.tipo}-${i}`} className="border-b border-[#F1F5F9]">
                      <td className="px-4 py-2.5 text-[12px] text-[#64748B]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                        {m.data ? new Date(m.data).toLocaleDateString('pt-AO') : '—'}
                      </td>
                      <td className="px-4 py-2.5 text-[13px] text-[#0F172A]">{m.descricao ?? '—'}</td>
                      <td className="px-4 py-2.5 text-[13px] text-[#475569]">{m.conta} — {m.nomeConta}</td>
                      <td className="px-4 py-2.5 text-[13px] text-[#475569]">{m.contraConta ?? '—'}</td>
                      <td className={`px-4 py-2.5 text-[13px] text-right font-medium ${m.tipo === 'ENTRADA' ? 'text-[#059669]' : 'text-[#DC2626]'}`} style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                        <span className="inline-flex items-center gap-1 justify-end">
                          {m.tipo === 'ENTRADA' ? <ArrowDownLeft size={12} /> : <ArrowUpRight size={12} />}
                          {formatarKwanza(m.valor)}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      ) : null}
    </div>
  );
}
