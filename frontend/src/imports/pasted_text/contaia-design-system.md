Actua como um especialista sénior em Product Design e Design Systems para
aplicações empresariais (ERP/SaaS financeiro).

Vou descrever-te um sistema real. O teu trabalho é criar um design que pareça
ter sido feito por uma equipa de design profissional de uma empresa de software
empresarial — não por IA.


O SISTEMA

Nome: ContaIA
Tipo: Sistema web de automação contabilística com IA para PME angolanas
Categoria: ERP financeiro / SaaS contabilístico
Referências visuais de produto: Notion + Linear + Plane.so
(interfaces limpas, densas de informação, sem excessos decorativos)


FILOSOFIA DE DESIGN — LÊ COM ATENÇÃO

Este sistema é usado por contabilistas profissionais em ambiente de trabalho.
O design deve transmitir:


Confiança e seriedade — é software financeiro, não um app de consumo
Eficiência — o contabilista passa horas aqui, nada pode distrair
Precisão — cada elemento tem propósito, nada é decorativo por acaso
Modernidade discreta — moderno, mas não trendy; sofisticado, não chamativo


O que EVITAR a todo o custo:


Gradientes exuberantes ou fondos coloridos
Cards com sombras excessivas (shadow-xl, shadow-2xl)
Ícones grandes e decorativos sem função
Ilustrações ou imagens decorativas
Cores saturadas em grandes áreas
Tipografia com peso excessivo em tudo
Espaçamentos demasiado generosos que desperdiçam ecrã
Aspecto de "landing page" ou "dashboard de tutorial"
Qualquer coisa que pareça um template de UI kit genérico


O que FAZER:


