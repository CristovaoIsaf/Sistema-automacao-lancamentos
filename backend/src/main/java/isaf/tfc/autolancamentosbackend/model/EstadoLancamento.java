package isaf.tfc.autolancamentosbackend.model;

public enum EstadoLancamento {

    // Auditoria C01/C15: até aqui nunca era usado na prática — um
    // lançamento manual nascia logo VALIDADO, aprovado pelo próprio
    // criador (ver LancamentoServiceImpl.criarLancamentoManual). Agora é o
    // estado real de um lançamento manual à espera de um segundo
    // contabilista o aprovar (ver LancamentoService.aprovar) — por isso
    // continua fora do Balancete/DRE/Balanço/Fluxo de Caixa/Livro Razão
    // (ver BalanceteService.lancamentosValidadosNoIntervalo), tal como
    // sempre esteve.
    PENDENTE,
    VALIDADO,
    CANCELADO,

    // Auditoria C03: pedido de anulação de um lançamento já VALIDADO, à
    // espera de um segundo contabilista (≠ de quem pediu) o aprovar ou
    // rejeitar — ver LancamentoService.solicitarCancelamento/
    // aprovarCancelamento/rejeitarCancelamento. Continua a contar como
    // ativo nos relatórios (o lançamento ainda não foi revertido).
    CANCELAMENTO_PENDENTE
}