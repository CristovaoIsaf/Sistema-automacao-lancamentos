package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.AnaliseResponse;
import isaf.tfc.autolancamentosbackend.dto.LinhaSugeridaDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Marco 2: confirma que o backend envia os bytes reais do documento (não um id em JSON)
 * à API de análise via multipart, e que a resposta estruturada (linhas equilibradas de
 * partidas dobradas) é corretamente desserializada.
 *
 * Testa AnalisadorDocumentoIA isoladamente (não o fluxo completo via DocumentoRepository)
 * porque a tabela "documentos" da BD real tem uma coluna empresa_id NOT NULL (FK para
 * empresas) que ainda não existe em DocumentoContabilistico.java — ver Pergunta 3 do
 * plano (multi-empresa). A leitura/gravação via DocumentoRepository/SugestaoRepository
 * usa Spring Data JPA padrão e foi verificada por leitura de código + compilação.
 *
 * Requer o serviço FastAPI (fastapi.services.url) a correr.
 */
@SpringBootTest
class AnaliseContabilServiceIntegrationTest {

    @Autowired
    private AnalisadorDocumentoIA analisadorDocumentoIA;

    @Test
    void analisar_enviaBytesReaisEDevolveLinhasEquilibradas() throws Exception {
        AnaliseResponse resposta = analisadorDocumentoIA.analisar(gerarImagemDeFaturaTeste(), "fatura_teste.png", null, null);

        assertThat(resposta).isNotNull();
        assertThat(resposta.isSuccess()).isTrue();
        assertThat(resposta.getTextoOcr()).isNotBlank();
        assertThat(resposta.getLinhas()).isNotEmpty();

        BigDecimal totalDebito = somaLinhas(resposta.getLinhas(), LinhaSugeridaDTO::getDebito);
        BigDecimal totalCredito = somaLinhas(resposta.getLinhas(), LinhaSugeridaDTO::getCredito);

        assertThat(totalDebito).isEqualByComparingTo(totalCredito);
    }

    private BigDecimal somaLinhas(List<LinhaSugeridaDTO> linhas, Function<LinhaSugeridaDTO, String> campo) {
        return linhas.stream()
                .map(campo)
                .filter(valor -> valor != null && !valor.isBlank())
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private byte[] gerarImagemDeFaturaTeste() throws Exception {
        BufferedImage imagem = new BufferedImage(900, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 900, 400);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 26));

        String[] linhasTexto = {
                "FATURA",
                "Emitente: Loja Teste Lda",
                "NIF: 5417123456",
                "Cliente: Cliente XYZ",
                "NIF: 5000123456",
                "FT 2026/001",
                "Data: 15/03/2026",
                "Total: 15000.00 AOA",
        };
        int y = 40;
        for (String linha : linhasTexto) {
            g.drawString(linha, 20, y);
            y += 40;
        }
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", out);
        return out.toByteArray();
    }
}
