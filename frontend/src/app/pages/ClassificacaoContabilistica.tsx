import { useEffect, useState } from 'react';
import { Sparkles, Check, X } from 'lucide-react';
import { formatarKwanza, planoContasAngolano } from '../data/mockData';
import { toast } from 'sonner';
import { useAuth, permissoes } from '../auth/AuthContext';

const documentosPendentes = [
  { id: '1', doc: 'FT 002/2026', data: '08/07/2026', valor: 650000, descricao: 'Prestação de serviços de consultoria', sugContaD: '1.1.2', sugContaC: '4.1.2', confianca: 94 },
  { id: '2', doc: 'FC 031/2026', data: '07/07/2026', valor: 320000, descricao: 'Compra de material de escritório', sugContaD: '5.1.6', sugContaC: '2.1.1', confianca: 88 },
  { id: '3', doc: 'RC 019/2026', data: '06/07/2026', valor: 180000, descricao: 'Pagamento de energia — ENDE', sugContaD: '5.1.4', sugContaC: '1.1.2', confianca: 99 },
];

const contasOptions = planoContasAngolano.filter(c => c.nivel === 3);

export function ClassificacaoContabilistica() {
  const { perfil } = useAuth();
  const podeValidar = permissoes(perfil).podeEscreverLancamentos; // RN002: só Contabilista
  const [selected, setSelected] = useState<string | null>(documentosPendentes[0].id);
  const [overrides, setOverrides] = useState<Record<string, { contaD: string; contaC: string }>>({});

  const doc = documentosPendentes.find(d => d.id === selected);
  const override = selected ? overrides[selected] : null;
  const contaD = override?.contaD ?? doc?.sugContaD ?? '';
  const contaC = override?.contaC ?? doc?.sugContaC ?? '';

  const handleAprovar = () => { toast.success('Lançamento classificado e aprovado'); };
  const handleRejeitar = () => { toast.error('Classificação rejeitada'); };

  return (
    <div className="max-w-[1200px] space-y-4">
      <div>
        <h1 className="text-[18px] font-semibold text-[#0F172A]">Classificação Contabilística</h1>
        <p className="text-[13px] text-[#475569] mt-0.5">{documentosPendentes.length} documentos aguardam classificação</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* List */}
        <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden self-start">
          <div className="px-4 py-2.5 border-b border-[#E2E8F0] bg-[#F8FAFC]">
            <p className="text-[11px] font-medium text-[#475569] uppercase tracking-wide">Pendentes</p>
          </div>
          {documentosPendentes.map(d => (
            <button
              key={d.id}
              onClick={() => setSelected(d.id)}
              className={`w-full text-left px-4 py-3 border-b border-[#F1F5F9] transition-colors ${selected === d.id ? 'bg-[#EFF6FF]' : 'hover:bg-[#F8FAFC]'}`}
            >
              <div className="flex items-center justify-between mb-0.5">
                <span className="text-[13px] font-medium text-[#0F172A]">{d.doc}</span>
                <span className={`text-[11px] font-medium px-1.5 py-0.5 rounded-full ${d.confianca >= 95 ? 'bg-[#ECFDF5] text-[#059669]' : 'bg-[#FFFBEB] text-[#D97706]'}`}>
                  {d.confianca}%
                </span>
              </div>
              <p className="text-[12px] text-[#475569] truncate">{d.descricao}</p>
              <p className="text-[12px] text-[#94A3B8] mt-0.5">{formatarKwanza(d.valor)}</p>
            </button>
          ))}
        </div>

        {/* Classification panel */}
        {doc && (
          <div className="lg:col-span-2 space-y-3">
            <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
              <div className="px-4 py-3 border-b border-[#E2E8F0] flex items-center gap-2">
                <h2 className="text-[13px] font-semibold text-[#0F172A]">{doc.doc}</h2>
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium bg-[#F5F3FF] text-[#7C3AED]">
                  <Sparkles style={{ width: 10, height: 10 }} /> Sugestão IA
                </span>
              </div>

              <div className="p-4 space-y-4">
                {/* Doc info */}
                <div className="grid grid-cols-2 gap-3">
                  {[['Data', doc.data], ['Valor', formatarKwanza(doc.valor)], ['Documento', doc.doc], ['Confiança IA', `${doc.confianca}%`]].map(([k, v]) => (
                    <div key={k}>
                      <p className="text-[11px] font-medium text-[#94A3B8] uppercase tracking-wide mb-0.5">{k}</p>
                      <p className="text-[13px] font-medium text-[#0F172A]" style={{ fontFamily: ['Valor','Confiança IA'].includes(k) ? 'JetBrains Mono, monospace' : undefined }}>{v}</p>
                    </div>
                  ))}
                </div>
                <div>
                  <p className="text-[11px] font-medium text-[#94A3B8] uppercase tracking-wide mb-0.5">Descrição</p>
                  <p className="text-[13px] text-[#475569]">{doc.descricao}</p>
                </div>

                {/* Account selectors */}
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Conta Débito</label>
                    <select
                      value={contaD}
                      onChange={e => setOverrides(prev => ({ ...prev, [doc.id]: { contaD: e.target.value, contaC: contaC } }))}
                      className="w-full h-8 px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all"
                      style={{ fontFamily: 'JetBrains Mono, monospace' }}
                    >
                      {contasOptions.map(c => (
                        <option key={c.id} value={c.codigo}>{c.codigo} — {c.nome}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Conta Crédito</label>
                    <select
                      value={contaC}
                      onChange={e => setOverrides(prev => ({ ...prev, [doc.id]: { contaD: contaD, contaC: e.target.value } }))}
                      className="w-full h-8 px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all"
                      style={{ fontFamily: 'JetBrains Mono, monospace' }}
                    >
                      {contasOptions.map(c => (
                        <option key={c.id} value={c.codigo}>{c.codigo} — {c.nome}</option>
                      ))}
                    </select>
                  </div>
                </div>

                {/* Actions — RN002/RN010: só o Contabilista aprova/rejeita */}
                {podeValidar ? (
                  <div className="flex items-center gap-2 pt-2">
                    <button onClick={handleAprovar} className="flex items-center gap-1.5 h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-[13px] font-medium rounded-md transition-colors">
                      <Check style={{ width: 13, height: 13 }} /> Aprovar
                    </button>
                    <button onClick={handleRejeitar} className="flex items-center gap-1.5 h-8 px-3 bg-[#DC2626] hover:bg-[#B91C1C] text-white text-[13px] font-medium rounded-md transition-colors">
                      <X style={{ width: 13, height: 13 }} /> Rejeitar
                    </button>
                  </div>
                ) : (
                  <div className="pt-2 text-[12px] text-[#64748B] italic">
                    Modo de leitura — apenas o Contabilista pode aprovar ou rejeitar.
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
