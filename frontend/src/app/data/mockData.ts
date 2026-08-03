import { Conta, Lancamento, ResumoFinanceiro, CategoriaIA } from '../types/contabilidade';

// Plano de Contas adaptado ao contexto angolano
export const planoContasAngolano: Conta[] = [
  // ATIVO
  { id: '1', codigo: '1', nome: 'ATIVO', tipo: 'ATIVO', natureza: 'DEVEDORA', nivel: 1, ativa: true },
  { id: '1.1', codigo: '1.1', nome: 'ATIVO CIRCULANTE', tipo: 'ATIVO', natureza: 'DEVEDORA', nivel: 2, contaPai: '1', ativa: true },
  { id: '1.1.1', codigo: '1.1.1', nome: 'Caixa', tipo: 'ATIVO', natureza: 'DEVEDORA', nivel: 3, contaPai: '1.1', ativa: true },
  { id: '1.1.2', codigo: '1.1.2', nome: 'Bancos Conta Movimento', tipo: 'ATIVO', natureza: 'DEVEDORA', nivel: 3, contaPai: '1.1', ativa: true },
  { id: '1.1.3', codigo: '1.1.3', nome: 'Clientes', tipo: 'ATIVO', natureza: 'DEVEDORA', nivel: 3, contaPai: '1.1', ativa: true },
  { id: '1.1.4', codigo: '1.1.4', nome: 'Stocks de Mercadorias', tipo: 'ATIVO', natureza: 'DEVEDORA', nivel: 3, contaPai: '1.1', ativa: true },
  { id: '1.1.5', codigo: '1.1.5', nome: 'IVA a Recuperar', tipo: 'ATIVO', natureza: 'DEVEDORA', nivel: 3, contaPai: '1.1', ativa: true },
  
  { id: '1.2', codigo: '1.2', nome: 'ATIVO NÃO CIRCULANTE', tipo: 'ATIVO', natureza: 'DEVEDORA', nivel: 2, contaPai: '1', ativa: true },
  { id: '1.2.1', codigo: '1.2.1', nome: 'Imobilizado Corpóreo', tipo: 'ATIVO', natureza: 'DEVEDORA', nivel: 3, contaPai: '1.2', ativa: true },
  { id: '1.2.2', codigo: '1.2.2', nome: 'Veículos', tipo: 'ATIVO', natureza: 'DEVEDORA', nivel: 3, contaPai: '1.2', ativa: true },
  { id: '1.2.3', codigo: '1.2.3', nome: 'Equipamentos Informáticos', tipo: 'ATIVO', natureza: 'DEVEDORA', nivel: 3, contaPai: '1.2', ativa: true },

  // PASSIVO
  { id: '2', codigo: '2', nome: 'PASSIVO', tipo: 'PASSIVO', natureza: 'CREDORA', nivel: 1, ativa: true },
  { id: '2.1', codigo: '2.1', nome: 'PASSIVO CIRCULANTE', tipo: 'PASSIVO', natureza: 'CREDORA', nivel: 2, contaPai: '2', ativa: true },
  { id: '2.1.1', codigo: '2.1.1', nome: 'Fornecedores', tipo: 'PASSIVO', natureza: 'CREDORA', nivel: 3, contaPai: '2.1', ativa: true },
  { id: '2.1.2', codigo: '2.1.2', nome: 'Salários a Pagar', tipo: 'PASSIVO', natureza: 'CREDORA', nivel: 3, contaPai: '2.1', ativa: true },
  { id: '2.1.3', codigo: '2.1.3', nome: 'IVA a Pagar', tipo: 'PASSIVO', natureza: 'CREDORA', nivel: 3, contaPai: '2.1', ativa: true },
  { id: '2.1.4', codigo: '2.1.4', nome: 'INSS a Recolher', tipo: 'PASSIVO', natureza: 'CREDORA', nivel: 3, contaPai: '2.1', ativa: true },
  { id: '2.1.5', codigo: '2.1.5', nome: 'Imposto Industrial a Pagar', tipo: 'PASSIVO', natureza: 'CREDORA', nivel: 3, contaPai: '2.1', ativa: true },
  
  { id: '2.2', codigo: '2.2', nome: 'PASSIVO NÃO CIRCULANTE', tipo: 'PASSIVO', natureza: 'CREDORA', nivel: 2, contaPai: '2', ativa: true },
  { id: '2.2.1', codigo: '2.2.1', nome: 'Empréstimos Bancários', tipo: 'PASSIVO', natureza: 'CREDORA', nivel: 3, contaPai: '2.2', ativa: true },

  // PATRIMÔNIO LÍQUIDO
  { id: '3', codigo: '3', nome: 'PATRIMÔNIO LÍQUIDO', tipo: 'PATRIMONIO_LIQUIDO', natureza: 'CREDORA', nivel: 1, ativa: true },
  { id: '3.1', codigo: '3.1', nome: 'Capital Social', tipo: 'PATRIMONIO_LIQUIDO', natureza: 'CREDORA', nivel: 2, contaPai: '3', ativa: true },
  { id: '3.2', codigo: '3.2', nome: 'Reservas', tipo: 'PATRIMONIO_LIQUIDO', natureza: 'CREDORA', nivel: 2, contaPai: '3', ativa: true },
  { id: '3.3', codigo: '3.3', nome: 'Resultados Transitados', tipo: 'PATRIMONIO_LIQUIDO', natureza: 'CREDORA', nivel: 2, contaPai: '3', ativa: true },

  // RECEITAS
  { id: '4', codigo: '4', nome: 'RECEITAS', tipo: 'RECEITA', natureza: 'CREDORA', nivel: 1, ativa: true },
  { id: '4.1', codigo: '4.1', nome: 'RECEITAS OPERACIONAIS', tipo: 'RECEITA', natureza: 'CREDORA', nivel: 2, contaPai: '4', ativa: true },
  { id: '4.1.1', codigo: '4.1.1', nome: 'Vendas de Mercadorias', tipo: 'RECEITA', natureza: 'CREDORA', nivel: 3, contaPai: '4.1', ativa: true },
  { id: '4.1.2', codigo: '4.1.2', nome: 'Prestação de Serviços', tipo: 'RECEITA', natureza: 'CREDORA', nivel: 3, contaPai: '4.1', ativa: true },
  { id: '4.2', codigo: '4.2', nome: 'OUTRAS RECEITAS', tipo: 'RECEITA', natureza: 'CREDORA', nivel: 2, contaPai: '4', ativa: true },
  { id: '4.2.1', codigo: '4.2.1', nome: 'Receitas Financeiras', tipo: 'RECEITA', natureza: 'CREDORA', nivel: 3, contaPai: '4.2', ativa: true },

  // DESPESAS
  { id: '5', codigo: '5', nome: 'DESPESAS', tipo: 'DESPESA', natureza: 'DEVEDORA', nivel: 1, ativa: true },
  { id: '5.1', codigo: '5.1', nome: 'DESPESAS OPERACIONAIS', tipo: 'DESPESA', natureza: 'DEVEDORA', nivel: 2, contaPai: '5', ativa: true },
  { id: '5.1.1', codigo: '5.1.1', nome: 'Custo das Mercadorias Vendidas', tipo: 'DESPESA', natureza: 'DEVEDORA', nivel: 3, contaPai: '5.1', ativa: true },
  { id: '5.1.2', codigo: '5.1.2', nome: 'Salários e Ordenados', tipo: 'DESPESA', natureza: 'DEVEDORA', nivel: 3, contaPai: '5.1', ativa: true },
  { id: '5.1.3', codigo: '5.1.3', nome: 'Encargos Sociais', tipo: 'DESPESA', natureza: 'DEVEDORA', nivel: 3, contaPai: '5.1', ativa: true },
  { id: '5.1.4', codigo: '5.1.4', nome: 'Energia e Água', tipo: 'DESPESA', natureza: 'DEVEDORA', nivel: 3, contaPai: '5.1', ativa: true },
  { id: '5.1.5', codigo: '5.1.5', nome: 'Combustível', tipo: 'DESPESA', natureza: 'DEVEDORA', nivel: 3, contaPai: '5.1', ativa: true },
  { id: '5.1.6', codigo: '5.1.6', nome: 'Material de Escritório', tipo: 'DESPESA', natureza: 'DEVEDORA', nivel: 3, contaPai: '5.1', ativa: true },
  { id: '5.1.7', codigo: '5.1.7', nome: 'Telefone e Internet', tipo: 'DESPESA', natureza: 'DEVEDORA', nivel: 3, contaPai: '5.1', ativa: true },
  { id: '5.1.8', codigo: '5.1.8', nome: 'Renda e Aluguéis', tipo: 'DESPESA', natureza: 'DEVEDORA', nivel: 3, contaPai: '5.1', ativa: true },
  { id: '5.2', codigo: '5.2', nome: 'DESPESAS FINANCEIRAS', tipo: 'DESPESA', natureza: 'DEVEDORA', nivel: 2, contaPai: '5', ativa: true },
  { id: '5.2.1', codigo: '5.2.1', nome: 'Juros e Encargos Bancários', tipo: 'DESPESA', natureza: 'DEVEDORA', nivel: 3, contaPai: '5.2', ativa: true },
];

