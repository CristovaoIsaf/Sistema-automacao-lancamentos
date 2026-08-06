package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.CategoriaContaDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Camada de categorização por cima do plano de contas real
 * (ContaController.CONTAS) — agrupa as contas por tipo de operação
 * (Vendas, Compras, ...) para o lançamento manual não obrigar a percorrer
 * as 18 contas todas de cada vez. Não define nenhuma conta nova, só
 * referencia códigos já existentes; uma conta pode pertencer a várias
 * categorias (ex. "45 Caixa" é principal em "Caixa" e ocasional em
 * "Vendas"/"Compras").
 *
 * Só existem aqui categorias com pelo menos uma conta real por trás — não
 * há "Salários"/"Activos Fixos"/etc. enquanto essas contas não existirem no
 * plano (ver ContaController), para não mostrar categorias vazias.
 * Acrescentar uma categoria nova é só uma entrada nova em CATEGORIAS.
 */
@RestController
@RequestMapping("/api/categorias-conta")
public class CategoriaContaController {

    private static final Map<String, String[]> PRINCIPAIS_POR_CATEGORIA = new LinkedHashMap<>();
    private static final Map<String, String[]> OCASIONAIS_POR_CATEGORIA = new LinkedHashMap<>();

    static {
        PRINCIPAIS_POR_CATEGORIA.put("Vendas", new String[]{"31", "61", "62", "34.5.2"});
        OCASIONAIS_POR_CATEGORIA.put("Vendas", new String[]{"43", "45"});

        PRINCIPAIS_POR_CATEGORIA.put("Compras", new String[]{"32", "21", "26", "34.5.1"});
        OCASIONAIS_POR_CATEGORIA.put("Compras", new String[]{"43", "45"});

        PRINCIPAIS_POR_CATEGORIA.put("Tesouraria", new String[]{"45", "43", "31", "32"});
        OCASIONAIS_POR_CATEGORIA.put("Tesouraria", new String[]{});

        PRINCIPAIS_POR_CATEGORIA.put("Bancos", new String[]{"43"});
        OCASIONAIS_POR_CATEGORIA.put("Bancos", new String[]{"45"});

        PRINCIPAIS_POR_CATEGORIA.put("Caixa", new String[]{"45"});
        OCASIONAIS_POR_CATEGORIA.put("Caixa", new String[]{"43"});

        PRINCIPAIS_POR_CATEGORIA.put("Impostos", new String[]{"34.5.1", "34.5.2"});
        OCASIONAIS_POR_CATEGORIA.put("Impostos", new String[]{});

        PRINCIPAIS_POR_CATEGORIA.put("Operações Diversas", new String[]{
                "75", "75.2.11", "75.2.12", "75.2.14", "75.2.20", "75.2.21", "75.2.22", "75.2.23"});
        OCASIONAIS_POR_CATEGORIA.put("Operações Diversas", new String[]{"32", "45", "43"});
    }

    @GetMapping
    public ResponseEntity<List<CategoriaContaDTO>> listar() {
        List<CategoriaContaDTO> categorias = PRINCIPAIS_POR_CATEGORIA.keySet().stream()
                .map(nome -> new CategoriaContaDTO(
                        nome,
                        contas(PRINCIPAIS_POR_CATEGORIA.get(nome)),
                        contas(OCASIONAIS_POR_CATEGORIA.get(nome))
                ))
                .toList();
        return ResponseEntity.ok(categorias);
    }

    private List<isaf.tfc.autolancamentosbackend.dto.ContaDTO> contas(String[] codigos) {
        return List.of(codigos).stream().map(ContaController::porCodigo).toList();
    }
}
