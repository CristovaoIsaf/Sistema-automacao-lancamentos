import { useState } from 'react';
import { useNavigate, Link } from 'react-router';
import { FileText } from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '../auth/AuthContext';

// Fase 20 do plano de 20 fases — "registo público": antes desta fase este
// formulário nunca chamava nenhum backend, só mostrava um toast de
// sucesso falso e navegava para /login (POST /auth/registo não existia,
// ver limitação documentada no README). Agora chama o endpoint real —
// só funciona uma vez, para criar o primeiro Administrador desta
// instalação (o backend devolve 409 depois disso, ver AuthController).
export function Cadastro() {
  const navigate = useNavigate();
  const { registar } = useAuth();
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    nomeEmpresa: '', nifEmpresa: '', nome: '', nif: '', email: '', senha: '', confirmar: '',
  });

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm(prev => ({ ...prev, [k]: e.target.value }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.nomeEmpresa || !form.nifEmpresa || !form.nome || !form.nif || !form.email || !form.senha) {
      toast.error('Preencha todos os campos obrigatórios');
      return;
    }
    if (form.senha !== form.confirmar) {
      toast.error('As senhas não coincidem');
      return;
    }

    setLoading(true);
    try {
      await registar({
        nome: form.nome,
        nif: form.nif,
        email: form.email,
        senha: form.senha,
        nomeEmpresa: form.nomeEmpresa,
        nifEmpresa: form.nifEmpresa,
      });
      toast.success('Conta criada. Sessão iniciada como Administrador.');
      navigate('/');
    } catch (erro) {
      if (erro instanceof Error && erro.message.includes('409')) {
        toast.error('Esta instalação já tem utilizadores. Peça a um Administrador para criar a sua conta em "Utilizadores".');
      } else {
        toast.error('Não foi possível concluir o registo.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center p-4" style={{ fontFamily: 'Inter, system-ui, sans-serif' }}>
      <div className="w-full max-w-[440px]">
        <div className="bg-white border border-[#E2E8F0] rounded-xl p-8 shadow-sm">

          <div className="mb-6">
            <div className="flex items-center gap-2.5 mb-1">
              <div className="w-8 h-8 bg-[#2563EB] rounded-lg flex items-center justify-center">
                <FileText style={{ width: 18, height: 18 }} className="text-white" />
              </div>
              <span className="text-[#0F172A] font-bold text-[20px]">ContaIA</span>
            </div>
            <p className="text-[13px] text-[#475569] mt-2">Registar empresa — PME angolana</p>
            <p className="text-[11px] text-[#94A3B8] mt-1">
              Só funciona uma vez: cria a empresa (se ainda não existir) e a
              sua conta de Administrador. Depois disto, utilizadores
              adicionais (Contabilista, Auditor) são criados pelo
              Administrador em "Utilizadores" (RN006).
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-3">
            {[
              { key: 'nomeEmpresa', label: 'Nome da Empresa *', placeholder: 'Empresa Lda.', type: 'text', mono: false },
              { key: 'nifEmpresa', label: 'NIF da Empresa *', placeholder: '5000123456LA', type: 'text', mono: true },
              { key: 'nome', label: 'O seu nome *', placeholder: 'Nome completo', type: 'text', mono: false },
              { key: 'nif', label: 'O seu NIF *', placeholder: '5000123456LA', type: 'text', mono: true },
              { key: 'email', label: 'Email *', placeholder: 'admin@empresa.ao', type: 'email', mono: false },
              { key: 'senha', label: 'Senha *', placeholder: '••••••••', type: 'password', mono: false },
              { key: 'confirmar', label: 'Confirmar Senha *', placeholder: '••••••••', type: 'password', mono: false },
            ].map(f => (
              <div key={f.key}>
                <label className="block text-[12px] font-medium text-[#475569] mb-1.5">{f.label}</label>
                <input
                  type={f.type}
                  placeholder={f.placeholder}
                  value={form[f.key as keyof typeof form]}
                  onChange={set(f.key as keyof typeof form)}
                  className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
                  style={f.mono ? { fontFamily: 'JetBrains Mono, monospace' } : {}}
                />
              </div>
            ))}

            <button
              type="submit"
              disabled={loading}
              className="w-full h-8 bg-[#2563EB] hover:bg-[#1D4ED8] disabled:opacity-60 disabled:cursor-not-allowed text-white text-[13px] font-medium rounded-md transition-colors mt-2"
            >
              {loading ? 'A registar...' : 'Registar Empresa'}
            </button>
          </form>

          <div className="mt-5 pt-4 border-t border-[#E2E8F0]">
            <p className="text-[11px] text-[#94A3B8] text-center mt-2">
              Já tem conta?{' '}
              <Link to="/login" className="text-[#2563EB] hover:text-[#1D4ED8] transition-colors">
                Iniciar sessão
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
