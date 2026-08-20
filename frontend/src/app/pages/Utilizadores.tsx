import { useEffect, useState } from 'react';
import { Plus, Edit2, Shield, User, UserCog, UserCheck, UserX, X, Check, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import * as Dialog from '@radix-ui/react-dialog';
import type { Perfil, Utilizador } from '../types/contabilidade';
import { useAuth, permissoes } from '../auth/AuthContext';
import { alternarEstadoUtilizador, criarUtilizador, listarUtilizadores } from '../api/utilizadorApi';

// Taxonomia alinhada ao TFC: exactamente 3 perfis (Administrador, Contabilista, Auditor).
type Role = Perfil;

const roleConfig: Record<Role, { label: string; color: string; icon: React.ElementType; desc: string }> = {
  ADMINISTRADOR: { label: 'Administrador', color: 'bg-[#FEF2F2] text-[#DC2626]', icon: Shield, desc: 'Acesso total ao sistema' },
  CONTABILISTA:  { label: 'Contabilista',  color: 'bg-[#EFF6FF] text-[#2563EB]', icon: UserCog, desc: 'Importa, valida e regista lançamentos' },
  AUDITOR:       { label: 'Auditor',       color: 'bg-[#F8FAFC] text-[#64748B]', icon: User, desc: 'Apenas leitura (histórico, relatórios, logs)' },
};

function NovoUtilizadorDialog({ open, onClose, onCriado }: { open: boolean; onClose: () => void; onCriado: () => void }) {
  const [form, setForm] = useState({ nome: '', email: '', nif: '', senha: '', perfil: 'CONTABILISTA' as Role });
  const [salvando, setSalvando] = useState(false);
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm(prev => ({ ...prev, [k]: e.target.value }));

  const handleCriar = async () => {
    if (!form.nome || !form.email || !form.nif || !form.senha) {
      toast.error('Preencha todos os campos');
      return;
    }

    try {
      setSalvando(true);
      await criarUtilizador(form);
      toast.success('Utilizador criado com sucesso');
      setForm({ nome: '', email: '', nif: '', senha: '', perfil: 'CONTABILISTA' });
      onCriado();
      onClose();
    } catch (error) {
      console.error(error);
      toast.error('Erro ao criar utilizador');
    } finally {
      setSalvando(false);
    }
  };

  return (
    <Dialog.Root open={open} onOpenChange={v => !v && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/30 z-50" />
        <Dialog.Content className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-[420px] bg-white border border-[#E2E8F0] rounded-xl shadow-sm" style={{ fontFamily: 'Inter, system-ui, sans-serif' }}>
          <div className="flex items-center justify-between px-5 py-3.5 border-b border-[#E2E8F0]">
            <Dialog.Title className="text-[14px] font-semibold text-[#0F172A]">Novo Utilizador</Dialog.Title>
            <button onClick={onClose} className="w-6 h-6 flex items-center justify-center text-[#94A3B8] hover:text-[#0F172A] hover:bg-[#F1F5F9] rounded transition-colors">
              <X style={{ width: 14, height: 14 }} />
            </button>
          </div>
          <div className="p-5 space-y-3">
            {[
              { key: 'nome', label: 'Nome completo', placeholder: 'Ana Ferreira', type: 'text', mono: false },
              { key: 'email', label: 'Email', placeholder: 'ana@empresa.ao', type: 'email', mono: false },
              { key: 'nif', label: 'NIF', placeholder: '5000123456LA', type: 'text', mono: true },
              { key: 'senha', label: 'Senha provisória', placeholder: '••••••••', type: 'password', mono: false },
            ].map(f => (
              <div key={f.key}>
                <label className="block text-[12px] font-medium text-[#475569] mb-1.5">{f.label}</label>
                <input
                  type={f.type}
                  placeholder={f.placeholder}
                  value={form[f.key as 'nome' | 'email' | 'nif' | 'senha']}
                  onChange={set(f.key as keyof typeof form)}
                  className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
                  style={f.mono ? { fontFamily: 'JetBrains Mono, monospace' } : {}}
                />
              </div>
            ))}
            <div>
              <label className="block text-[12px] font-medium text-[#475569] mb-1.5">Perfil</label>
              <select value={form.perfil} onChange={set('perfil')}
                className="w-full h-8 px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all">
                <option value="ADMINISTRADOR">Administrador</option>
                <option value="CONTABILISTA">Contabilista</option>
                <option value="AUDITOR">Auditor</option>
              </select>
            </div>
          </div>
          <div className="flex items-center justify-end gap-2 px-5 py-3.5 border-t border-[#E2E8F0]">
            <button onClick={onClose} className="h-8 px-3 bg-white border border-[#E2E8F0] hover:bg-[#F8FAFC] text-[#0F172A] text-[13px] font-medium rounded-md transition-colors">
              Cancelar
            </button>
            <button onClick={handleCriar} disabled={salvando}
              className="flex items-center gap-1.5 h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-[13px] font-medium rounded-md transition-colors disabled:opacity-50">
              {salvando ? <Loader2 className="animate-spin" style={{ width: 13, height: 13 }} /> : <Check style={{ width: 13, height: 13 }} />} Criar
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

export function Utilizadores() {
  const { perfil: perfilAtual } = useAuth();
  const podeGerir = permissoes(perfilAtual).podeGerirUtilizadores;
  const [search, setSearch] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [utilizadores, setUtilizadores] = useState<Utilizador[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [alternandoId, setAlternandoId] = useState<string | null>(null);

  const carregar = async () => {
    try {
      setLoading(true);
      setErro(null);
      setUtilizadores(await listarUtilizadores());
    } catch (err) {
      console.error(err);
      setErro('Não foi possível carregar os utilizadores.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregar();
  }, []);

  // Auditoria C09 — suspender/reativar um utilizador sem o eliminar
  // fisicamente (o único modo que existia antes desta correção, ver
  // UserController.apagar): mantém o histórico de auditoria associado a
  // ele intacto (ver AuditoriaService — apagar fisicamente fazia esse
  // histórico mostrar "Utilizador desconhecido").
  const alternarEstado = async (u: Utilizador) => {
    try {
      setAlternandoId(u.id);
      await alternarEstadoUtilizador(u);
      toast.success(u.ativo ? 'Utilizador desativado' : 'Utilizador ativado');
      await carregar();
    } catch (error) {
      console.error(error);
      toast.error('Erro ao alterar o estado do utilizador');
    } finally {
      setAlternandoId(null);
    }
  };

  const filtered = utilizadores.filter(u =>
    u.nome.toLowerCase().includes(search.toLowerCase()) ||
    u.email.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="max-w-[1200px] space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-[18px] font-semibold text-[#0F172A]">Gestão de Utilizadores</h1>
          <p className="text-[13px] text-[#475569] mt-0.5">{utilizadores.filter(u => u.ativo).length} activos · {utilizadores.length} total</p>
        </div>
        {podeGerir && (
          <button
            onClick={() => setDialogOpen(true)}
            className="flex items-center gap-1.5 h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-[13px] font-medium rounded-md transition-colors"
          >
            <Plus style={{ width: 13, height: 13 }} /> Novo Utilizador
          </button>
        )}
      </div>

      {/* Perfis summary */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        {(Object.entries(roleConfig) as [Role, typeof roleConfig[Role]][]).map(([role, cfg]) => {
          const Icon = cfg.icon;
          const count = utilizadores.filter(u => u.perfil === role).length;
          return (
            <div key={role} className="bg-white border border-[#E2E8F0] rounded-lg p-3">
              <div className="flex items-center gap-2 mb-1.5">
                <div className="w-6 h-6 bg-[#F8FAFC] rounded flex items-center justify-center">
                  <Icon style={{ width: 13, height: 13 }} className="text-[#64748B]" />
                </div>
                <span className="text-[12px] font-medium text-[#475569]">{cfg.label}</span>
              </div>
              <p className="text-[20px] font-bold text-[#0F172A]">{count}</p>
              <p className="text-[11px] text-[#94A3B8] mt-0.5">{cfg.desc}</p>
            </div>
          );
        })}
      </div>

      {/* Search + Table */}
      <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
        <div className="px-4 py-3 border-b border-[#E2E8F0] flex items-center gap-3">
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Pesquisar por nome ou email..."
            className="flex-1 h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
          />
        </div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                {['Nome', 'Email', 'NIF', 'Perfil', 'Último Acesso', 'Estado', 'Acções'].map(h => (
                  <th key={h} className="px-4 py-2.5 text-left text-[11px] font-medium text-[#475569] uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-[13px] text-[#94A3B8]">
                    <Loader2 className="animate-spin inline-block mr-2" size={14} /> A carregar...
                  </td>
                </tr>
              ) : erro ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-[13px] text-[#DC2626]">{erro}</td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-[13px] text-[#94A3B8]">Nenhum utilizador encontrado</td>
                </tr>
              ) : filtered.map(u => {
                const cfg = roleConfig[u.perfil];
                return (
                  <tr key={u.id} className="border-b border-[#F1F5F9] hover:bg-[#F8FAFC] transition-colors">
                    <td className="px-4 py-2.5">
                      <div className="flex items-center gap-2">
                        <div className="w-7 h-7 rounded-full bg-[#EFF6FF] flex items-center justify-center flex-shrink-0">
                          <span className="text-[#2563EB] text-[11px] font-medium">{u.nome.split(' ').map(n => n[0]).join('').slice(0, 2)}</span>
                        </div>
                        <span className="text-[13px] font-medium text-[#0F172A]">{u.nome}</span>
                      </div>
                    </td>
                    <td className="px-4 py-2.5 text-[13px] text-[#475569]">{u.email}</td>
                    <td className="px-4 py-2.5 text-[13px] text-[#475569]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{u.nif}</td>
                    <td className="px-4 py-2.5">
                      <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium ${cfg.color}`}>
                        {cfg.label}
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-[13px] text-[#475569]">{u.ultimoAcesso}</td>
                    <td className="px-4 py-2.5">
                      <span className={`inline-flex px-2 py-0.5 rounded-full text-[11px] font-medium ${u.ativo ? 'bg-[#ECFDF5] text-[#059669]' : 'bg-[#F1F5F9] text-[#94A3B8]'}`}>
                        {u.ativo ? 'Ativo' : 'Inativo'}
                      </span>
                    </td>
                    <td className="px-4 py-2.5">
                      {podeGerir && (
                        <div className="flex items-center gap-1">
                          <button className="w-6 h-6 flex items-center justify-center text-[#64748B] hover:text-[#0F172A] hover:bg-[#F1F5F9] rounded transition-colors">
                            <Edit2 style={{ width: 13, height: 13 }} />
                          </button>
                          <button
                            onClick={() => alternarEstado(u)}
                            disabled={alternandoId === u.id}
                            title={u.ativo ? 'Desativar utilizador' : 'Ativar utilizador'}
                            className="w-6 h-6 flex items-center justify-center text-[#64748B] hover:text-[#0F172A] hover:bg-[#F1F5F9] rounded transition-colors disabled:opacity-50"
                          >
                            {alternandoId === u.id ? (
                              <Loader2 className="animate-spin" style={{ width: 13, height: 13 }} />
                            ) : u.ativo ? (
                              <UserX style={{ width: 13, height: 13 }} />
                            ) : (
                              <UserCheck style={{ width: 13, height: 13 }} />
                            )}
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      <NovoUtilizadorDialog open={dialogOpen} onClose={() => setDialogOpen(false)} onCriado={carregar} />
    </div>
  );
}
