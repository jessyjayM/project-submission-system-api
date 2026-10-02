package mz.co.isctem.tcc.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.config.PropriedadesIsctem;
import mz.co.isctem.tcc.dominio.modelo.RefreshToken;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.dominio.repositorio.RefreshTokenRepositorio;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emissao de access tokens (JWT assinado com HS256) e refresh tokens opacos (RF02).
 *
 * <p>Do refresh token guarda-se apenas o SHA-256: se a base de dados for comprometida, os tokens em
 * circulacao continuam inutilizaveis.
 */
@Service
public class ServicoTokens {

    private final PropriedadesIsctem.Seguranca config;
    private final RefreshTokenRepositorio refreshTokens;
    private final JwtEncoder codificador;
    private final SecureRandom aleatorio = new SecureRandom();

    public ServicoTokens(PropriedadesIsctem propriedades, RefreshTokenRepositorio refreshTokens) {
        this.config = propriedades.security();
        this.refreshTokens = refreshTokens;
        this.codificador = new NimbusJwtEncoder(new ImmutableSecret<>(chaveHmac(config.jwtSecret())));
    }

    static SecretKeySpec chaveHmac(String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "isctem.security.jwt-secret tem de ter pelo menos 32 caracteres para HS256.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /** Access token de curta duracao, com o perfil e o estado da senha do utilizador. */
    public String emitirAccessToken(Utilizador utilizador) {
        Instant agora = Instant.now();
        JwtClaimsSet reivindicacoes =
                JwtClaimsSet.builder()
                        .issuer("isctem-tcc")
                        .subject(String.valueOf(utilizador.getId()))
                        .issuedAt(agora)
                        .expiresAt(agora.plus(config.accessTokenTtl()))
                        .claim(Reivindicacoes.NOME, utilizador.getNome())
                        .claim(Reivindicacoes.PERFIL, utilizador.getTipo().name())
                        .claim(Reivindicacoes.CREDENCIAL, utilizador.credencialLogin())
                        .claim(Reivindicacoes.DEVE_ALTERAR_SENHA, utilizador.isDeveAlterarSenha())
                        .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return codificador.encode(JwtEncoderParameters.from(cabecalho, reivindicacoes)).getTokenValue();
    }

    /** Gera um refresh token opaco e persiste apenas o seu hash. */
    @Transactional
    public String emitirRefreshToken(Utilizador utilizador) {
        byte[] bytes = new byte[48];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(
                RefreshToken.builder()
                        .utilizador(utilizador)
                        .tokenHash(hash(token))
                        .expiraEm(Instant.now().plus(config.refreshTokenTtl()))
                        .build());
        return token;
    }

    @Transactional
    public void revogarTodosDe(Long utilizadorId) {
        refreshTokens.revogarTodosDoUtilizador(utilizadorId);
    }

    public static String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponivel nesta JVM", e);
        }
    }

    public long segundosDeValidadeDoAccessToken() {
        return config.accessTokenTtl().toSeconds();
    }

    public long segundosDeValidadeDoRefreshToken() {
        return config.refreshTokenTtl().toSeconds();
    }
}
