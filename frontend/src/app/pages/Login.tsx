import { useState } from 'react';
import { useNavigate, Link } from 'react-router';
import { FileText, Eye, EyeOff } from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '../auth/AuthContext';

export function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !senha) {
      toast.error('Preencha todos os campos');
      return;
    }
    setLoading(true);
    try {
      await login(email, senha);
      toast.success('Bem-vindo ao ContaIA');
      navigate('/');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Erro ao iniciar sessão');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center p-4" style={{ fontFamily: 'Inter, system-ui, sans-serif' }}>
      <div className="w-full max-w-[400px]">

        {/* Card */}
        <div className="bg-white border border-[#E2E8F0] rounded-xl p-8 shadow-sm">

          {/* Logo */}
          <div className="mb-8">
            <div className="flex items-center gap-2.5 mb-1">
              <div className="w-8 h-8 bg-[#2563EB] rounded-lg flex items-center justify-center">
                <FileText className="w-4.5 h-4.5 text-white" style={{ width: 18, height: 18 }} />
              </div>
              <span className="text-[#0F172A] font-bold text-[20px]">ContaIA</span>
            </div>
            <p className="text-[#475569] text-[13px] mt-2">Sistema de Automação Contabilística</p>
          </div>

          {/* Form */}
          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-[12px] font-medium text-[#475569] mb-1.5">
                Email
              </label>
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="contador@empresa.ao"
                className="w-full h-8 px-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
              />
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="text-[12px] font-medium text-[#475569]">
                  Senha
                </label>
                <a href="#" className="text-[12px] text-[#2563EB] hover:text-[#1D4ED8] transition-colors">
                  Esqueceu a senha?
                </a>
              </div>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={senha}
                  onChange={e => setSenha(e.target.value)}
                  placeholder="••••••••"
                  className="w-full h-8 px-3 pr-9 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[#94A3B8] hover:text-[#475569] transition-colors"
                >
                  {showPassword ? <EyeOff style={{ width: 14, height: 14 }} /> : <Eye style={{ width: 14, height: 14 }} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full h-8 bg-[#2563EB] hover:bg-[#1D4ED8] disabled:opacity-60 text-white text-[13px] font-medium rounded-md transition-colors mt-2"
            >
              {loading ? 'A entrar...' : 'Entrar'}
            </button>
          </form>

          {/* Footer card */}
          <div className="mt-6 pt-5 border-t border-[#E2E8F0]">
            <p className="text-[11px] text-[#94A3B8] text-center mt-2">
              Ainda não tem conta?{' '}
              <Link to="/cadastro" className="text-[#2563EB] hover:text-[#1D4ED8] transition-colors">
                Criar conta
              </Link>
            </p>
          </div>
        </div>

        {/* Version */}
        <p className="text-center text-[11px] text-[#94A3B8] mt-4">v1.0.0 · PGCA (Decreto n.º 82/01)</p>
      </div>
    </div>
  );
}