Sidebar compacta com navegação por ícone + label
Tabelas densas mas legíveis (linha height equilibrado)
Uso de cor apenas para estados e acções críticas
Hierarquia visual através de peso tipográfico e espaçamento, não cor
Bordas subtis (1px) em vez de sombras para separar elementos
Fundo branco ou cinza muito claro (#F8FAFC) — nunca cor



SISTEMA DE CORES — SEGUE EXACTAMENTE ISTO

Background principal:    #FFFFFF
Background página:       #F8FAFC
Background sidebar:      #0F172A  (dark sidebar, como Linear/Vercel)
Sidebar texto:           #94A3B8  (inactivo)  |  #F8FAFC (activo)
Sidebar item activo:     #1E293B  (highlight)

Texto principal:         #0F172A
Texto secundário:        #475569
Texto desactivado:       #94A3B8
Bordas:                  #E2E8F0
Bordas focus:            #2563EB

Azul primário:           #2563EB  (acções principais, links, focus)
Azul hover:              #1D4ED8
Azul background suave:   #EFF6FF  (highlights, rows seleccionadas)

Verde aprovação:         #059669
Verde background:        #ECFDF5

Vermelho rejeição:       #DC2626
Vermelho background:     #FEF2F2

Laranja corrigido:       #D97706
Laranja background:      #FFFBEB

Roxo IA:                 #7C3AED  (APENAS para elementos de IA)
Roxo background:         #F5F3FF

Cinza neutro:            #64748B


TIPOGRAFIA

Font family: Inter (Google Fonts)
Monospace (valores, códigos de conta): JetBrains Mono

Escala:
  xs:   11px / Regular  — labels, badges
  sm:   12px / Regular  — texto tabela, metadados
  base: 14px / Regular  — corpo de texto padrão
  md:   14px / Medium   — labels de campo, títulos de coluna
  lg:   16px / Semibold — títulos de secção
  xl:   18px / Semibold — títulos de página
  2xl:  24px / Bold     — KPIs, números grandes


LAYOUT GLOBAL

┌─────────────────────────────────────────────────────┐
│  SIDEBAR (240px dark)  │  ÁREA PRINCIPAL             │
│                        │  ┌─────────────────────┐   │
│  Logo ContaIA          │  │ TOPBAR (48px)        │   │
│  ─────────────────     │  │ Breadcrumb | Acções  │   │
│  MENU                  │  └─────────────────────┘   │
│  > Dashboard           │                             │
│  > Documentos          │  CONTEÚDO DA PÁGINA         │
│  > Lançamentos         │  (padding 24px)             │
│  > Relatórios          │                             │
│  > Plano de Contas     │                             │
│  ─────────────────     │                             │
│  ADMIN                 │                             │
│  > Utilizadores        │                             │
│  > Auditoria           │                             │
│  > Configurações       │                             │
│  ─────────────────     │                             │
│  Avatar + Nome         │                             │
└─────────────────────────────────────────────────────┘

Sidebar: width 240px | colapsada: 56px (só ícones)
Topbar: height 48px | background branco | border-bottom 1px #E2E8F0
Conteúdo: max-width 1200px centrado | padding 24px


COMPONENTES — ESPECIFICAÇÕES

Botões

Primary:   bg #2563EB | text branco | hover #1D4ED8 | h 32px | px 12px | radius 6px
Secondary: bg branco | border 1px #E2E8F0 | text #0F172A | hover bg #F8FAFC
Danger:    bg #DC2626 | text branco | hover #B91C1C
Ghost:     sem border | text #475569 | hover bg #F1F5F9
Tamanho de fonte: 13px / Medium em todos

Tabelas

Header: bg #F8FAFC | text #475569 | font 12px/Medium | uppercase | letter-spacing 0.05em
Row: height 40px | border-bottom 1px #F1F5F9 | hover bg #F8FAFC
Texto célula: 13px / Regular / #0F172A
Valores monetários: JetBrains Mono 13px | alinhamento right
Paginação: compacta, em baixo, alinhada à direita

Cards KPI

Background: branco | border 1px #E2E8F0 | radius 8px | padding 16px
Sem sombra — apenas border
Label: 12px / Medium / #475569
Valor: 24px / Bold / #0F172A
Variação: 12px com ícone de seta (verde positivo / vermelho negativo)

Badges de estado

Aprovado:  bg #ECFDF5 | text #059669 | 11px/Medium
Rejeitado: bg #FEF2F2 | text #DC2626 | 11px/Medium
Editado:   bg #FFFBEB | text #D97706 | 11px/Medium
IA:        bg #F5F3FF | text #7C3AED | 11px/Medium
Manual:    bg #EFF6FF | text #2563EB | 11px/Medium
Padding: 2px 8px | radius: 9999px (pill)

Inputs e Forms

Height: 32px
Border: 1px #E2E8F0 | radius 6px | bg branco
Focus: border #2563EB | ring 3px #EFF6FF
Font: 13px / Regular
Label: 12px / Medium / #475569 | margin-bottom 4px
Error: border #DC2626 | mensagem 12px #DC2626 abaixo

Sidebar (dark)

Background: #0F172A
Item normal: text #64748B | padding 6px 12px | radius 6px
Item hover: bg #1E293B | text #CBD5E1
Item activo: bg #1E293B | text #F8FAFC | font Medium
Ícone: 16px | margin-right 8px
Separador: 1px #1E293B
Secção label: 10px/Medium/#475569 uppercase | margin 16px 12px 4px


ECRÃS A CRIAR (por ordem de prioridade)

ECRÃ 1 — Dashboard do Contabilista

Layout: sidebar dark + topbar + grid de KPIs + tabela recente

KPIs (4 cards em linha):


Documentos importados hoje
Sugestões IA pendentes (badge roxo com número)
Lançamentos aprovados este mês
Taxa de acerto IA (percentagem + trend)


Abaixo dos KPIs:


Tabela "Lançamentos Recentes" com colunas:
Data | Documento | Contas | Valor | Origem (badge) | Estado (badge) | Acções


Sidebar deve mostrar item "Dashboard" como activo.


ECRÃ 2 — Processamento de Documento (fluxo central)

Este é o ecrã mais importante — onde o contabilista valida a sugestão da IA.

Layout dividido em 2 colunas (50/50):

Coluna esquerda — Documento Original:


Preview do PDF/imagem (área cinza com border dashed se sem documento)
Metadados extraídos abaixo: NIF, Data, Total, IVA (em formato tabela compacta)
Badge "OCR concluído" ou "A processar..."


Coluna direita — Sugestão IA:


Header com badge roxo "Sugestão IA" + ícone de spark
Tabela de linhas de lançamento:
Conta (JetBrains Mono) | Descrição | Débito (right) | Crédito (right)
Linha de totais em bold com indicador:
✓ Equilibrado (verde) ou ✗ Desequilibrado (vermelho)
Secção colapsável "Justificação da IA" — texto do LLM em bg #F8FAFC, radius 6px
Secção colapsável "Contexto RAG" — trechos do PGCA recuperados
3 botões de acção em baixo:
[Aprovar ✓]  [Editar ✏]  [Rejeitar ✗]
Primary/Ghost/Danger respectivamente



ECRÃ 3 — Histórico de Lançamentos

Topbar com filtros inline (não em modal):
Período (date range) | Conta (select) | Origem (select) | Estado (select) | [Pesquisar]

Tabela completa com todas as colunas.
Exportar PDF/Excel — botão secundário no canto direito do topbar.


ECRÃ 4 — Gestão do Plano de Contas (Admin)

Layout de árvore à esquerda (classes 1-8) + tabela de contas à direita.
Breadcrumb: Administrador > Plano de Contas > Classe 3 — Terceiros
Botão "+ Nova Conta" no canto direito.
Tabela: Código (Mono) | Designação | Natureza | Estado (toggle) | Acções


ECRÃ 5 — Login

Centrado na página | bg #F8FAFC
Card branco 400px | border 1px #E2E8F0 | radius 12px | padding 32px
Logo ContaIA no topo (texto bold, sem imagem complexa)
Subtítulo: "Sistema de Automação Contabilística"
Campos: Email + Password
Botão: "Entrar" (primary, full width)
Link: "Esqueceu a senha?"
Footer do card: versão do sistema em cinza claro


NOTAS FINAIS PARA O DESIGNER


Nenhum gradiente em backgrounds — só cores sólidas
Sombras máximo shadow-sm (0 1px 2px rgba(0,0,0,0.05)) — nunca mais que isso
Radius consistente: 6px para inputs/buttons, 8px para cards, 12px para modais
Espaçamento base: múltiplos de 4px (4, 8, 12, 16, 20, 24, 32, 48)
O roxo (#7C3AED) é EXCLUSIVO para elementos de IA — não usar noutro contexto
Ícones: Lucide Icons — tamanho 16px inline, 20px standalone
O sistema deve parecer Linear.app ou Plane.so — não parecer Dribbble
Densidade média — não espaçoso demais, não comprimido demais
Testa o design com dados reais angolanos: NIF 5000123456LA, AOA, datas dd/mm/aaaa
O contabilista passa 8h/dia nisto — cada pixel conta para a sua produtividade

Conteúdobuild.xmlxmlmanifest.mfmfTFC-Sistema Automação de Lançamento.odtodtpdfTFC-Sistema Automação de Lançamento.odtodt# 📚 Contexto Atualizado — TFC Sistema de Automação de Lançamentos Contabilísticos com IA
**Gerado a partir da sessão de trabalho sobre modelagem UML e persistência**

---

## 👤 PERFIL DO ESTUDANTE (sem alterações)

- **Curso:** Licenciatura em Informática de Gestão Financeira (IGF)
- **Inspasted# Prompt para Criação de Design no Figma

Atue como um **Especialista Sênior em UX/UI Design, Frontend (React) e Design Systems**, com vasta experiência no desenvolvimento de interfaces modernas, profissionais e totalmente responsivas para aplicações web.

O objetivo é criar um projeto de interfpasted