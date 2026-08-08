import { AlertTriangle, AlertCircle } from 'lucide-react';
import type { ResultadoValidacao } from '../types/documento';

// Sugestao.validacaoJson é o ResultadoValidacao do motor de validação
// determinística (Fase 3 — ver fastapi/app/services/document_validation.py)
// serializado em JSON pelo backend. Fonte única de parsing — usada tanto em
// UploadDocumentos.tsx (logo após a análise) como em LancamentoDiario.tsx
// (ecrã de revisão), para não haver duas interpretações diferentes do
// mesmo JSON.
export function parseValidacao(validacaoJson?: string | null): ResultadoValidacao | null {
  if (!validacaoJson) return null;
  try {
    return JSON.parse(validacaoJson) as ResultadoValidacao;
  } catch (err) {
    console.error('Não foi possível interpretar o resultado da validação:', err);
    return null;
  }
}

/** Lista de problemas encontrados pelo motor de validação determinística
 * — erros a vermelho (bloqueiam a confiança no documento), avisos a
 * amarelo (informativos, não impedem aprovar a sugestão). */
export function ValidacaoDocumento({ validacaoJson }: { validacaoJson?: string | null }) {
  const resultado = parseValidacao(validacaoJson);
  if (!resultado || resultado.problemas.length === 0) return null;

  const erros = resultado.problemas.filter(p => p.gravidade === 'erro');
  const avisos = resultado.problemas.filter(p => p.gravidade === 'aviso');

  return (
    <div className="space-y-1.5">
      {erros.map((problema, i) => (
        <div
          key={`erro-${i}`}
          className="flex items-start gap-1.5 bg-[#FEF2F2] text-[#DC2626] rounded-md px-2.5 py-1.5 text-[12px]"
        >
          <AlertCircle style={{ width: 13, height: 13 }} className="flex-shrink-0 mt-0.5" />
          <span>{problema.mensagem}</span>
        </div>
      ))}
      {avisos.map((problema, i) => (
        <div
          key={`aviso-${i}`}
          className="flex items-start gap-1.5 bg-[#FFFBEB] text-[#D97706] rounded-md px-2.5 py-1.5 text-[12px]"
        >
          <AlertTriangle style={{ width: 13, height: 13 }} className="flex-shrink-0 mt-0.5" />
          <span>{problema.mensagem}</span>
        </div>
      ))}
    </div>
  );
}
