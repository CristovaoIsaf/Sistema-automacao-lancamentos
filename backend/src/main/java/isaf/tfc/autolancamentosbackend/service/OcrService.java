/*package isaf.tfc.autolancamentosbackend.service;

import lombok.Value;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.hibernate.query.Page;
import org.springframework.http.converter.ObjectToStringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class OcrService {

    @Value("${tesseract.datapath}")
    private String tessDataPath;

    @Value("${tesseract.language}")
    private String language;

    public String extrairTexto(MultipartFile ficheiro) throws TesseractException {
        try {
            // Converter MultipartFile em File temporário
            File tempFile = File.createTempFile("ocr_",
                    getExtensao(ficheiro.getOriginalFilename()));
            ficheiro.transferTo(tempFile);

            // Inicializar Tesseract
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage(language);

            // Se for PDF — converter página por página
            if (isPdf(ficheiro)) {
                return extrairTextoPdf(tempFile, tesseract);
            }

            // Se for imagem (PNG, JPG)
            String texto = tesseract.doOCR(tempFile);
            tempFile.delete(); // limpar ficheiro temporário
            return texto;

        } catch (TesseractException | IOException e) {
            throw new TesseractException("Erro ao processar OCR: " + e.getMessage(), e);
        }
    }

    private String extrairTextoPdf(File pdf, ITesseract tesseract)
            throws TesseractException {
        // Tess4J suporta PDF directamente
        List<Page> pages = new Object.getPages(pdf, null);
        StringBuilder sb = new StringBuilder();
        for (Page page : pages) {
            sb.append(tesseract.doOCR(page.getImage()));
            sb.append("\n");
        }
        return sb.toString();
    }

    private boolean isPdf(MultipartFile f) {
        return "application/pdf".equals(f.getContentType()) ||
                f.getOriginalFilename().toLowerCase().endsWith(".pdf");
    }

    private String getExtensao(String nome) {
        return nome != null && nome.contains(".")
                ? nome.substring(nome.lastIndexOf("."))
                : ".tmp";
    }
}*/
