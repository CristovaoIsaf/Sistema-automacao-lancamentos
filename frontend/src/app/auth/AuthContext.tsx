import { createContext, useContext, useState, ReactNode } from 'react';
import type { Perfil, Utilizador } from '../types/contabilidade';
import { login as apiLogin } from '../api/authApi';
import { registar as apiRegistar, type CadastroRequest } from '../api/cadastroApi';
import { setToken } from '../api/client';

// Contexto de "utilizador atual". O perfil vem sempre do backend (JWT
// devolvido por POST /auth/login ou /auth/registo) — não é escolhido pelo
// cliente, para que o RBAC do frontend não possa ser contornado trocando
// de perfil localmente.

interface AuthContextValue {
  utilizador: Utilizador;
  perfil: Perfil;
  autenticado: boolean;
  login: (email: string, password: string) => Promise<void>;
  registar: (dados: CadastroRequest) => Promise<void>;
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

  function aplicarSessao(resp: { id: number; token: string; nome: string; email: string; papel: string }) {
    setToken(resp.token);
    const p = resp.papel as Perfil;
    setPerfil(p);
    setUtilizadorReal({
      // Auditoria C01/C03 — id real do utilizador (antes ficava sempre
      // "-"): Lancamentos.tsx usa-o para saber se pode aprovar um
      // lançamento/anulação que ele próprio criou/pediu.
      id: String(resp.id), nome: resp.nome, email: resp.email,
      perfil: p, nif: '', ativo: true, ultimoAcesso: '',
    });
    setAutenticado(true);
  }

  async function login(email: string, password: string) {
    aplicarSessao(await apiLogin(email, password));
  }

  // Fase 20 do plano de 20 fases — POST /auth/registo devolve a mesma
  // forma de LoginResposta que o login (ver AuthController.registar), por
  // isso a sessão é aplicada da mesma maneira — sem pedir login outra vez
  // a seguir ao registo.
  async function registar(dados: CadastroRequest) {
    aplicarSessao(await apiRegistar(dados));
  }

  function logout() {
    setToken(null);
    setUtilizadorReal(null);
    setAutenticado(false);
  }

  return (
    <AuthContext.Provider value={{ utilizador, perfil, autenticado, login, registar, logout }}>
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
