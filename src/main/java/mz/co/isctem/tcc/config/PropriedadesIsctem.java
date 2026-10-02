package mz.co.isctem.tcc.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Parametros de configuracao do sistema, agrupados sob o prefixo {@code isctem}. */
@ConfigurationProperties(prefix = "isctem")
public record PropriedadesIsctem(
        Seguranca security,
        Armazenamento storage,
        Plagio plagio,
        Notificacoes notificacoes,
        Cors cors,
        Seed seed) {

    /** Dados criados no arranque para permitir o primeiro acesso e as demonstracoes. */
    public record Seed(boolean adminInicial, String adminEmail, String adminSenha, boolean demo) {}

    /** RF02, RF03, RNF01. */
    public record Seguranca(
            String jwtSecret,
            Duration accessTokenTtl,
            Duration refreshTokenTtl,
            int maxFailedAttempts,
            Duration lockDuration,
            int senhaTamanhoMinimo) {}

    /** RF06. */
    public record Armazenamento(String root, List<String> formatosAceites, long tamanhoMaximoBytes) {

        public boolean formatoAceite(String extensao) {
            return extensao != null && formatosAceites.contains(extensao.toLowerCase());
        }
    }

    /** RF08. */
    public record Plagio(BigDecimal limiarAlerta, int maxDocumentosSemelhantes, int minTermos) {}

    /** RF09. */
    public record Notificacoes(boolean emailActivo, String remetente) {}

    public record Cors(List<String> origens) {}
}
