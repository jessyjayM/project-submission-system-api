package mz.co.isctem.tcc.seguranca;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mz.co.isctem.tcc.config.PropriedadesIsctem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuracao do Spring Security: API sem estado, autenticada por Bearer token JWT.
 *
 * <p>As permissoes por perfil (quadro da seccao 5.5) sao aplicadas em duas camadas: as regras
 * grosseiras por rota ficam aqui, e as regras de propriedade dos dados (RNF06 - um supervisor so ve
 * os seus orientandos, um estudante so ve o seu processo) sao verificadas nos servicos.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class ConfiguracaoSeguranca {

    private final PropriedadesIsctem propriedades;
    private final FiltroAlteracaoSenhaObrigatoria filtroSenha;

    @Bean
    public PasswordEncoder codificadorSenha() {
        // RNF01: as senhas nunca sao guardadas em texto plano.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public JwtDecoder descodificadorJwt() {
        return NimbusJwtDecoder.withSecretKey(ServicoTokens.chaveHmac(propriedades.security().jwtSecret()))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Converte a reivindicacao {@code perfil} nas authorities do Spring Security. */
    @Bean
    public JwtAuthenticationConverter conversorAutenticacaoJwt() {
        JwtGrantedAuthoritiesConverter delegado = new JwtGrantedAuthoritiesConverter();
        delegado.setAuthoritiesClaimName(Reivindicacoes.PERFIL);
        delegado.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(
                jwt -> {
                    List<GrantedAuthority> authorities = List.copyOf(delegado.convert(jwt));
                    return authorities.isEmpty()
                            ? List.of(new SimpleGrantedAuthority("ROLE_ANONIMO"))
                            : authorities;
                });
        return conversor;
    }

    @Bean
    public SecurityFilterChain cadeiaFiltros(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(gestao -> gestao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        pedidos ->
                                pedidos
                                        // O logout autentica-se pelo proprio refresh token que
                                        // vem no corpo; exigir access token impediria a revogacao
                                        // quando a sessao ja tivesse expirado.
                                        .requestMatchers(
                                                "/api/auth/login",
                                                "/api/auth/refresh",
                                                "/api/auth/logout",
                                                "/actuator/health")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        // RF01: gestao de contas exclusiva do Administrador
                                        .requestMatchers("/api/utilizadores/**")
                                        .hasRole("ADMIN")
                                        // RF04 e RF10: supervisao e prazos sao do Coordenador
                                        .requestMatchers("/api/supervisao/**", "/api/prazos/**")
                                        .hasRole("COORDENADOR")
                                        // RF12: relatorios institucionais
                                        .requestMatchers("/api/relatorios/**")
                                        .hasRole("COORDENADOR")
                                        .requestMatchers("/api/auditoria/**")
                                        .hasAnyRole("ADMIN", "COORDENADOR")
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorAutenticacaoJwt())))
                .addFilterAfter(filtroSenha, AuthorizationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource fonteConfiguracaoCors() {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(propriedades.cors().origens());
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("*"));
        configuracao.setExposedHeaders(List.of("Content-Disposition"));
        configuracao.setAllowCredentials(true);
        configuracao.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", configuracao);
        return fonte;
    }
}
