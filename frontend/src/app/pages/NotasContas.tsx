import { useEffect, useState } from 'react';
import { Sparkles, FileSignature, Loader2, AlertCircle, FileText } from 'lucide-react';
import { formatarKwanza } from '../data/mockData';
import { intervaloDoPeriodo } from '../data/periodo';
import { listarContas } from '../api/contaApi';
import { obterNotaConta, obterRedacaoNota } from '../api/notaContaApi';
import { abrirDocumento } from '../api/documentoApi';
import type { ContaResumo } from '../types/categoriaConta';
import type { NotaConta } from '../types/notaConta';
import { toast } from 'sonner';

// Fase 14 do plano de 20 fases — "Notas às Contas": composição do saldo
// de uma conta por entidade de origem (já implementado no backend desde
// T3, ver NotaContaService — mas nunca tinha nenhuma página a consumi-lo).
// A "redação assistida por IA" é sempre um RASCUNHO editável — nunca é
// gravada automaticamente; o contabilista revê e ajusta antes de a usar
// (ver texto no botão "Gerar rascunho").
export function NotasContas() {
  const [contas, setContas] = useState<ContaResumo[]>([]);
  const [contaSelecionada, setContaSelecionada] = useState('');
  const [periodo, setPeriodo] = useState('ano');

  const [nota, setNota] = useState<NotaConta | null>(null);
  const [carregandoNota, setCarregandoNota] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const [rascunho, setRascunho] = useState('');
  const [fonteRascunho, setFonteRascunho] = useState<'ia' | 'template' | null>(null);
  const [aGerarRascunho, setAGerarRascunho] = useState(false);

  useEffect(() => {
    listarContas()
      .then(dados => {
        setContas(dados);
        if (dados.length > 0) setContaSelecionada(dados[0].codigo);
      })
      .catch(err => console.error('Erro ao carregar plano de contas:', err));
  }, []);

  useEffect(() => {
    if (!contaSelecionada) return;
    let cancelado = false;
    setCarregandoNota(true);
    setErro(null);
    setRascunho('');
    setFonteRascunho(null);

    const { inicio, fim } = intervaloDoPeriodo(periodo);

    obterNotaConta(contaSelecionada, inicio, fim)
      .then(dados => { if (!cancelado) setNota(dados); })
      .catch(err => {
        if (cancelado) return;
        console.error('Erro ao carregar nota:', err);
        setErro('Não foi possível carregar a nota desta conta.');
      })
      .finally(() => { if (!cancelado) setCarregandoNota(false); });

    return () => { cancelado = true; };
  }, [contaSelecionada, periodo]);

  const gerarRascunho = async () => {
    if (!contaSelecionada) return;
    setAGerarRascunho(true);
    try {
      const { inicio, fim } = intervaloDoPeriodo(periodo);
      const redacao = await obterRedacaoNota(contaSelecionada, inicio, fim);
      if (!redacao) {
        toast.error('Não foi possível gerar um rascunho (serviço de IA indisponível)');
        return;
      }
      setRascunho(redacao.texto);
      setFonteRascunho(redacao.fonte);
    } catch (err) {
      toast.error('Não foi possível gerar o rascunho');
    } finally {
      setAGerarRascunho(false);
    }
  };

  return (
    <div className="max-w-[1000px] space-y-4">
      <div>
        <h1 className="text-[18px] font-semibold text-[#0F172A]">Notas às Contas</h1>
        <p className="text-[13px] text-[#475569] mt-0.5">PGCA · Decreto n.º 82/01</p>
      </div>

      <div className="flex items-center gap-3 flex-wrap">
        <select
          value={contaSelecionada}
          onChange={e => setContaSelecionada(e.target.value)}
          className="h-8 px-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] focus:outline-none focus:border-[#2563EB] transition-all"
          style={{ fontFamily: 'JetBrains Mono, monospace' }}
        >
          {contas.map(c => <option key={c.codigo} value={c.codigo}>{c.codigo} — {c.nome}</option>)}
        </select>
        <div className="flex items-center gap-1">
          {['mes-atual', 'trimestre', 'ano'].map(p => (
            <button
              key={p}
              onClick={() => setPeriodo(p)}
              className={`h-7 px-3 text-[12px] font-medium rounded-md transition-colors ${periodo === p ? 'bg-[#2563EB] text-white' : 'bg-white border border-[#E2E8F0] text-[#475569] hover:bg-[#F8FAFC]'}`}
            >
              {p === 'mes-atual' ? 'Mês actual' : p === 'trimestre' ? 'Trimestre' : 'Ano'}
            </button>
          ))}
        </div>
      </div>

      {carregandoNota ? (
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-8 flex items-center justify-center gap-2 text-[13px] text-[#94A3B8]">
          <Loader2 className="animate-spin" size={16} /> A carregar...
        </div>
      ) : erro ? (
        <div className="bg-white border border-[#E2E8F0] rounded-lg p-8 flex items-center justify-center gap-2 text-[13px] text-[#DC2626]">
          <AlertCircle size={16} /> {erro}
        </div>
      ) : nota ? (
        <div className="space-y-4">
          <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
            <div className="px-4 py-3 border-b border-[#E2E8F0]">
              <h2 className="text-[13px] font-semibold text-[#0F172A]">
                {nota.conta} — {nota.nomeConta ?? 'Conta não encontrada no plano de contas'}
              </h2>
            </div>
            <div className="p-4 grid grid-cols-3 gap-3">
              <div>
                <p className="text-[11px] font-medium text-[#94A3B8] uppercase tracking-wide mb-0.5">Total Débito</p>
                <p className="text-[15px] font-semibold text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(nota.totalDebito)}</p>
              </div>
              <div>
                <p className="text-[11px] font-medium text-[#94A3B8] uppercase tracking-wide mb-0.5">Total Crédito</p>
                <p className="text-[15px] font-semibold text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(nota.totalCredito)}</p>
              </div>
              <div>
                <p className="text-[11px] font-medium text-[#94A3B8] uppercase tracking-wide mb-0.5">Saldo</p>
                <p className="text-[15px] font-semibold text-[#2563EB]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{formatarKwanza(Math.abs(nota.saldo))}</p>
              </div>
            </div>
          </div>

          <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
            <div className="px-4 py-3 border-b border-[#E2E8F0]">
              <h2 className="text-[13px] font-semibold text-[#0F172A]">Composição por entidade</h2>
            </div>
            {nota.porEntidade.length === 0 ? (
              <p className="px-4 py-8 text-center text-[13px] text-[#94A3B8]">Sem movimentos no período seleccionado.</p>
            ) : (
              <div className="divide-y divide-[#F1F5F9]">
                {nota.porEntidade.map(grupo => (
                  <div key={grupo.entidade} className="px-4 py-3">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-[13px] font-medium text-[#0F172A]">{grupo.entidade}</span>
                      <div className="flex items-center gap-3 text-[12px]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                        <span className="text-[#059669]">{formatarKwanza(grupo.subtotalDebito)}</span>
                        <span className="text-[#7C3AED]">{formatarKwanza(grupo.subtotalCredito)}</span>
                      </div>
                    </div>
                    <div className="space-y-1">
                      {grupo.movimentos.map((mov, idx) => (
                        <div key={`${mov.lancamentoId}-${idx}`} className="flex items-center justify-between text-[12px] text-[#64748B]">
                          <span className="truncate flex-1">{new Date(mov.data).toLocaleDateString('pt-AO')} — {mov.descricao}</span>
                          {mov.documentoId != null && (
                            <button
                              onClick={() => abrirDocumento(mov.documentoId!).catch(() => toast.error('Não foi possível abrir o documento'))}
                              className="flex items-center gap-1 text-[#2563EB] hover:text-[#1D4ED8] shrink-0 ml-2"
                            >
                              <FileText size={11} /> {mov.documentoNome}
                            </button>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
            <div className="px-4 py-3 border-b border-[#E2E8F0] flex items-center justify-between">
              <h2 className="text-[13px] font-semibold text-[#0F172A]">Rascunho da nota</h2>
              <button
                onClick={gerarRascunho}
                disabled={aGerarRascunho}
                className="flex items-center gap-1.5 h-7 px-2.5 bg-[#F5F3FF] hover:bg-[#EDE9FE] text-[#7C3AED] text-[12px] font-medium rounded-md transition-colors disabled:opacity-50"
              >
                {aGerarRascunho ? <Loader2 className="animate-spin" size={12} /> : <Sparkles size={12} />}
                Gerar rascunho
              </button>
            </div>
            <div className="p-4 space-y-2">
              {fonteRascunho && (
                <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium ${
                  fonteRascunho === 'ia' ? 'bg-[#F5F3FF] text-[#7C3AED]' : 'bg-[#F1F5F9] text-[#475569]'
                }`}>
                  <FileSignature size={11} /> {fonteRascunho === 'ia' ? 'Rascunho gerado por IA — rever antes de usar' : 'Rascunho gerado automaticamente'}
                </span>
              )}
              <textarea
                value={rascunho}
                onChange={e => setRascunho(e.target.value)}
                placeholder="Clica em “Gerar rascunho” para obter um texto explicativo de base, ou escreve directamente aqui."
                rows={5}
                className="w-full px-3 py-2 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all resize-y"
              />
              <p className="text-[11px] text-[#94A3B8]">
                Este texto não é gravado automaticamente — revê e copia para o relatório final quando estiveres satisfeito.
              </p>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
