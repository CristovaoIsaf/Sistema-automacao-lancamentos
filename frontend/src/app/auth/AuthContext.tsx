import { createContext, useContext, useState, ReactNode } from 'react';
import type { Perfil, Utilizador } from '../types/contabilidade';
import { login as apiLogin } from '../api/authApi';
import { setToken } from '../api/client';

// Contexto de "utilizador atual".
// Suporta login real contra o backend (POST /auth/login) e um selector de teste
// para demonstrar o controlo de acesso sem backend (RN002, RN006, RN010).

interface AuthContextValue {
  utilizador: Utilizador;
  perfil: Perfil;
  autenticado: boolean;
  setPerfil: (p: Perfil) => void;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const UTILIZADORES_MOCK: Record<Perfil, Utilizador> = {
  ADMINISTRADOR: {
    id: '1', nome: 'Ana Ferreira', email: 'ana.ferreira@empresa.ao',
    perfil: 'ADMINISTRADOR', nif: '5000123456LA', ativo: true, ultimoAcesso: '08/07/2026 09:45',
  },
  CONTABILISTA: {
    id: '2', nome: 'João Silva', email: 'joao.silva@empresa.ao',
    perfil: 'CONTABILISTA', nif: '5001234567AO', ativo: true, ultimoAcesso: '08/07/2026 08:30',
  },
  AUDITOR: {
    id: '3', nome: 'Maria Santos', email: 'maria.santos@empresa.ao',
    perfil: 'AUDITOR', nif: '5002345678AO', ativo: true, ultimoAcesso: '07/07/2026 16:20',
  },
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [perfil, setPerfil] = useState<Perfil>('ADMINISTRADOR');
  const [utilizadorReal, setUtilizadorReal] = useState<Utilizador | null>(null);
  const [autenticado, setAutenticado] = useState(false);

  const utilizador = utilizadorReal ?? UTILIZADORES_MOCK[perfil];

  async function login(email: string, password: string) {
    const resp = await apiLogin(email, password);
    setToken(resp.token);
    const p = resp.papel as Perfil;
    setPerfil(p);
    setUtilizadorReal({
      id: '-', nome: resp.nome, email: resp.email,
      perfil: p, nif: '', ativo: true, ultimoAcesso: '',
    });
    setAutenticado(true);
  }

  function logout() {
    setToken(null);
    setUtilizadorReal(null);
    setAutenticado(false);
  }

  return (
    <AuthContext.Provider value={{ utilizador, perfil, autenticado, setPerfil, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth deve ser usado dentro de <AuthProvider>');
  }
  return ctx;
}

export function permissoes(perfil: Perfil) {
  return {
    podeEscreverLancamentos: perfil === 'CONTABILISTA',
    podeGerirUtilizadores: perfil === 'ADMINISTRADOR',
    podeConfigurarSistema: perfil === 'ADMINISTRADOR',
    podeVerLogs: perfil === 'ADMINISTRADOR' || perfil === 'AUDITOR',
    apenasLeitura: perfil === 'AUDITOR',
  };
}
