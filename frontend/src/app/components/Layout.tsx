import { Link, Navigate, Outlet, useLocation, useNavigate } from 'react-router';
import {
  LayoutDashboard,
  FileText,
  BookOpen,
  BarChart3,
  Brain,
  Settings,
  Menu,
  X,
  Upload,
  Scale,
  FileCode,
  Users,
  ChevronLeft,
  ChevronRight,
  Bell,
  Search,
  LogOut,
  ScrollText,
  FolderOpen,
  NotebookPen,
} from 'lucide-react';
import { useState } from 'react';
import type { Perfil } from '../types/contabilidade';
import { useAuth } from '../auth/AuthContext';
import { PERFIS_POR_ROTA } from '../auth/permissoesRotas';

// Fase 1 — os perfis de cada item vêm de PERFIS_POR_ROTA (auth/permissoesRotas.ts),
// a mesma tabela usada por RotaProtegida.tsx para bloquear a navegação
// directa por URL. Antes, esta lista de perfis estava duplicada aqui e não
// tinha nenhuma verificação correspondente ao nível da rota.
type MenuItem = {
  path: string;
  label: string;
  icon: React.ElementType;
  badge?: number;
};

const menuSections: { label: string; items: MenuItem[] }[] = [
  {
    label: 'PRINCIPAL',
    items: [
      { path: '/', label: 'Dashboard', icon: LayoutDashboard },
      // Importar documentos e um acto de Contabilista (UC004)
      { path: '/upload-documentos', label: 'Novo Documento', icon: Upload },
      // Arquivo organizado por entidade — só leitura, por isso o Auditor
      // também acede (RN010 só exclui a escrita de documentos ao Auditor).
      { path: '/documentos', label: 'Documentos', icon: FolderOpen },
      { path: '/lancamentos', label: 'Lançamentos', icon: FileText },
      // Registo manual de partidas dobradas — acto de escrita, mesma regra
      // de perfis que "Novo Documento" (RN010: Auditor não escreve).
      { path: '/lancamento-diario', label: 'Lançamento Manual', icon: NotebookPen },
      { path: '/balancetes', label: 'Balancetes', icon: Scale },
      { path: '/relatorios', label: 'Relatórios', icon: BarChart3 },
      // Consulta de logs de auditoria (UC010) — Administrador e Auditor
      { path: '/auditoria', label: 'Auditoria', icon: ScrollText },
    ],
  },
  {
    label: 'FISCAL',
    items: [
      { path: '/livros-fiscais', label: 'Livros Fiscais', icon: BookOpen },
      { path: '/saft', label: 'Exportação SAF-T', icon: FileCode },
      { path: '/ia-categorizacao', label: 'IA Categorização', icon: Brain, badge: 7 },
    ],
  },
  {
    label: 'ADMINISTRADOR',
    items: [
      { path: '/plano-contas', label: 'Plano de Contas', icon: BookOpen },
      { path: '/utilizadores', label: 'Utilizadores', icon: Users },
      { path: '/configuracoes', label: 'Configurações', icon: Settings },
    ],
  },
];

