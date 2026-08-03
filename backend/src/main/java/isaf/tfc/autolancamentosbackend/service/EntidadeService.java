package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.model.TipoEntidade;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import org.springframework.stereotype.Service;

/**
 * Resolve a entidade de origem de um documento (T2 — arquivo por entidade),
 * a partir do NIF/nome que a análise (FastAPI) já devolve. Documentos sem
 * NIF/entidade identificável partilham sempre a mesma entidade "Sem
 * identificação" — não se cria uma entidade nova por cada documento
 * desconhecido.
 */
@Service
public class EntidadeService {

    private static final String NIF_SEM_IDENTIFICACAO = "SEM_NIF";
    private static final String NOME_SEM_IDENTIFICACAO = "Sem identificação";

    private final EntidadeRepository entidadeRepository;

    public EntidadeService(EntidadeRepository entidadeRepository) {
        this.entidadeRepository = entidadeRepository;
    }

    public Entidade resolverOuCriar(String nif, String nome, String tipoDocumento) {
        boolean identificada = nif != null && !nif.isBlank();

        String nifNormalizado = identificada ? nif.trim() : NIF_SEM_IDENTIFICACAO;
        String nomeFinal = identificada && nome != null && !nome.isBlank()
                ? nome.trim()
                : NOME_SEM_IDENTIFICACAO;
        TipoEntidade tipo = identificada ? inferirTipo(tipoDocumento) : TipoEntidade.DESCONHECIDO;

        return entidadeRepository.findByNif(nifNormalizado)
                .orElseGet(() -> entidadeRepository.save(new Entidade(null, nomeFinal, nifNormalizado, tipo)));
    }

    private TipoEntidade inferirTipo(String tipoDocumento) {
        if (tipoDocumento == null) {
            return TipoEntidade.DESCONHECIDO;
        }
        return switch (tipoDocumento) {
            case "compra_mercadoria", "compra_servico", "pagamento_fornecedor" -> TipoEntidade.FORNECEDOR;
            case "venda_mercadoria", "prestacao_servico", "recebimento_cliente" -> TipoEntidade.CLIENTE;
            default -> TipoEntidade.DESCONHECIDO;
        };
    }
}