// Lançamentos mock
export const lancamentosMock: Lancamento[] = [
  {
    id: '1',
    data: '2026-03-25',
    descricao: 'Venda de mercadorias - Fatura 001/2026',
    valor: 850000,
    contaDebito: '1.1.1',
    contaDebitoNome: 'Caixa',
    contaCredito: '4.1.1',
    contaCreditoNome: 'Vendas de Mercadorias',
    documento: 'FT001/2026',
    historico: 'Venda à vista de mercadorias conforme fatura',
    status: 'APROVADO',
    categoriaIA: 'Venda à Vista',
    confiancaIA: 98,
    criadoPor: 'Sistema IA',
    criadoEm: '2026-03-25T10:30:00',
  },
  {
    id: '2',
    data: '2026-03-24',
    descricao: 'Pagamento de salários - Março 2026',
    valor: 1250000,
    contaDebito: '5.1.2',
    contaDebitoNome: 'Salários e Ordenados',
    contaCredito: '1.1.2',
    contaCreditoNome: 'Bancos Conta Movimento',
    documento: 'TRANSF-032024',
    historico: 'Pagamento de salários referente ao mês de março',
    status: 'APROVADO',
    categoriaIA: 'Folha de Pagamento',
    confiancaIA: 100,
    criadoPor: 'Sistema IA',
    criadoEm: '2026-03-24T14:00:00',
  },
  {
    id: '3',
    data: '2026-03-23',
    descricao: 'Compra de combustível',
    valor: 45000,
    contaDebito: '5.1.5',
    contaDebitoNome: 'Combustível',
    contaCredito: '1.1.2',
    contaCreditoNome: 'Bancos Conta Movimento',
    documento: 'REC-4532',
    historico: 'Abastecimento veículos da empresa',
    status: 'APROVADO',
    categoriaIA: 'Despesas Operacionais',
    confiancaIA: 95,
    criadoPor: 'Sistema IA',
    criadoEm: '2026-03-23T09:15:00',
  },
  {
    id: '4',
    data: '2026-03-22',
    descricao: 'Pagamento fornecedor - Sonangol',
    valor: 320000,
    contaDebito: '2.1.1',
    contaDebitoNome: 'Fornecedores',
    contaCredito: '1.1.2',
    contaCreditoNome: 'Bancos Conta Movimento',
    documento: 'PGTO-2203',
    historico: 'Quitação parcial fatura fornecedor',
    status: 'PENDENTE',
    categoriaIA: 'Pagamento Fornecedor',
    confiancaIA: 92,
    criadoPor: 'Ana Costa',
    criadoEm: '2026-03-22T16:45:00',
  },
  {
    id: '5',
    data: '2026-03-20',
    descricao: 'Recebimento cliente - TPA',
    valor: 1500000,
    contaDebito: '1.1.2',
    contaDebitoNome: 'Bancos Conta Movimento',
    contaCredito: '1.1.3',
    contaCreditoNome: 'Clientes',
    documento: 'REC-TPA-001',
    historico: 'Recebimento de duplicata',
    status: 'APROVADO',
    categoriaIA: 'Recebimento Cliente',
    confiancaIA: 100,
    criadoPor: 'Sistema IA',
    criadoEm: '2026-03-20T11:20:00',
  },
  {
    id: '6',
    data: '2026-03-19',
    descricao: 'Pagamento energia elétrica - ENDE',
    valor: 78000,
    contaDebito: '5.1.4',
    contaDebitoNome: 'Energia e Água',
    contaCredito: '1.1.2',
    contaCreditoNome: 'Bancos Conta Movimento',
    documento: 'FAT-ENDE-032026',
    historico: 'Pagamento fatura energia março',
    status: 'APROVADO',
    categoriaIA: 'Serviços Públicos',
    confiancaIA: 97,
    criadoPor: 'Sistema IA',
    criadoEm: '2026-03-19T08:30:00',
  },
];

