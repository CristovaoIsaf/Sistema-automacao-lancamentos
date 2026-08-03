# Integração com o AnythingLLM (substitui o RAG à mão)

## O que mudou

Antes, `document_analyzer.py` fazia RAG "à mão": `rag/retriever.py` calculava
embeddings via Ollama (`nomic-embed-text`) sobre `rag/pgc_corpus.jsonl`,
recuperava os excertos mais relevantes por similaridade de cosseno, e
`services/llm_local.py` chamava o Ollama directamente (`llama3.2:1b`) com
esse contexto para classificar o documento.

Agora, essa camada (retrieval + chamada ao LLM) foi substituída por chamadas
à API REST do **AnythingLLM** (`services/anythingllm_client.py`), que já gere
por trás o Ollama, os embeddings e o vector store (LanceDB).

**O que NÃO mudou**: `services/pgc.py` (mapeamento tipo de transacção → conta
oficial do Decreto 82/01, construção das linhas por partidas dobradas,
verificação de equilíbrio débito/crédito) continua a ser a única fonte de
verdade para códigos de conta. O modelo (seja qual for, ou o fallback por
regras) **nunca escolhe contas**, só classifica o tipo de documento — as
contas e o IVA são sempre calculados de forma determinística.

O código antigo (`rag/retriever.py`, `rag/ingest.py`, `services/llm_local.py`)
foi movido para `legacy/` — não é chamado pelo fluxo actual, mas fica
disponível para comparar os dois caminhos (RAG à mão vs AnythingLLM) na
defesa da tese, se for útil.

## Dois workspaces separados

Ao contrário da versão inicial (um único `my-workspace`), a classificação usa
agora **dois workspaces AnythingLLM com papéis diferentes**:

| Workspace | Papel | Conteúdo embutido |
|---|---|---|
| `regras-do-negocio` (`ANYTHINGLLM_WORKSPACE_EXEMPLOS`) | Decide a classificação (tipo, valor, entidade), por semelhança com documentos de exemplo já classificados. | PDF de casos práticos + ficheiros `.txt` por classe do PGC-AO. |
| `workspace-normativo` (`ANYTHINGLLM_WORKSPACE_NORMAS`) | Só é consultado **depois**, para fundamentar legalmente o tipo já decidido (citação do artigo/secção). Uma falha aqui não invalida uma classificação já bem-sucedida. | Texto legal do Decreto 82/01. |

Fluxo em `document_analyzer.py`: `_classificar_com_anythingllm` consulta
`EXEMPLOS` primeiro (`mode: "query"`, sem histórico) → `_obter_fundamentacao`
consulta `NORMAS` a seguir, só com o tipo já decidido.

## Ficheiros

| Ficheiro | Papel |
|---|---|
| `services/anythingllm_client.py` | Cliente REST: `POST /api/v1/workspace/{workspace}/chat`, recebe o workspace como parâmetro explícito (não há um workspace global fixo). Timeout actual: 300s (ver nota de desempenho abaixo). |
| `services/document_analyzer.py` | `_classificar_com_anythingllm` (workspace exemplos) + `_obter_fundamentacao` (workspace normas); fallback automático para `_classificar_com_regras` em caso de erro/timeout de qualquer um dos dois. |
| `services/pgc.py` | Contas, partidas dobradas, validação de equilíbrio — **e agora também o IVA** (ver secção seguinte). |
| `legacy/` | Caminho antigo (RAG à mão + Ollama directo), preservado só para comparação. |
| `rag/pgc_corpus.jsonl`, `rag/pgc_contas.json` | Dados reais do Decreto 82/01 — continuam a existir independentemente do motor de IA usado. |

## Configuração (`.env`)

```
ANYTHINGLLM_BASE_URL=http://localhost:3001
ANYTHINGLLM_WORKSPACE_NORMAS=workspace-normativo
ANYTHINGLLM_WORKSPACE_EXEMPLOS=regras-do-negocio
ANYTHINGLLM_API_KEY=<a tua chave>
```

