package isaf.tfc.autolancamentosbackend.dto;

import lombok.Data;

/**
 * Fase 20 do plano de 20 fases — POST /auth/registo. nomeEmpresa/nifEmpresa
 * só são usados se ainda não existir nenhuma Empresa nesta instalação (ver
 * AuthController.registar) — este projeto continua single-tenant (uma
 * única empresa por instalação, ver EmpresaService), o registo público
 * nunca cria uma SEGUNDA empresa.
 */
@Data
public class RegistoRequestDTO {

    private String nome;

    private String nif;

    private String email;

    private String senha;

    private String nomeEmpresa;

    private String nifEmpresa;
}
