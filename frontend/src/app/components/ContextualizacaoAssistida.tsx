import { HelpCircle } from 'lucide-react';
import type { OpcaoContextualizacao, PerguntaContextualizacao } from '../types/documento';

// Fase 4 do plano de 20 fases — "contextualização assistida": quando o
// backend não conseguiu decidir sozinho o tipo de operação, mostra UMA
// pergunta fechada com opções fixas (nunca um chatbot genérico). Cada
// opção já traz o lançamento pré-calculado — escolher uma aplica-o
// imediatamente, sem novo pedido ao servidor.
export function parsePerguntaContextualizacao(json?: string | null): PerguntaContextualizacao | null {
  if (!json) return null;
  try {
    return JSON.parse(json) as PerguntaContextualizacao;
  } catch (err) {
    console.error('Não foi possível interpretar a pergunta de contextualização:', err);
    return null;
  }
}

export function ContextualizacaoAssistida({
  perguntaContextualizacaoJson,
  onEscolher,
}: {
  perguntaContextualizacaoJson?: string | null;
  onEscolher: (opcao: OpcaoContextualizacao) => void;
}) {
  const pergunta = parsePerguntaContextualizacao(perguntaContextualizacaoJson);
  if (!pergunta) return null;

  return (
    <div className="flex flex-col gap-2 bg-[#FFFBEB] border border-[#FDE68A] rounded-lg px-4 py-3">
      <div className="flex items-center gap-2">
        <HelpCircle style={{ width: 14, height: 14 }} className="text-[#D97706] flex-shrink-0" />
        <p className="text-[13px] font-medium text-[#92400E]">{pergunta.pergunta}</p>
      </div>
      <div className="flex flex-wrap gap-2">
        {pergunta.opcoes.map(opcao => (
          <button
            key={opcao.valor}
            onClick={() => onEscolher(opcao)}
            className="h-8 px-3 text-[13px] font-medium rounded-md border border-[#FDE68A] bg-white text-[#92400E] hover:bg-[#FEF3C7] transition-colors"
          >
            {opcao.rotulo}
          </button>
        ))}
      </div>
    </div>
  );
}
