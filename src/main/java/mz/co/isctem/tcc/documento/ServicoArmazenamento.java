package mz.co.isctem.tcc.documento;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import mz.co.isctem.tcc.comum.excepcao.RegraNegocioException;
import mz.co.isctem.tcc.config.PropriedadesIsctem;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * RF06: armazenamento dos ficheiros no sistema de ficheiros do servidor, com apenas a referencia
 * guardada na base de dados.
 *
 * <p>Os nomes gravados sao gerados pelo sistema (UUID + extensao validada); o nome original do
 * utilizador nunca toca no caminho, o que elimina a travessia de directorios.
 */
@Slf4j
@Service
public class ServicoArmazenamento {

    private final PropriedadesIsctem.Armazenamento config;
    private final Path raiz;

    public ServicoArmazenamento(PropriedadesIsctem propriedades) {
        this.config = propriedades.storage();
        this.raiz = Path.of(config.root()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
        } catch (IOException e) {
            throw new IllegalStateException("Nao foi possivel criar a pasta de armazenamento: " + raiz, e);
        }
    }

    /** Metadados do ficheiro acabado de gravar. */
    public record FicheiroGuardado(
            String caminhoRelativo, String nomeOriginal, String extensao, String tipoConteudo, long tamanho, String hashSha256) {}

    /**
     * Valida e grava o ficheiro submetido.
     *
     * @param subPasta caminho logico dentro da raiz (ex.: {@code tcc-3/fase-2})
     */
    public FicheiroGuardado guardar(MultipartFile ficheiro, String subPasta) {
        if (ficheiro == null || ficheiro.isEmpty()) {
            throw new RegraNegocioException("Nenhum ficheiro foi enviado.");
        }
        if (ficheiro.getSize() > config.tamanhoMaximoBytes()) {
            throw new RegraNegocioException(
                    "O ficheiro excede o tamanho maximo de %d MB."
                            .formatted(config.tamanhoMaximoBytes() / (1024 * 1024)));
        }
        String extensao = extensaoDe(ficheiro.getOriginalFilename());
        if (!config.formatoAceite(extensao)) {
            throw new RegraNegocioException(
                    "Formato nao aceite. Formatos permitidos: %s.".formatted(String.join(", ", config.formatosAceites())));
        }

        Path destinoPasta = resolverSeguro(subPasta);
        String nomeGerado = UUID.randomUUID() + "." + extensao;
        Path destino = destinoPasta.resolve(nomeGerado);

        try {
            Files.createDirectories(destinoPasta);
            try (InputStream entrada = ficheiro.getInputStream()) {
                Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            String hash = calcularHash(destino);
            String relativo = raiz.relativize(destino).toString().replace('\\', '/');
            return new FicheiroGuardado(
                    relativo,
                    nomeSeguro(ficheiro.getOriginalFilename()),
                    extensao,
                    ficheiro.getContentType() == null ? "application/octet-stream" : ficheiro.getContentType(),
                    ficheiro.getSize(),
                    hash);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gravar o ficheiro submetido.", e);
        }
    }

    public Resource carregar(String caminhoRelativo) {
        Path ficheiro = resolverSeguro(caminhoRelativo);
        if (!Files.exists(ficheiro) || !Files.isReadable(ficheiro)) {
            throw new RegraNegocioException("O ficheiro ja nao esta disponivel no servidor.");
        }
        try {
            return new UrlResource(ficheiro.toUri());
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler o ficheiro " + caminhoRelativo, e);
        }
    }

    public Path caminhoAbsoluto(String caminhoRelativo) {
        return resolverSeguro(caminhoRelativo);
    }

    /** Garante que o caminho resultante nunca sai da raiz de armazenamento. */
    private Path resolverSeguro(String caminhoRelativo) {
        Path resolvido = raiz.resolve(caminhoRelativo).normalize();
        if (!resolvido.startsWith(raiz)) {
            throw new RegraNegocioException("Caminho de ficheiro invalido.");
        }
        return resolvido;
    }

    private static String extensaoDe(String nomeFicheiro) {
        if (nomeFicheiro == null) {
            return "";
        }
        int ponto = nomeFicheiro.lastIndexOf('.');
        return ponto < 0 ? "" : nomeFicheiro.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }

    /** Remove componentes de caminho do nome apresentado ao utilizador. */
    private static String nomeSeguro(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "documento";
        }
        String limpo = nomeOriginal.replaceAll("[\\\\/]", "_").trim();
        return limpo.length() > 255 ? limpo.substring(limpo.length() - 255) : limpo;
    }

    private static String calcularHash(Path ficheiro) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(ficheiro)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel nesta JVM", e);
        }
    }
}
