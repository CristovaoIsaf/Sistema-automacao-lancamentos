# Sistema de Automação de Lançamentos Contabilísticos

Trabalho de Fim de Curso (TFC) — plataforma de automação contabilística para PME
angolanas, alinhada com o Plano Geral de Contabilidade Angolano (PGC-AO, Decreto
n.º 82/01). O sistema recebe documentos contabilísticos (faturas, recibos), extrai
os dados por OCR, classifica o tipo de operação (regras determinísticas com
fallback a IA) e propõe lançamentos por partidas dobradas para revisão e aprovação
por um contabilista — nunca lança nada automaticamente sem confirmação humana.

## Arquitetura

O sistema tem três componentes independentes que comunicam por HTTP:

```
frontend (React + Vite)  ──HTTP──▶  backend (Spring Boot)  ──HTTP──▶  fastapi (Python)
     porta 5173                         porta 8080                      porta 8000
                                              │                              │
                                         PostgreSQL                    Tesseract OCR
                                                                    AnythingLLM (RAG + Ollama, opcional)
```

- **`backend/`** — Spring Boot (Java 17). Fonte de verdade dos dados: utilizadores,
  empresa, documentos, entidades, sugestões da IA, lançamentos, plano de contas
  (espelhado do FastAPI), autenticação JWT e RBAC (`ADMINISTRADOR` /
  `CONTABILISTA` / `AUDITOR`).
- **`fastapi/`** — serviço de OCR e classificação (Python). Extrai texto de
  imagens/PDF (Tesseract), extrai dados estruturados por regex, valida o
  documento de forma determinística, classifica o tipo de operação (regras
  determinísticas → perfil da entidade → IA via AnythingLLM, só quando as
  etapas anteriores não decidem) e constrói as linhas do lançamento com contas
  reais do Decreto 82/01 (`app/services/pgc.py` — fonte única do plano de
  contas).
- **`frontend/`** — React 18 + TypeScript + Vite + React Router 7. Interface de
  utilizador: upload de documentos, revisão de sugestões da IA, lançamento
  manual, plano de contas, balancete/DRE/balanço, arquivo por entidade,
  auditoria.

## Pré-requisitos

