package mz.co.isctem.tcc.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mz.co.isctem.tcc.dominio.enums.EstadoUtilizador;
import mz.co.isctem.tcc.dominio.enums.PerfilUtilizador;

/**
 * Conta de acesso a plataforma (RF01, RF02, RF03).
 *
 * <p>A senha e sempre guardada cifrada com BCrypt (RNF01). O campo {@code deveAlterarSenha} obriga
 * a troca no primeiro acesso e apos qualquer redefinicao feita pelo Administrador.
 */
@Entity
@Table(name = "utilizador")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Utilizador extends EntidadeAuditada {

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilUtilizador tipo;

    /** Email institucional. Credencial de login de Admin, Coordenador e Docente. */
    @Column(length = 150)
    private String email;

    /** Codigo do estudante. Credencial de login do perfil Estudante. */
    @Column(length = 50)
    private String codigo;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoUtilizador estado = EstadoUtilizador.ACTIVO;

    @Column(name = "deve_alterar_senha", nullable = false)
    @Builder.Default
    private boolean deveAlterarSenha = true;

    @Column(name = "tentativas_falhadas", nullable = false)
    @Builder.Default
    private int tentativasFalhadas = 0;

    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    @Column(name = "ultimo_acesso")
    private Instant ultimoAcesso;

    /** Credencial usada no ecra de login, consoante o perfil (RF02). */
    public String credencialLogin() {
        return tipo.autenticaPorCodigo() ? codigo : email;
    }

    public boolean activo() {
        return estado == EstadoUtilizador.ACTIVO;
    }

    /** RF02: bloqueio temporario apos tentativas falhadas consecutivas. */
    public boolean bloqueado() {
        return bloqueadoAte != null && bloqueadoAte.isAfter(Instant.now());
    }

    public void registarAcessoValido() {
        this.tentativasFalhadas = 0;
        this.bloqueadoAte = null;
        this.ultimoAcesso = Instant.now();
    }
}
