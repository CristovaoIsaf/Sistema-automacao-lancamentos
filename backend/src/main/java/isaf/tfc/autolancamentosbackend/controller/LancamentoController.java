package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.LancamentoRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaLancamentoDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.service.LancamentoService;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/lancamentos")
@RequiredArgsConstructor
public class LancamentoController {


    private final LancamentoService service;


    // Fase 1 do plano de 20 fases — escrita de lançamentos manuais é um
    // acto de Contabilista (RN002/RN010, já aplicado no frontend em
    // AuthContext.permissoes().podeEscreverLancamentos — Administrador
    // gere o sistema mas não faz lançamentos, Auditor nunca escreve).
    // Antes desta fase, este endpoint não tinha nenhuma restrição de
    // papel no backend — a regra só existia como esconder/desabilitar
    // botões no frontend, contornável por chamar a API directamente.
    @PreAuthorize("hasRole('CONTABILISTA')")
    @PostMapping
    public ResponseEntity<LancamentoResponseDTO> criar(
            @RequestBody LancamentoRequestDTO request
    ){

        LancamentoResponseDTO response =
                service.criarLancamentoManual(request);


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    /**
     * Lista todos os lançamentos (usado pela página de histórico no frontend).
     */
    @GetMapping
    public ResponseEntity<List<LancamentoResponseDTO>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }


    /**
     * Detalhe de um lançamento (usado pelo botão "Ver" na tabela).
     */
    @GetMapping("/{id}")
    public ResponseEntity<LancamentoResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }


    /**
     * Atualiza um lançamento ainda não cancelado (usado pelo botão "Editar").
     */
    @PreAuthorize("hasRole('CONTABILISTA')")
    @PutMapping("/{id}")
    public ResponseEntity<LancamentoResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody LancamentoRequestDTO request
    ) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }


    /**
     * Cancela um lançamento (não apaga — mantém rasto para auditoria).
     */
    @PreAuthorize("hasRole('CONTABILISTA')")
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        service.cancelar(id);
        return ResponseEntity.noContent().build();
    }


    /**
     * Exporta os lançamentos de um período em Excel (.xlsx), uma linha por
     * LinhaLancamento (débito/crédito), para respeitar partidas dobradas.
     */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) EstadoLancamento estado
    ) {

        List<LancamentoResponseDTO> lancamentos =
                service.listarPorPeriodo(inicio, fim, estado);

        byte[] excel = gerarExcel(lancamentos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lancamentos.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }


    private byte[] gerarExcel(List<LancamentoResponseDTO> lancamentos) {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Lancamentos");

            String[] cabecalho = {"Lançamento", "Data", "Descrição", "Estado", "Origem", "Conta", "Débito", "Crédito"};
            Row linhaCabecalho = sheet.createRow(0);
            for (int i = 0; i < cabecalho.length; i++) {
                linhaCabecalho.createCell(i).setCellValue(cabecalho[i]);
            }

            int numeroLinha = 1;
            for (LancamentoResponseDTO lancamento : lancamentos) {
                for (LinhaLancamentoDTO linha : lancamento.getLinhas()) {
                    Row row = sheet.createRow(numeroLinha++);

                    row.createCell(0).setCellValue(lancamento.getId());
                    row.createCell(1).setCellValue(lancamento.getData().toString());
                    row.createCell(2).setCellValue(lancamento.getDescricao());
                    row.createCell(3).setCellValue(lancamento.getEstado().toString());
                    row.createCell(4).setCellValue(lancamento.getOrigem().toString());
                    row.createCell(5).setCellValue(linha.getConta());
                    setValorNumerico(row.createCell(6), linha.getDebito());
                    setValorNumerico(row.createCell(7), linha.getCredito());
                }
            }

            for (int i = 0; i < cabecalho.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o ficheiro Excel", e);
        }
    }


    private void setValorNumerico(Cell cell, java.math.BigDecimal valor) {
        if (valor != null) {
            cell.setCellValue(valor.doubleValue());
        }
    }

}