Cada workspace precisa, na interface do AnythingLLM, de:
1. Ter documentos **embutidos** (Workspace → Documentos → "Move to Workspace" — o upload por si só não chega).
2. Ter um **modelo de chat configurado** em Settings → Chat Settings (Provider: Ollama + um modelo concreto, ex. `qwen3-vl:2b-instruct-q8_0`). Sem isto, os pedidos ficam pendurados indefinidamente em vez de responder ou falhar — não há erro explícito do lado do AnythingLLM.

## IVA (extensão do projeto, não do Decreto 82/01)

O Decreto 82/01 é de 2001, anterior à entrada em vigor do IVA em Angola
(2019) — por isso **não define nenhuma conta oficial de IVA**. Por indicação
explícita do autor deste TFC, foram criadas duas subcontas sob a conta 34
«Estado» (que no decreto só tinha 34.1 a 34.9 para outros impostos):

- `34.5.1` — IVA dedutível (compras)
- `34.5.2` — IVA liquidado (vendas)
- Taxa aplicada: 14% (taxa geral em Angola), assumindo que o valor extraído
  do documento já inclui IVA.

Isto está implementado em `services/pgc.py` (`IVA_ATIVO`,
`CONTA_IVA_DEDUTIVEL`, `CONTA_IVA_LIQUIDADO`, `TAXA_IVA`) e replicado nas duas
listas espelho (`rag/pgc_contas.json`, `ContaController.java` no backend),
ambas com a mesma nota de proveniência. **Importante para a defesa**: citar
sempre 34.5.1/34.5.2 como decisão de projeto, nunca como texto do decreto.

O IVA é calculado sempre pelo `pgc_ao`, independentemente de a classificação
ter vindo do AnythingLLM ou do fallback por regras — confirmado em teste real
(ver secção de desempenho).

## ⚠️ Desempenho — fallback é esperado no hardware actual

Testes reais mostraram que o modelo local (`qwen3-vl:2b-instruct-q8_0`, CPU,
sem GPU) é lento sobretudo quando o workspace `regras-do-negocio` tem um
documento grande embutido (~115 mil palavras): uma pergunta trivial demorou
~70s; o prompt real de classificação (maior, com mais instruções) excedeu
180s. O timeout foi subido progressivamente (90s → 180s → 300s) para dar
margem a isto e a possíveis "arranques a frio" do Ollama entre pedidos.

Isto **não é um bug** — o sistema foi desenhado para isto: se o AnythingLLM
não responder a tempo, cai automaticamente para `_classificar_com_regras`
(classificação por palavras-chave), e o lançamento continua a ser gerado
correctamente (contas + IVA), só sem a fundamentação legal citada. Para a
demo, vale a pena testar previamente qual dos dois caminhos está a responder
mais depressa nesse momento, e não assumir que o modelo vai responder dentro
de poucos segundos.

## Como correr uma demonstração

1. Confirma que o AnythingLLM está a correr (`http://localhost:3001`), que
   os dois workspaces têm documentos embutidos, e que ambos têm um modelo de
   chat configurado (ver secção de configuração acima).
2. Arranca o FastAPI (`cd fastapi/app && python -m uvicorn main:app --port 8000`),
   o backend Java e o frontend, como habitual.
3. Na interface (`Upload de Documentos`): carrega um documento de teste
   (fatura, recibo, etc.), clica "Analisar". Pode demorar até alguns minutos
   se o AnythingLLM estiver a responder devagar (ver aviso de desempenho).
4. Se o AnythingLLM responder a tempo, o resultado mostra
   `modelo: "anythingllm:regras-do-negocio+workspace-normativo"` e
   `fundamentacao` com a citação do artigo/secção do decreto. Se cair no
   fallback, mostra `modelo: "regras"` e `fundamentacao` vazia — em ambos os
   casos as contas e o IVA saem corretos.
5. Aprova a sugestão para gerar o `Lancamento` oficial, como no fluxo normal.
