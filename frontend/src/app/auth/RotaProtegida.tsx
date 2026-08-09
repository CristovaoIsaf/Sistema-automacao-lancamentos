import { useEffect } from 'react';
import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router';
import { toast } from 'sonner';
import { useAuth } from './AuthContext';
import { PERFIS_POR_ROTA } from './permissoesRotas';

// Fase 1 — bloqueia mesmo a navegação por perfil (não só esconde o link no
// menu, ver Layout.tsx). Antes, um utilizador autenticado com qualquer
// perfil conseguia aceder a /utilizadores, /configuracoes, etc. digitando
// o URL directamente — o menu escondia o link, mas a rota em si nunca
// verificava o perfil.
export function RotaProtegida({ children }: { children: ReactNode }) {
  const { perfil } = useAuth();
  const location = useLocation();
  const permitidos = PERFIS_POR_ROTA[location.pathname];
  const bloqueado = permitidos !== undefined && !permitidos.includes(perfil);

  useEffect(() => {
    if (bloqueado) {
      toast.error('O teu perfil não tem acesso a esta página.');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bloqueado, location.pathname]);

  if (bloqueado) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
