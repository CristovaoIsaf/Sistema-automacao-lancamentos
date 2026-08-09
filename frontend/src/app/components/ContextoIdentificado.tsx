import { Compass } from 'lucide-react';
import type { ContextoClassificacao } from '../types/documento';

// Sugestao.contextoJson é o ContextoClassificacaoDTO (Fase 3 — Context
// Engine) serializado em JSON pelo backend. Fonte única de parsing — mesmo
// padrão já usado em ValidacaoDocumento.tsx/ContextualizacaoAssistida.tsx.
export function parseContexto(contextoJson?: string | null): ContextoClassificacao | null {
  if (!contextoJson) return null;
  try {
    return JSON.parse(contextoJson) as ContextoClassificacao;
  } catch (err) {
    console.error('Não foi possível interpretar o contexto da classificação:', err);
    return null;
  }
}

// Fase 8 do plano de 20 fases — "mostrar imediatamente ... contexto
// identificado": até esta fase, o Context Engine (Fase 3) já calculava e
// guardava este contexto em Sugestao.contextoJson, mas nunca era mostrado
// ao contabilista — ficava só disponível para rastreabilidade em auditoria.
export function ContextoIdentificado({ contextoJson }: { contextoJson?: string | null }) {
  const contexto = parseContexto(contextoJson);
  if (!contexto) return null;

  const partes: string[] = [];
  if (contexto.empresaAtividadeEconomica) partes.push(contexto.empresaAtividadeEconomica);
  if (contexto.empresaNaturezaNegocio) partes.push(contexto.empresaNaturezaNegocio);

  const temHistorico = contexto.historicoTiposOperacaoRecentes?.length > 0;

  if (partes.length === 0 && !temHistorico) return null;

  return (
    <div className="flex items-start gap-1.5 bg-[#F0F9FF] text-[#0369A1] rounded-md px-2.5 py-1.5 text-[12px]">
      <Compass style={{ width: 13, height: 13 }} className="flex-shrink-0 mt-0.5" />
      <span>
        {partes.length > 0 && <>Empresa: {partes.join(' · ')}.</>}
        {temHistorico && (
          <> Operações recentes desta entidade: {contexto.historicoTiposOperacaoRecentes.join(', ')}.</>
        )}
      </span>
    </div>
  );
}
