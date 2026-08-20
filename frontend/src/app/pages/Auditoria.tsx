import { useEffect, useState } from 'react';
import { ScrollText, Loader2, AlertCircle } from 'lucide-react';
import type { LogAuditoria } from '../types/contabilidade';
import { useAuth, permissoes } from '../auth/AuthContext';
import { listarLogsAuditoria } from '../api/auditoriaApi';

const perfilBadge: Record<string, string> = {
  ADMINISTRADOR: 'bg-[#FEF2F2] text-[#DC2626]',
  CONTABILISTA: 'bg-[#EFF6FF] text-[#2563EB]',
  AUDITOR: 'bg-[#F8FAFC] text-[#64748B]',
};

function rotuloPerfil(perfil?: string | null): string {
  if (!perfil) return '—';
  return perfil.charAt(0) + perfil.slice(1).toLowerCase();
}

export function Auditoria() {
  const { perfil } = useAuth();
  const [logs, setLogs] = useState<LogAuditoria[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  // Fase 15 do plano de 20 fases — antes desta fase, esta página mostrava
  // sempre `logsMock` (6 registos de exemplo estáticos, nunca ligados a
  // nenhum backend). Agora deriva-se de eventos reais já timestampados no
  // sistema (uploads de documento, criação/aprovação de lançamentos — ver
  // AuditoriaService no backend).
  useEffect(() => {
    if (!permissoes(perfil).podeVerLogs) return;
    let cancelado = false;
    setLoading(true);
    setErro(null);

    listarLogsAuditoria()
      .then(dados => { if (!cancelado) setLogs(dados.itens); })
      .catch(err => {
        if (cancelado) return;
        console.error('Erro ao carregar logs de auditoria:', err);
        setErro('Não foi possível carregar os logs de auditoria.');
      })
      .finally(() => { if (!cancelado) setLoading(false); });

    return () => { cancelado = true; };
  }, [perfil]);

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
      <div>
        <div className="flex items-center gap-2">
          <ScrollText style={{ width: 18, height: 18 }} className="text-[#0F172A]" />
          <h1 className="text-[18px] font-semibold text-[#0F172A]">Logs de Auditoria</h1>
        </div>
        <p className="text-[13px] text-[#64748B] mt-0.5">
          Registo de operações do sistema (UC010, RF017) — importação de documentos e criação/aprovação de lançamentos.
        </p>
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
              <th className="px-4 py-2.5 text-left text-[11px] font-semibold text-[#475569] uppercase tracking-wider">Resultado</th>
              <th className="px-4 py-2.5 text-right text-[11px] font-semibold text-[#475569] uppercase tracking-wider">Data / Hora</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-[13px] text-[#94A3B8]">
                  <Loader2 className="animate-spin inline-block mr-2" size={14} /> A carregar...
                </td>
              </tr>
            ) : erro ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-[13px] text-[#DC2626]">
                  <AlertCircle className="inline-block mr-2" size={14} /> {erro}
                </td>
              </tr>
            ) : logs.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-[13px] text-[#94A3B8]">
                  Ainda não há eventos registados.
                </td>
              </tr>
            ) : (
              logs.map(log => (
                <tr
                  key={log.id}
                  className="border-b border-[#F1F5F9] last:border-0 hover:bg-[#F8FAFC] transition-colors"
                  title={[log.motivo, log.ip ? `IP: ${log.ip}` : null].filter(Boolean).join(' · ') || undefined}
                >
                  <td className="px-4 py-2.5 text-[13px] text-[#0F172A]">{log.utilizador}</td>
                  <td className="px-4 py-2.5">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-[11px] font-medium ${(log.perfil && perfilBadge[log.perfil]) ?? 'bg-[#F1F5F9] text-[#475569]'}`}>
                      {rotuloPerfil(log.perfil)}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 text-[13px] text-[#475569]">{log.acao}</td>
                  <td className="px-4 py-2.5 text-[13px] text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{log.entidade}</td>
                  <td className="px-4 py-2.5">
                    {log.resultado && (
                      <span className={`inline-flex px-2 py-0.5 rounded-full text-[11px] font-medium ${log.resultado === 'FALHA' ? 'bg-[#FEF2F2] text-[#DC2626]' : 'bg-[#ECFDF5] text-[#059669]'}`}>
                        {log.resultado === 'FALHA' ? 'Falha' : 'Sucesso'}
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-2.5 text-right text-[12px] text-[#64748B]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                    {log.dataHora ? new Date(log.dataHora).toLocaleString('pt-AO') : '—'}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
