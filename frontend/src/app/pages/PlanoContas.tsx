import { useState } from 'react';
import { ChevronRight, ChevronDown, Plus, Search, ToggleLeft, ToggleRight, Edit2 } from 'lucide-react';
import { planoContasAngolano } from '../data/mockData';
import { Conta } from '../types/contabilidade';

function NaturezaBadge({ natureza }: { natureza: string }) {
  return (
    <span className={`inline-flex px-2 py-0.5 rounded-full text-[11px] font-medium ${
      natureza === 'DEVEDORA' ? 'bg-[#EFF6FF] text-[#2563EB]' : 'bg-[#F5F3FF] text-[#7C3AED]'
    }`}>
      {natureza}
    </span>
  );
}

function TipoBadge({ tipo }: { tipo: string }) {
  const styles: Record<string, string> = {
    ATIVO:              'bg-[#ECFDF5] text-[#059669]',
    PASSIVO:            'bg-[#FEF2F2] text-[#DC2626]',
    RECEITA:            'bg-[#EFF6FF] text-[#2563EB]',
    DESPESA:            'bg-[#FFFBEB] text-[#D97706]',
    PATRIMONIO_LIQUIDO: 'bg-[#F5F3FF] text-[#7C3AED]',
  };
  const labels: Record<string, string> = {
    ATIVO: 'Ativo', PASSIVO: 'Passivo', RECEITA: 'Receita', DESPESA: 'Despesa', PATRIMONIO_LIQUIDO: 'Patr. Líq.',
  };
  return (
    <span className={`inline-flex px-2 py-0.5 rounded-full text-[11px] font-medium ${styles[tipo] ?? 'bg-[#F1F5F9] text-[#64748B]'}`}>
      {labels[tipo] ?? tipo}
    </span>
  );
}

export function PlanoContas() {
  const classes = planoContasAngolano.filter(c => c.nivel === 1);
  const [selectedClass, setSelectedClass] = useState<string>('1');
  const [expanded, setExpanded] = useState<Set<string>>(new Set(['1', '1.1', '2', '3', '4', '5']));
  const [search, setSearch] = useState('');

  const toggle = (id: string) => {
    setExpanded(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const getChildren = (parentId: string) =>
    planoContasAngolano.filter(c => c.contaPai === parentId);

  // Flat list of all accounts in selected class
  const getAll = (rootId: string): Conta[] => {
    const result: Conta[] = [];
    const root = planoContasAngolano.find(c => c.id === rootId);
    if (!root) return result;
    const recurse = (id: string) => {
      planoContasAngolano.filter(c => c.contaPai === id).forEach(c => {
        result.push(c);
        recurse(c.id);
      });
    };
    recurse(rootId);
    return result;
  };

  const allInClass = getAll(selectedClass);
  const filtered = search
    ? planoContasAngolano.filter(c => c.nivel > 1 && (
        c.codigo.includes(search) ||
        c.nome.toLowerCase().includes(search.toLowerCase())
      ))
    : allInClass;

  const selectedClassData = classes.find(c => c.id === selectedClass);

  return (
    <div className="max-w-[1200px] space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-1.5 text-[12px] text-[#475569] mb-1">
            <span>Administrador</span>
            <span className="text-[#CBD5E1]">&gt;</span>
            <span>Plano de Contas</span>
            {selectedClassData && (
              <>
                <span className="text-[#CBD5E1]">&gt;</span>
                <span className="text-[#0F172A] font-medium">Classe {selectedClass} — {selectedClassData.nome}</span>
              </>
            )}
          </div>
          <h1 className="text-[18px] font-semibold text-[#0F172A]">Plano de Contas</h1>
          <p className="text-[13px] text-[#475569] mt-0.5">PGCA · Decreto n.º 82/01</p>
        </div>
        <button className="flex items-center gap-1.5 h-8 px-3 bg-[#2563EB] hover:bg-[#1D4ED8] text-white text-[13px] font-medium rounded-md transition-colors">
          <Plus style={{ width: 13, height: 13 }} /> Nova Conta
        </button>
      </div>

      <div className="flex gap-4">

        {/* ── Árvore de classes ── */}
        <div className="w-52 flex-shrink-0 bg-white border border-[#E2E8F0] rounded-lg overflow-hidden self-start">
          <div className="px-3 py-2.5 border-b border-[#E2E8F0]">
            <p className="text-[11px] font-medium text-[#475569] uppercase tracking-wide">Classes PGCA</p>
          </div>
          <div className="py-1">
            {classes.map(cls => (
              <button
                key={cls.id}
                onClick={() => { setSelectedClass(cls.id); setSearch(''); }}
                className={`w-full flex items-center gap-2 px-3 py-2 text-left transition-colors ${
                  selectedClass === cls.id ? 'bg-[#EFF6FF] text-[#2563EB]' : 'text-[#475569] hover:bg-[#F8FAFC]'
                }`}
              >
                <span className="text-[12px] font-medium" style={{ fontFamily: 'JetBrains Mono, monospace' }}>{cls.codigo}</span>
                <span className="text-[12px] truncate">{cls.nome}</span>
              </button>
            ))}
          </div>
        </div>

        {/* ── Tabela de contas ── */}
        <div className="flex-1 min-w-0 space-y-3">
          {/* Search */}
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
                  {['Código', 'Designação', 'Natureza', 'Tipo', 'Estado', 'Acções'].map(h => (
                    <th key={h} className={`px-4 py-2.5 text-[11px] font-medium text-[#475569] uppercase tracking-wide text-left`}>
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filtered.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-[13px] text-[#94A3B8]">
                      Nenhuma conta encontrada
                    </td>
                  </tr>
                ) : filtered.map(conta => (
                  <tr key={conta.id} className="border-b border-[#F1F5F9] hover:bg-[#F8FAFC] transition-colors">
                    <td className="px-4 py-2.5 text-[13px] font-medium text-[#0F172A]" style={{ fontFamily: 'JetBrains Mono, monospace', paddingLeft: conta.nivel > 2 ? `${(conta.nivel - 1) * 16 + 16}px` : undefined }}>
                      {conta.codigo}
                    </td>
                    <td className="px-4 py-2.5 text-[13px] text-[#0F172A]">{conta.nome}</td>
                    <td className="px-4 py-2.5"><NaturezaBadge natureza={conta.natureza} /></td>
                    <td className="px-4 py-2.5"><TipoBadge tipo={conta.tipo} /></td>
                    <td className="px-4 py-2.5">
                      <button className={`flex items-center gap-1 text-[11px] font-medium transition-colors ${conta.ativa ? 'text-[#059669]' : 'text-[#94A3B8]'}`}>
                        {conta.ativa
                          ? <ToggleRight style={{ width: 16, height: 16 }} />
                          : <ToggleLeft style={{ width: 16, height: 16 }} />}
                        {conta.ativa ? 'Ativa' : 'Inativa'}
                      </button>
                    </td>
                    <td className="px-4 py-2.5">
                      <button className="w-6 h-6 flex items-center justify-center text-[#64748B] hover:text-[#0F172A] hover:bg-[#F1F5F9] rounded transition-colors">
                        <Edit2 style={{ width: 13, height: 13 }} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="px-4 py-2.5 border-t border-[#E2E8F0]">
              <p className="text-[12px] text-[#475569]">{filtered.length} contas</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
