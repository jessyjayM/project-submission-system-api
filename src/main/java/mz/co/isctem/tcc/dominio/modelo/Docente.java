package mz.co.isctem.tcc.dominio.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Docente/Supervisor. Pode orientar varios estudantes em simultaneo (RF04). */
@Entity
@Table(name = "docente")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Docente extends EntidadeAuditada {

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST, optional = false)
    @JoinColumn(name = "utilizador_id", nullable = false, unique = true)
    private Utilizador utilizador;

    @Column(nullable = false, length = 120)
    private String departamento;
}