- **Java 17+** e Maven (o projeto inclui o wrapper `mvnw`/`mvnw.cmd`)
- **Python 3.11+**
- **Node.js 18+** e npm
- **PostgreSQL** (local ou remoto)
- **Tesseract OCR** instalado localmente, com os pacotes de idioma `por` e `eng`
  ([instruções Windows](https://github.com/UB-Mannheim/tesseract/wiki))
- **Poppler** (só necessário para OCR de PDF —
  [poppler-windows](https://github.com/oschwartz10612/poppler-windows/releases/))
- **AnythingLLM** a correr localmente (opcional — ver [Camada de IA](#camada-de-ia-opcional))

## A correr localmente

### 1. Base de dados

Cria uma base de dados PostgreSQL vazia (por omissão o backend espera
`automacao_contabilistica` em `localhost:5432`). As tabelas são criadas
automaticamente no arranque (`spring.jpa.hibernate.ddl-auto=update`) — não há
migrações nem dados semeados.

### 2. Backend (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

Fica disponível em `http://localhost:8080`. Variáveis de ambiente (todas têm
valor por omissão para desenvolvimento local — ver
`src/main/resources/application.properties`):

| Variável | Omissão (dev) | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/automacao_contabilistica` | Ligação à BD |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `123456` | Credenciais da BD |
| `JWT_SECRET` | chave de exemplo (≥32 bytes) | **Define uma chave própria em produção** |
| `FASTAPI_SERVICES_URL` | `http://localhost:8000/api/v1` | URL do serviço FastAPI |
| `ALLOWED_ORIGIN` | `http://localhost:5173` | Origem CORS permitida |
| `PORT` | `8080` | Porta HTTP |

### 3. FastAPI (OCR + IA)

```bash
cd fastapi
pip install -r requirements.txt
cd app
uvicorn main:app --port 8000
```

Fica disponível em `http://localhost:8000`.

> **Nota**: este serviço **não corre com `--reload`** neste fluxo de
> desenvolvimento — depois de alterar código Python é preciso reiniciar o
> processo manualmente para as alterações terem efeito.

Cria `fastapi/app/.env` (ver `fastapi/app/config/settings.py` para a lista
completa) com, no mínimo:

```env
TESSERACT_CMD=C:\Program Files\Tesseract-OCR\tesseract.exe
```

As restantes variáveis (`ANYTHINGLLM_*`, `GEMINI_API_KEY`) só são necessárias
para a camada de IA — ver secção seguinte. Sem elas, o sistema classifica
documentos inteiramente por regras determinísticas (ver
[Camada de IA](#camada-de-ia-opcional)).

### 4. Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

Fica disponível em `http://localhost:5173`. Para apontar a um backend que não
seja `http://localhost:8080`, define `VITE_API_BASE_URL` (ver `.env` do Vite).

### 5. Primeiro utilizador

Não existe endpoint de registo público nem dados semeados na base de dados —
a criação de utilizadores exige já estar autenticado como `ADMINISTRADOR`
(`POST /api/utilizadores`). Para o primeiro acesso, insere uma linha
diretamente na tabela `users` (campo `senha` com hash BCrypt, `papel` =
`ADMINISTRADOR`) ou usa um teste JUnit descartável que grave o utilizador via
`UserRepository`/`PasswordEncoder` — é o padrão seguido ao longo do
desenvolvimento deste projeto (ver histórico de commits para exemplos).

## Camada de IA (opcional)

A classificação de documentos segue uma cadeia de etapas, cada uma só
acionada se a anterior não conseguir decidir (para minimizar chamadas à IA):

```
hash/duplicado → OCR (com cache) → regras determinísticas → perfil da
entidade (histórico já classificado) → cache de IA → AnythingLLM (RAG + Ollama)
```

Sem o **AnythingLLM** configurado e acessível, o sistema cai sempre no
fallback por regras determinísticas — continua funcional, só perde a
capacidade de desambiguar casos que as regras não reconhecem (nesse caso, a
sugestão fica marcada como "a classificar" para revisão manual, nunca
adivinha). Para ativar a camada de IA:

1. Corre uma instância do [AnythingLLM](https://anythingllm.com/) localmente.
2. Cria dois workspaces: um com exemplos de classificação (`regras-do-negocio`)
   e outro com o texto do Decreto 82/01 para fundamentação legal
   (`workspace-normativo`).
3. Define `ANYTHINGLLM_BASE_URL`, `ANYTHINGLLM_API_KEY`,
   `ANYTHINGLLM_WORKSPACE_EXEMPLOS` e `ANYTHINGLLM_WORKSPACE_NORMAS` no `.env`
   do FastAPI.

## Testes

```bash
# Backend
cd backend && ./mvnw test

# FastAPI
cd fastapi && pytest
```

Os testes do backend usam a mesma base de dados configurada em
`application.properties` (sem perfil de testes isolado) — corre-os contra uma
BD de desenvolvimento, não produção.

## Plano de contas

O plano de contas oficial (Decreto n.º 82/01) tem **uma única fonte**:
`fastapi/app/services/pgc.py`. O backend nunca define contas — busca-as ao
FastAPI (`GET /pgc/contas`) e cacheia-as em memória (`PlanoContasClient`).
Contas de IVA (34.5.1/34.5.2) são uma extensão explícita deste projeto — o
decreto original é anterior à introdução do IVA em Angola.

## Limitações conhecidas

- Sem movimento de existências/CMVC — a Demonstração de Resultados trata
  Compras como gasto do período inteiro, sem ajuste de stock.
- Capital Próprio (Decreto 82/01, classe 5 — Capital/Reservas) está no
  Balanço, mas este sistema não tem fecho de exercício: o resultado do
  período fica sempre em Capital Próprio como "Resultado do Exercício"
  pendente de aplicação, e sem Ativo Não Corrente modelado o Balanço pode
  ainda não fechar exatamente a zero (ver campo `diferenca` na resposta
  da API).
- Sem endpoint de registo público (ver [Primeiro utilizador](#5-primeiro-utilizador)).
