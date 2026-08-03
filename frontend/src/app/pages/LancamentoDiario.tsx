import { useEffect, useState } from 'react';
import { Plus, Trash2, CheckCircle2, AlertCircle } from 'lucide-react';
import { formatarKwanza, planoContasAngolano } from '../data/mockData';
import { toast } from 'sonner';

interface Linha {
  id: string;
  conta: string;
  descricao: string;
  debito: string;
  credito: string;
}

const contas = planoContasAngolano.filter(c => c.nivel === 3);

const novaLinha = (): Linha => ({ id: Date.now().toString(), conta: '', descricao: '', debito: '', credito: '' });

export function LancamentoDiario() {
  const [data, setData] = useState('2026-07-08');
  const [nfDoc, setNfDoc] = useState('');
  const [nif, setNif] = useState('');
  const [tipoDoc, setTipoDoc] = useState('Fatura');
  const [historico, setHistorico] = useState('');
  const [linhas, setLinhas] = useState<Linha[]>([novaLinha(), novaLinha()]);

  const totalD = linhas.reduce((s, l) => s + (parseFloat(l.debito) || 0), 0);
  const totalC = linhas.reduce((s, l) => s + (parseFloat(l.credito) || 0), 0);
  const equilibrado = Math.abs(totalD - totalC) < 0.01 && totalD > 0;

  const addLinha = () => setLinhas(prev => [...prev, novaLinha()]);
  const removeLinha = (id: string) => setLinhas(prev => prev.filter(l => l.id !== id));
  const updateLinha = (id: string, field: keyof Linha, value: string) =>
    setLinhas(prev => prev.map(l => l.id === id ? { ...l, [field]: value } : l));

  const handleSave = () => {
    if (!equilibrado) { toast.error('Lançamento desequilibrado'); return; }
    toast.success('Lançamento diário registado');
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
            <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Histórico</label>
            <input type="text" value={historico} onChange={e => setHistorico(e.target.value)}
              placeholder="Descrição do lançamento..."
              className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all" />
          </div>
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
                      {contas.map(c => <option key={c.id} value={c.codigo}>{c.codigo} — {c.nome}</option>)}
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
            disabled={!equilibrado}
            className="flex items-center gap-1.5 h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] disabled:opacity-40 text-white text-[13px] font-medium rounded-md transition-colors"
          >
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