export function Layout() {
  const location = useLocation();
  const { utilizador, perfil, autenticado, setPerfil, logout } = useAuth();
  const navigate = useNavigate();

  if (!autenticado) {
    return <Navigate to="/login" replace />;
  }
  const handleLogout = () => { logout(); navigate('/login'); };
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  const isActive = (path: string) => {
    if (path === '/') return location.pathname === '/';
    return location.pathname.startsWith(path);
  };

  // RBAC: mostra só as secções/itens permitidos ao perfil activo, e secções vazias somem
  const seccoesVisiveis = menuSections
    .map(s => ({ ...s, items: s.items.filter(i => (PERFIS_POR_ROTA[i.path] ?? []).includes(perfil)) }))
    .filter(s => s.items.length > 0);

  const activeLabel = seccoesVisiveis
    .flatMap(s => s.items)
    .find(i => isActive(i.path))?.label ?? 'Dashboard';

  // Iniciais do utilizador actual para os avatares
  const iniciais = utilizador.nome.split(' ').map(n => n[0]).slice(0, 2).join('');
  const perfilLabel = perfil.charAt(0) + perfil.slice(1).toLowerCase();

  const sidebarW = collapsed ? 'w-14' : 'w-60';

  return (
    <div className="flex h-screen overflow-hidden bg-[#F8FAFC]" style={{ fontFamily: 'Inter, system-ui, sans-serif' }}>

      {/* ── SIDEBAR ────────────────────────────────────────────── */}
      <aside
        className={`hidden md:flex flex-col flex-shrink-0 ${sidebarW} bg-[#0F172A] transition-all duration-200 overflow-hidden`}
        style={{ minHeight: '100vh' }}
      >
        {/* Logo */}
        <div className="flex items-center h-12 px-3 border-b border-[#1E293B] flex-shrink-0">
          {!collapsed && (
            <div className="flex items-center gap-2 flex-1 min-w-0">
              <div className="w-6 h-6 bg-[#2563EB] rounded flex items-center justify-center flex-shrink-0">
                <FileText className="w-3.5 h-3.5 text-white" />
              </div>
              <span className="text-[#F8FAFC] font-semibold text-[14px] truncate">ContaIA</span>
            </div>
          )}
          {collapsed && (
            <div className="w-6 h-6 bg-[#2563EB] rounded flex items-center justify-center mx-auto">
              <FileText className="w-3.5 h-3.5 text-white" />
            </div>
          )}
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto py-3 space-y-4">
          {seccoesVisiveis.map((section) => (
            <div key={section.label}>
              {!collapsed && (
                <p className="px-3 mb-1 text-[10px] font-medium text-[#475569] uppercase tracking-wider">
                  {section.label}
                </p>
              )}
              <div className="space-y-0.5 px-2">
                {section.items.map((item) => {
                  const Icon = item.icon;
                  const active = isActive(item.path);
                  return (
                    <Link
                      key={item.path}
                      to={item.path}
                      title={collapsed ? item.label : undefined}
                      className={`flex items-center gap-2 px-2 py-1.5 rounded-md transition-colors text-[13px] ${
                        active
                          ? 'bg-[#1E293B] text-[#F8FAFC] font-medium'
                          : 'text-[#64748B] hover:bg-[#1E293B] hover:text-[#CBD5E1]'
                      } ${collapsed ? 'justify-center' : ''}`}
                    >
                      <Icon className="w-4 h-4 flex-shrink-0" />
                      {!collapsed && <span className="truncate flex-1">{item.label}</span>}
                      {!collapsed && 'badge' in item && item.badge ? (
                        <span className="flex-shrink-0 min-w-[18px] h-[18px] px-1 flex items-center justify-center bg-[#7C3AED] text-white rounded-full text-[10px] font-semibold leading-none">
                          {item.badge}
                        </span>
                      ) : null}
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* User + Collapse */}
        <div className="border-t border-[#1E293B] flex-shrink-0">
          {!collapsed && (
            <div className="px-3 py-3 space-y-2">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-full bg-[#1E293B] flex items-center justify-center flex-shrink-0">
                  <span className="text-[#94A3B8] text-[11px] font-medium">{iniciais}</span>
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-[#CBD5E1] text-[12px] font-medium truncate">{utilizador.nome}</p>
                  <p className="text-[#64748B] text-[11px] truncate">{perfilLabel}</p>
                </div>
                <button onClick={handleLogout} className="text-[#64748B] hover:text-[#CBD5E1] transition-colors" title="Terminar sessão">
                  <LogOut className="w-3.5 h-3.5" />
                </button>
              </div>
              {/* Selector de teste de perfil (mock — sem backend). Permite validar o RBAC. */}
              <div>
                <label className="block text-[9px] uppercase tracking-wider text-[#475569] mb-1">Perfil (teste)</label>
                <select
                  value={perfil}
                  onChange={e => setPerfil(e.target.value as Perfil)}
                  className="w-full h-7 px-2 text-[11px] bg-[#1E293B] text-[#CBD5E1] border border-[#334155] rounded focus:outline-none"
                >
                  <option value="ADMINISTRADOR">Administrador</option>
                  <option value="CONTABILISTA">Contabilista</option>
                  <option value="AUDITOR">Auditor</option>
                </select>
              </div>
            </div>
          )}
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="w-full flex items-center justify-center py-2 text-[#64748B] hover:text-[#CBD5E1] hover:bg-[#1E293B] transition-colors"
          >
            {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
          </button>
        </div>
      </aside>

      {/* ── MOBILE OVERLAY ──────────────────────────────────────── */}
      {mobileOpen && (
        <div
          className="md:hidden fixed inset-0 z-50 bg-black/60"
          onClick={() => setMobileOpen(false)}
        >
          <div
            className="w-60 h-full bg-[#0F172A] flex flex-col"
            onClick={e => e.stopPropagation()}
          >
            <div className="flex items-center justify-between h-12 px-3 border-b border-[#1E293B]">
              <div className="flex items-center gap-2">
                <div className="w-6 h-6 bg-[#2563EB] rounded flex items-center justify-center">
                  <FileText className="w-3.5 h-3.5 text-white" />
                </div>
                <span className="text-[#F8FAFC] font-semibold text-[14px]">ContaIA</span>
              </div>
              <button onClick={() => setMobileOpen(false)} className="text-[#64748B]">
                <X className="w-4 h-4" />
              </button>
            </div>
            <nav className="flex-1 overflow-y-auto py-3 space-y-4">
              {seccoesVisiveis.map((section) => (
                <div key={section.label}>
                  <p className="px-3 mb-1 text-[10px] font-medium text-[#475569] uppercase tracking-wider">
                    {section.label}
                  </p>
                  <div className="space-y-0.5 px-2">
                    {section.items.map((item) => {
                      const Icon = item.icon;
                      const active = isActive(item.path);
                      return (
                        <Link
                          key={item.path}
                          to={item.path}
                          onClick={() => setMobileOpen(false)}
                          className={`flex items-center gap-2 px-2 py-1.5 rounded-md transition-colors text-[13px] ${
                            active
                              ? 'bg-[#1E293B] text-[#F8FAFC] font-medium'
                              : 'text-[#64748B] hover:bg-[#1E293B] hover:text-[#CBD5E1]'
                          }`}
                        >
                          <Icon className="w-4 h-4" />
                          <span>{item.label}</span>
                        </Link>
                      );
                    })}
                  </div>
                </div>
              ))}
            </nav>
          </div>
        </div>
      )}

      {/* ── MAIN AREA ───────────────────────────────────────────── */}
      <div className="flex flex-col flex-1 min-w-0 overflow-hidden">

        {/* Topbar */}
        <header className="min-h-12 bg-white border-b border-[#E2E8F0] flex flex-wrap items-center px-3 sm:px-4 py-2 gap-2 sm:gap-4 flex-shrink-0 z-10">
          <button
            className="md:hidden text-[#475569] hover:text-[#0F172A]"
            onClick={() => setMobileOpen(true)}
          >
            <Menu className="w-5 h-5" />
          </button>

          {/* Breadcrumb */}
          <div className="flex items-center gap-1.5 text-[13px] text-[#475569] flex-1 min-w-0 order-3 sm:order-none">
            <span className="text-[#94A3B8]">ContaIA</span>
            <span className="text-[#CBD5E1]">/</span>
            <span className="text-[#0F172A] font-medium truncate">{activeLabel}</span>
          </div>

          <div className="flex items-center gap-2 ml-auto">
            <button className="w-7 h-7 flex items-center justify-center text-[#64748B] hover:text-[#0F172A] hover:bg-[#F8FAFC] rounded transition-colors">
              <Search className="w-4 h-4" />
            </button>
            <button className="relative w-7 h-7 flex items-center justify-center text-[#64748B] hover:text-[#0F172A] hover:bg-[#F8FAFC] rounded transition-colors">
              <Bell className="w-4 h-4" />
              <span className="absolute top-1 right-1 w-1.5 h-1.5 bg-[#DC2626] rounded-full" />
            </button>
            <div className="w-px h-5 bg-[#E2E8F0]" />
            <div className="w-7 h-7 rounded-full bg-[#EFF6FF] flex items-center justify-center">
              <span className="text-[#2563EB] text-[11px] font-medium">{iniciais}</span>
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-3 sm:p-6 lg:p-8">
          <div className="mx-auto w-full max-w-7xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