// Categorias de IA
export const categoriasIA: CategoriaIA[] = [
  {
    id: '1',
    nome: 'Venda à Vista',
    palavrasChave: ['venda', 'fatura', 'mercadoria', 'cliente', 'produto'],
    contaDebito: '1.1.1',
    contaCredito: '4.1.1',
    confianca: 98,
    usosRecentes: 145,
  },
  {
    id: '2',
    nome: 'Folha de Pagamento',
    palavrasChave: ['salário', 'ordenado', 'pagamento', 'funcionário', 'colaborador'],
    contaDebito: '5.1.2',
    contaCredito: '1.1.2',
    confianca: 100,
    usosRecentes: 24,
  },
  {
    id: '3',
    nome: 'Despesas Operacionais',
    palavrasChave: ['combustível', 'material', 'escritório', 'despesa', 'consumo'],
    contaDebito: '5.1.5',
    contaCredito: '1.1.2',
    confianca: 92,
    usosRecentes: 89,
  },
  {
    id: '4',
    nome: 'Pagamento Fornecedor',
    palavrasChave: ['fornecedor', 'pagamento', 'quitação', 'dívida', 'compra'],
    contaDebito: '2.1.1',
    contaCredito: '1.1.2',
    confianca: 95,
    usosRecentes: 67,
  },
  {
    id: '5',
    nome: 'Recebimento Cliente',
    palavrasChave: ['recebimento', 'cliente', 'duplicata', 'cobrança', 'pagamento'],
    contaDebito: '1.1.2',
    contaCredito: '1.1.3',
    confianca: 97,
    usosRecentes: 112,
  },
  {
    id: '6',
    nome: 'Serviços Públicos',
    palavrasChave: ['energia', 'água', 'ENDE', 'EPAL', 'utilidade', 'luz'],
    contaDebito: '5.1.4',
    contaCredito: '1.1.2',
    confianca: 94,
    usosRecentes: 36,
  },
];

