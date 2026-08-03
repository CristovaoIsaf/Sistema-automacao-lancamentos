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
import { ClassificacaoContabilistica } from "./pages/ClassificacaoContabilistica";
import { Balancetes } from "./pages/Balancetes";
import { ExportacaoSAFT } from "./pages/ExportacaoSAFT";
import { Utilizadores } from "./pages/Utilizadores";
import { Auditoria } from "./pages/Auditoria";
import { Layout } from "./components/Layout";

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
      { index: true, Component: Dashboard },
      { path: "upload-documentos", Component: UploadDocumentos },
      { path: "documentos", Component: Arquivo },
      { path: "classificacao", Component: ClassificacaoContabilistica },
      { path: "lancamentos", Component: Lancamentos },
      { path: "lancamento-diario", Component: LancamentoDiario },
      { path: "balancetes", Component: Balancetes },
      { path: "plano-contas", Component: PlanoContas },
      { path: "livros-fiscais", Component: LivrosFiscais },
      { path: "saft", Component: ExportacaoSAFT },
      { path: "relatorios", Component: Relatorios },
      { path: "ia-categorizacao", Component: IACategorizacao },
      { path: "utilizadores", Component: Utilizadores },
      { path: "auditoria", Component: Auditoria },
      { path: "configuracoes", Component: Configuracoes },
    ],
  },
]);