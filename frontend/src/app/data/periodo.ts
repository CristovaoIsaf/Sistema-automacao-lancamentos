// Converte o seletor de período ('mes-atual' | 'trimestre' | 'ano') usado
// em Balancetes.tsx e Relatorios.tsx num intervalo [inicio, fim] de datas
// ISO — única fonte, para as duas páginas nunca calcularem o período de
// forma diferente uma da outra.
export function intervaloDoPeriodo(periodo: string): { inicio?: string; fim?: string } {
  const hoje = new Date();
  const fim = hoje.toISOString().slice(0, 10);

  switch (periodo) {
    case 'mes-atual': {
      const inicio = new Date(hoje.getFullYear(), hoje.getMonth(), 1).toISOString().slice(0, 10);
      return { inicio, fim };
    }
    case 'trimestre': {
      const inicio = new Date(hoje.getFullYear(), hoje.getMonth() - 2, 1).toISOString().slice(0, 10);
      return { inicio, fim };
    }
    case 'ano': {
      const inicio = new Date(hoje.getFullYear(), 0, 1).toISOString().slice(0, 10);
      return { inicio, fim };
    }
    default:
      return {};
  }
}
