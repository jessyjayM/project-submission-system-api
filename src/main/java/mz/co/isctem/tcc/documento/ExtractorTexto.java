package mz.co.isctem.tcc.documento;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

/**
 * Extraccao do texto dos documentos submetidos, base da analise de similaridade (RF08).
 *
 * <p>Suporta os formatos aceites em RF06: .pdf (PDFBox), .docx (POI XWPF) e .doc (POI HWPF). Um
 * PDF digitalizado sem camada de texto devolve vazio — o relatorio fica marcado como FALHOU e o
 * supervisor e avisado de que a verificacao nao pode ser conclusiva.
 */
@Slf4j
@Component
public class ExtractorTexto {

    /** Limite de caracteres guardados por documento, para conter o tamanho da base de dados. */
    private static final int MAX_CARACTERES = 400_000;

    public String extrair(Path ficheiro, String extensao) {
        try {
            String texto =
                    switch (extensao == null ? "" : extensao.toLowerCase()) {
                        case "pdf" -> extrairDePdf(ficheiro);
                        case "docx" -> extrairDeDocx(ficheiro);
                        case "doc" -> extrairDeDoc(ficheiro);
                        default -> "";
                    };
            return normalizar(texto);
        } catch (Exception e) {
            log.warn("Nao foi possivel extrair texto de {} ({}): {}", ficheiro, extensao, e.getMessage());
            return "";
        }
    }

    private String extrairDePdf(Path ficheiro) throws IOException {
        try (PDDocument documento = Loader.loadPDF(ficheiro.toFile())) {
            PDFTextStripper extractor = new PDFTextStripper();
            extractor.setSortByPosition(true);
            return extractor.getText(documento);
        }
    }

    private String extrairDeDocx(Path ficheiro) throws IOException {
        try (InputStream entrada = Files.newInputStream(ficheiro);
                XWPFDocument documento = new XWPFDocument(entrada);
                XWPFWordExtractor extractor = new XWPFWordExtractor(documento)) {
            return extractor.getText();
        }
    }

    private String extrairDeDoc(Path ficheiro) throws IOException {
        try (InputStream entrada = Files.newInputStream(ficheiro);
                WordExtractor extractor = new WordExtractor(entrada)) {
            return extractor.getText();
        }
    }

    private String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String limpo = texto.replaceAll("\\s+", " ").trim();
        return limpo.length() > MAX_CARACTERES ? limpo.substring(0, MAX_CARACTERES) : limpo;
    }
}
