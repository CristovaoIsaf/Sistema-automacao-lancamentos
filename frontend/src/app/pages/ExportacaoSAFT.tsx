import { useState } from 'react';
import { FileCode, Download, CheckCircle2, Clock, ShieldCheck, Calendar, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';

export function ExportacaoSAFT() {
  const [periodo, setPeriodo] = useState({ inicio: '2026-07-01', fim: '2026-07-31' });
  const [generating, setGenerating] = useState(false);
  const [generated, setGenerated] = useState(false);

  const handleGenerate = () => {
    setGenerating(true);
    setTimeout(() => { setGenerating(false); setGenerated(true); }, 1500);
  };

  const historico = [
    { periodo: 'Junho 2026', data: '01/07/2026 09:00', tamanho: '1.2 MB', estado: 'ok' },
    { periodo: 'Maio 2026',  data: '01/06/2026 08:45', tamanho: '1.1 MB', estado: 'ok' },
    { periodo: 'Abril 2026', data: '01/05/2026 10:20', tamanho: '0.9 MB', estado: 'ok' },
  ];

  return (
    <div className="max-w-[900px] space-y-4">
      <div>
        <h1 className="text-[18px] font-semibold text-[#0F172A]">Exportação SAF-T</h1>
        <p className="text-[13px] text-[#475569] mt-0.5">Standard Audit File for Tax · Decreto Presidencial n.º 71/25</p>
      </div>

      {/* PD 71/25 notice */}
      <div className="flex items-start gap-2.5 bg-[#FFFBEB] border border-[#FDE68A] rounded-lg px-4 py-3">
        <AlertCircle style={{ width: 14, height: 14 }} className="text-[#D97706] flex-shrink-0 mt-0.5" />
        <div className="text-[12px] text-[#92400E]">
          <strong>Decreto Presidencial n.º 71/25, art. 8.º</strong> — O ficheiro SAF-T deve ser submetido à AGT até ao dia 10 do mês seguinte ao período declarado.
          Prazo para Julho 2026: <strong>10 de Agosto de 2026</strong>.
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Geração */}
        <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
          <div className="px-4 py-3 border-b border-[#E2E8F0]">
            <h2 className="text-[13px] font-semibold text-[#0F172A]">Gerar Ficheiro SAF-T</h2>
          </div>
          <div className="p-4 space-y-4">
            <div>
              <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Período de início</label>
              <input
                type="date"
                value={periodo.inicio}
                onChange={e => setPeriodo(p => ({ ...p, inicio: e.target.value }))}
                className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
              />
            </div>
            <div>
              <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Período de fim</label>
              <input
                type="date"
                value={periodo.fim}
                onChange={e => setPeriodo(p => ({ ...p, fim: e.target.value }))}
                className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
              />
            </div>

            {/* Checkboxes */}
            <div className="space-y-2">
              <p className="text-[12px] font-medium text-[#475569]">Dados a incluir</p>
              {['Lançamentos contabilísticos', 'Plano de contas', 'Clientes e fornecedores', 'Documentos fiscais'].map(item => (
                <label key={item} className="flex items-center gap-2 cursor-pointer">
                  <input type="checkbox" defaultChecked className="rounded border-[#E2E8F0] text-[#2563EB]" />
                  <span className="text-[13px] text-[#0F172A]">{item}</span>
                </label>
              ))}
            </div>

            <button
              onClick={handleGenerate}
              disabled={generating}
              className="w-full flex items-center justify-center gap-1.5 h-8 bg-[#2563EB] hover:bg-[#1D4ED8] disabled:opacity-60 text-white text-[13px] font-medium rounded-md transition-colors"
            >
              {generating ? (
                <><div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" /> A gerar...</>
              ) : (
                <><FileCode style={{ width: 13, height: 13 }} /> Gerar SAF-T XML</>
              )}
            </button>

            {generated && (
              <div className="bg-[#ECFDF5] border border-[#A7F3D0] rounded-md p-3 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <CheckCircle2 style={{ width: 14, height: 14 }} className="text-[#059669]" />
                  <span className="text-[12px] text-[#059669] font-medium">SAF-T gerado · 1.3 MB</span>
                </div>
                <button
                  onClick={() => toast.success('A transferir SAF-T...')}
                  className="flex items-center gap-1 text-[12px] font-medium text-[#2563EB] hover:text-[#1D4ED8] transition-colors"
                >
                  <Download style={{ width: 12, height: 12 }} /> Transferir
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Info + Histórico */}
        <div className="space-y-3">
          {/* Info block */}
          <div className="bg-white border border-[#E2E8F0] rounded-lg p-4 space-y-3">
            <h2 className="text-[13px] font-semibold text-[#0F172A]">Conformidade</h2>
            {[
              { icon: ShieldCheck, label: 'Certificado AGT', ok: true },
              { icon: FileCode, label: 'Formato SAF-T AO v1.0', ok: true },
              { icon: CheckCircle2, label: 'Validação PGCA', ok: true },
              { icon: Clock, label: 'Submissão automática AGT', ok: false },
            ].map(({ icon: Icon, label, ok }) => (
              <div key={label} className="flex items-center gap-2">
                <Icon style={{ width: 14, height: 14 }} className={ok ? 'text-[#059669]' : 'text-[#94A3B8]'} />
                <span className={`text-[13px] ${ok ? 'text-[#0F172A]' : 'text-[#94A3B8]'}`}>{label}</span>
                {!ok && <span className="text-[11px] text-[#D97706] bg-[#FFFBEB] px-1.5 py-0.5 rounded-full ml-auto">Em breve</span>}
              </div>
            ))}
          </div>

          {/* Histórico */}
          <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
            <div className="px-4 py-3 border-b border-[#E2E8F0]">
              <h2 className="text-[13px] font-semibold text-[#0F172A]">Histórico de Exportações</h2>
            </div>
            <table className="w-full">
              <thead>
                <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                  {['Período', 'Data', 'Tamanho', ''].map(h => (
                    <th key={h} className="px-4 py-2 text-[11px] font-medium text-[#475569] uppercase tracking-wide text-left">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {historico.map(h => (
                  <tr key={h.periodo} className="border-b border-[#F1F5F9] hover:bg-[#F8FAFC]">
                    <td className="px-4 py-2.5 text-[13px] font-medium text-[#0F172A]">{h.periodo}</td>
                    <td className="px-4 py-2.5 text-[13px] text-[#475569]">{h.data}</td>
                    <td className="px-4 py-2.5 text-[13px] text-[#475569]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{h.tamanho}</td>
                    <td className="px-4 py-2.5">
                      <button className="flex items-center gap-1 text-[12px] text-[#2563EB] hover:text-[#1D4ED8] transition-colors">
                        <Download style={{ width: 12, height: 12 }} /> Transferir
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
