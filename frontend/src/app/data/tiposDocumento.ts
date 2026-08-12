// Mesmos tipos de fastapi/app/services/pgc.py (TIPOS_VALIDOS) — usados nos
// dropdowns de correção de classificação (Arquivo.tsx, UploadDocumentos.tsx).
export const TIPO_DOCUMENTO_LABEL: Record<string, string> = {
  compra_mercadoria: 'Compra de mercadoria',
  compra_servico: 'Compra de serviço',
  venda_mercadoria: 'Venda de mercadoria',
  prestacao_servico: 'Prestação de serviço',
  pagamento_fornecedor: 'Pagamento a fornecedor',
  recebimento_cliente: 'Recebimento de cliente',
  a_classificar: 'A classificar',
};
