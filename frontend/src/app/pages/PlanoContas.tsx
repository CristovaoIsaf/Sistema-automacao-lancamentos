import { Fragment, useEffect, useMemo, useState } from 'react';
import { Search, Loader2 } from 'lucide-react';
import { listarContas } from '../api/contaApi';
import { listarCategoriasConta } from '../api/categoriaContaApi';
import type { CategoriaConta, ContaResumo } from '../types/categoriaConta';

// Plano de contas real do PGC-AO (Decreto n.º 82/01) — as mesmas 18 contas
// usadas pelos lançamentos manual e automático (ver ContaController.CONTAS
// no backend). Antes esta página mostrava um plano de contas genérico de
// exemplo (mockData.planoContasAngolano, numeração 1=Ativo/2=Passivo/...),
// diferente da classificação real do Decreto 82/01 que os lançamentos
// realmente usam — por isso foi substituída por dados reais da API.
export function PlanoContas() {
  const [contas, setContas] = useState<ContaResumo[]>([]);
  const [categorias, setCategorias] = useState<CategoriaConta[]>([]);
  const [categoriaSelecionada, setCategoriaSelecionada] = useState('');
  const [search, setSearch] = useState('');
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    Promise.all([listarContas(), listarCategoriasConta()])
      .then(([dadosContas, dadosCategorias]) => {
        setContas(dadosContas);
        setCategorias(dadosCategorias);
      })
      .catch(err => console.error('Erro ao carregar plano de contas:', err))
      .finally(() => setCarregando(false));
  }, []);

  const categoriaAtual = categorias.find(c => c.nome === categoriaSelecionada);

  // Sem pesquisa: mostra a categoria escolhida (Principais/Ocasionais) ou
  // todas as contas. Com pesquisa: procura sempre no plano de contas
  // completo, independentemente da categoria seleccionada.
  const grupos = useMemo(() => {
    if (search) {
      const termo = search.toLowerCase();
      const encontradas = contas.filter(c =>
        c.codigo.toLowerCase().includes(termo) || c.nome.toLowerCase().includes(termo));
      return [{ titulo: '', contas: encontradas }];
    }
    if (!categoriaAtual) {
      return [{ titulo: '', contas }];
    }
    return [
      { titulo: 'Principais', contas: categoriaAtual.principais },
      { titulo: 'Ocasionais', contas: categoriaAtual.ocasionais },
    ].filter(g => g.contas.length > 0);
  }, [search, categoriaAtual, contas]);

  const totalContas = grupos.reduce((soma, g) => soma + g.contas.length, 0);

  return (
    <div className="max-w-[1200px] space-y-4">
      {/* Header */}
      <div>
        <h1 className="text-[18px] font-semibold text-[#0F172A]">Plano de Contas</h1>
        <p className="text-[13px] text-[#475569] mt-0.5">PGCA · Decreto n.º 82/01</p>
      </div>

      <div className="flex gap-4">
        {/* ── Categorias ── */}
        <div className="w-52 flex-shrink-0 bg-white border border-[#E2E8F0] rounded-lg overflow-hidden self-start">
          <div className="px-3 py-2.5 border-b border-[#E2E8F0]">
            <p className="text-[11px] font-medium text-[#475569] uppercase tracking-wide">Categorias</p>
          </div>
          <div className="py-1">
            <button
              onClick={() => { setCategoriaSelecionada(''); setSearch(''); }}
              className={`w-full text-left px-3 py-2 text-[12px] transition-colors ${
                categoriaSelecionada === '' ? 'bg-[#EFF6FF] text-[#2563EB] font-medium' : 'text-[#475569] hover:bg-[#F8FAFC]'
              }`}
            >
              Todas as contas
            </button>
            {categorias.map(cat => (
              <button
                key={cat.nome}
                onClick={() => { setCategoriaSelecionada(cat.nome); setSearch(''); }}
                className={`w-full text-left px-3 py-2 text-[12px] transition-colors ${
                  categoriaSelecionada === cat.nome ? 'bg-[#EFF6FF] text-[#2563EB] font-medium' : 'text-[#475569] hover:bg-[#F8FAFC]'
                }`}
              >
                {cat.nome}
              </button>
            ))}
          </div>
        </div>

        {/* ── Tabela de contas ── */}
        <div className="flex-1 min-w-0 space-y-3">
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 text-[#94A3B8]" style={{ width: 13, height: 13 }} />
            <input
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Pesquisar por código ou designação..."
              className="w-full h-8 pl-8 pr-3 text-[13px] border border-[#E2E8F0] rounded-md bg-white text-[#0F172A] placeholder-[#94A3B8] focus:outline-none focus:border-[#2563EB] focus:ring-[3px] focus:ring-[#EFF6FF] transition-all"
            />
          </div>

          <div className="bg-white border border-[#E2E8F0] rounded-lg overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="bg-[#F8FAFC] border-b border-[#E2E8F0]">
                  {['Código', 'Designação'].map(h => (
                    <th key={h} className="px-4 py-2.5 text-[11px] font-medium text-[#475569] uppercase tracking-wide text-left">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {carregando ? (
                  <tr>
                    <td colSpan={2} className="px-4 py-8 text-center text-[13px] text-[#94A3B8]">
                      <Loader2 className="animate-spin inline-block mr-2" size={14} /> A carregar...
                    </td>
                  </tr>
                ) : totalContas === 0 ? (
                  <tr>
                    <td colSpan={2} className="px-4 py-8 text-center text-[13px] text-[#94A3B8]">
                      Nenhuma conta encontrada
                    </td>
                  </tr>
                ) : grupos.map(grupo => (
                  <Fragment key={grupo.titulo || 'flat'}>
                    {grupo.titulo && (
                      <tr className="bg-[#F8FAFC]">
                        <td colSpan={2} className="px-4 py-1.5 text-[11px] font-medium text-[#94A3B8] uppercase tracking-wide">
                          {grupo.titulo}
                        </td>
                      </tr>
                    )}
                    {grupo.contas.map(conta => (
                      <tr key={`${grupo.titulo}-${conta.codigo}`} className="border-b border-[#F1F5F9] hover:bg-[#F8FAFC] transition-colors">
                        <td className="px-4 py-2.5 text-[13px] font-medium text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace' }}>
                          {conta.codigo}
                        </td>
                        <td className="px-4 py-2.5 text-[13px] text-[#0F172A]">{conta.nome}</td>
                      </tr>
                    ))}
                  </Fragment>
                ))}
              </tbody>
            </table>
            <div className="px-4 py-2.5 border-t border-[#E2E8F0]">
              <p className="text-[12px] text-[#475569]">{totalContas} contas</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
