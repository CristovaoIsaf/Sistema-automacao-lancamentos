import { createBrowserRouter } from "react-router";
import { Login } from "./pages/Login";
import { Cadastro } from "./pages/Cadastro";
import { Dashboard } from "./pages/Dashboard";
import { Lancamentos } from "./pages/Lancamentos";
import { LancamentoDiario } from "./pages/LancamentoDiario";
import { PlanoContas } from "./pages/PlanoContas";
import { LivrosFiscais } from "./pages/LivrosFiscais";
import { Relatorios } from "./pages/Relatorios";
import { IACategorizacao } from "./pages/IACategorizacao";
import { Configuracoes } from "./pages/Configuracoes";
import { UploadDocumentos } from "./pages/UploadDocumentos";
import { Arquivo } from "./pages/Arquivo";
import { Balancetes } from "./pages/Balancetes";
import { ExportacaoSAFT } from "./pages/ExportacaoSAFT";
import { Utilizadores } from "./pages/Utilizadores";
import { Auditoria } from "./pages/Auditoria";
import { Layout } from "./components/Layout";
import { RotaProtegida } from "./auth/RotaProtegida";

// Fase 1 — cada página fica envolvida em <RotaProtegida>, que consulta
// PERFIS_POR_ROTA (auth/permissoesRotas.ts) e bloqueia a navegação se o
// perfil autenticado não tiver acesso àquela rota — não é só o menu a
// esconder o link (ver Layout.tsx).
export const router = createBrowserRouter([
  {
    path: "/login",
    Component: Login,
  },
  {
    path: "/cadastro",
    Component: Cadastro,
  },
  {
    path: "/",
    Component: Layout,
    children: [
      { index: true, element: <RotaProtegida><Dashboard /></RotaProtegida> },
      { path: "upload-documentos", element: <RotaProtegida><UploadDocumentos /></RotaProtegida> },
      { path: "documentos", element: <RotaProtegida><Arquivo /></RotaProtegida> },
      { path: "lancamentos", element: <RotaProtegida><Lancamentos /></RotaProtegida> },
      { path: "lancamento-diario", element: <RotaProtegida><LancamentoDiario /></RotaProtegida> },
      { path: "balancetes", element: <RotaProtegida><Balancetes /></RotaProtegida> },
      { path: "plano-contas", element: <RotaProtegida><PlanoContas /></RotaProtegida> },
      { path: "livros-fiscais", element: <RotaProtegida><LivrosFiscais /></RotaProtegida> },
      { path: "saft", element: <RotaProtegida><ExportacaoSAFT /></RotaProtegida> },
      { path: "relatorios", element: <RotaProtegida><Relatorios /></RotaProtegida> },
      { path: "ia-categorizacao", element: <RotaProtegida><IACategorizacao /></RotaProtegida> },
      { path: "utilizadores", element: <RotaProtegida><Utilizadores /></RotaProtegida> },
      { path: "auditoria", element: <RotaProtegida><Auditoria /></RotaProtegida> },
      { path: "configuracoes", element: <RotaProtegida><Configuracoes /></RotaProtegida> },
    ],
  },
]);