// Resumo financeiro mock
export const resumoFinanceiroMock: ResumoFinanceiro = {
  totalAtivos: 12500000,
  totalPassivos: 3200000,
  totalReceitas: 8950000,
  totalDespesas: 4320000,
  resultado: 4630000,
};

// Dados para gráficos
export const dadosGraficoMensal = [
  { mes: 'Set', receitas: 2100000, despesas: 1200000 },
  { mes: 'Out', receitas: 2450000, despesas: 1350000 },
  { mes: 'Nov', receitas: 2300000, despesas: 1400000 },
  { mes: 'Dez', receitas: 3200000, despesas: 1650000 },
  { mes: 'Jan', receitas: 2800000, despesas: 1450000 },
  { mes: 'Fev', receitas: 2900000, despesas: 1500000 },
  { mes: 'Mar', receitas: 3150000, despesas: 1570000 },
];

export const dadosCategoriasIA = [
  { categoria: 'Vendas', quantidade: 145, precisao: 98 },
  { categoria: 'Recebimentos', quantidade: 112, precisao: 97 },
  { categoria: 'Despesas Operacionais', quantidade: 89, precisao: 92 },
  { categoria: 'Pagamentos', quantidade: 67, precisao: 95 },
  { categoria: 'Serviços', quantidade: 36, precisao: 94 },
  { categoria: 'Folha de Pagamento', quantidade: 24, precisao: 100 },
];

// Função auxiliar para formatar valores em Kwanza
export const formatarKwanza = (valor: number): string => {
  return new Intl.NumberFormat('pt-AO', {
    style: 'currency',
    currency: 'AOA',
  }).format(valor);
};

// Função auxiliar para formatar data
export const formatarData = (data: string): string => {
  return new Date(data).toLocaleDateString('pt-AO', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};
