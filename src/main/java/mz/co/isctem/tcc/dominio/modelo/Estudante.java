package mz.co.isctem.tcc.dominio.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mz.co.isctem.tcc.dominio.enums.AreaCurso;
import mz.co.isctem.tcc.dominio.enums.EstadoProcessoEstudante;

/**
 * Estudante inscrito no processo de TCC.
 *
 * <p>Regra de negocio (RF04): cada estudante possui apenas um supervisor activo num dado momento.
 * O historico de atribuicoes fica em {@link AtribuicaoSupervisor}.
 */
@Entity
@Table(name = "estudante")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Estudante extends EntidadeAuditada {

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST, optional = false)
    @JoinColumn(name = "utilizador_id", nullable = false, unique = true)
    private Utilizador utilizador;

    @Column(nullable = false, length = 120)
    private String curso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AreaCurso area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Docente supervisor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_processo", nullable = false, length = 30)
    @Builder.Default
    private EstadoProcessoEstudante estadoProcesso = EstadoProcessoEstudante.SEM_SUPERVISOR;

    public boolean temSupervisor() {
        return supervisor != null;
    }
}
