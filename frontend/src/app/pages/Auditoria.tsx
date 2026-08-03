import { ScrollText, Download } from 'lucide-react';
import type { LogAuditoria } from '../types/contabilidade';
import { useAuth, permissoes } from '../auth/AuthContext';

// Logs mock (UC010, RF017). Sem backend — dados estáticos de demonstração.
const logsMock: LogAuditoria[] = [
  { id: '1', utilizador: 'João Silva', perfil: 'CONTABILISTA', acao: 'Aprovou lançamento', entidade: 'LC-2026-0142', dataHora: '08/07/2026 09:52' },
  { id: '2', utilizador: 'João Silva', perfil: 'CONTABILISTA', acao: 'Importou documento', entidade: 'FT 002/2026', dataHora: '08/07/2026 09:48' },
  { id: '3', utilizador: 'Ana Ferreira', perfil: 'ADMINISTRADOR', acao: 'Criou utilizador', entidade: 'pedro.costa@empresa.ao', dataHora: '08/07/2026 09:30' },
  { id: '4', utilizador: 'Maria Santos', perfil: 'CONTABILISTA', acao: 'Corrigiu sugestão da IA', entidade: 'FC 031/2026', dataHora: '07/07/2026 16:25' },
  { id: '5', utilizador: 'Ana Ferreira', perfil: 'ADMINISTRADOR', acao: 'Alterou plano de contas', entidade: 'Conta 5.1.4', dataHora: '07/07/2026 14:10' },
  { id: '6', utilizador: 'João Silva', perfil: 'CONTABILISTA', acao: 'Rejeitou lançamento', entidade: 'LC-2026-0139', dataHora: '07/07/2026 11:03' },
];

const perfilBadge: Record<string, string> = {
  ADMINISTRADOR: 'bg-[#FEF2F2] text-[#DC2626]',
  CONTABILISTA: 'bg-[#EFF6FF] text-[#2563EB]',
  AUDITOR: 'bg-[#F8FAFC] text-[#64748B]',
};

export function Auditoria() {
  const { perfil } = useAuth();
  // Acesso a logs: Administrador e Auditor (UC010)
  if (!permissoes(perfil).podeVerLogs) {
    return (
      <div className="max-w-[600px]">
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-6 text-[13px] text-[#64748B]">
          Sem permissão para consultar os logs de auditoria.
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4" style={{ fontFamily: 'Inter, system-ui, sans-serif' }}>
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <ScrollText style={{ width: 18, height: 18 }} className="text-[#0F172A]" />
            <h1 className="text-[18px] font-semibold text-[#0F172A]">Logs de Auditoria</h1>
          </div>
          <p className="text-[13px] text-[#64748B] mt-0.5">
            Registo imutável das operações do sistema (UC010, RF017).
          </p>
        </div>
        <button className="flex items-center gap-1.5 h-8 px-3 bg-white border border-[#E2E8F0] hover:bg-[#F8FAFC] text-[#0F172A] text-[13px] font-medium rounded-md transition-colors">
          <Download style={{ width: 13, height: 13 }} /> Exportar
        </button>
      </div>

      {/* Table */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-[#E2E8F0] bg-[#F8FAFC]">
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold text-[#475569] uppercase tracking-wider">Utilizador</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold text-[#475569] uppercase tracking-wider">Perfil</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold text-[#475569] uppercase tracking-wider">Ação</th>
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold text-[#475569] uppercase tracking-wider">Entidade afetada</th>
              <th className="px-4 py-2.5 text-right text-[11px] font-semibold text-[#475569] uppercase tracking-wider">Data / Hora</th>
            </tr>
          </thead>
          <tbody>
            {logsMock.map(log => (
              <tr key={log.id} className="border-b border-[#F1F5F9] last:border-0 hover:bg-[#F8FAFC] transition-colors">
                <td className="px-4 py-2.5 text-[13px] text-[#0F172A]">{log.utilizador}</td>
                <td className="px-4 py-2.5">
                  <span className={`inline-flex px-2 py-0.5 rounded-full text-[11px] font-medium ${perfilBadge[log.perfil]}`}>
                    {log.perfil.charAt(0) + log.perfil.slice(1).toLowerCase()}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-[13px] text-[#475569]">{log.acao}</td>
                <td className="px-4 py-2.5 text-[13px] text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{log.entidade}</td>
                <td className="px-4 py-2.5 text-right text-[12px] text-[#64748B]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{log.dataHora}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
