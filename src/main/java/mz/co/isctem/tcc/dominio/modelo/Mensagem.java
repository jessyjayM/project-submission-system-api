package mz.co.isctem.tcc.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mensagem do canal privado entre estudante e supervisor (RF07).
 *
 * <p>O Coordenador pode consultar o historico para acompanhamento institucional, mas nunca editar
 * ou eliminar mensagens.
 */
@Entity
@Table(name = "mensagem")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tcc_id", nullable = false)
    private Tcc tcc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remetente_id", nullable = false)
    private Utilizador remetente;

    @Column(nullable = false, length = 4000)
    private String conteudo;

    @Column(name = "anexo_nome", length = 255)
    private String anexoNome;

    @Column(name = "anexo_caminho", length = 500)
    private String anexoCaminho;

    @Column(name = "anexo_tamanho")
    private Long anexoTamanho;

    @Column(name = "enviada_em", nullable = false)
    @Builder.Default
    private Instant enviadaEm = Instant.now();

    @Column(name = "lida_em")
    private Instant lidaEm;

    public boolean temAnexo() {
        return anexoCaminho != null;
    }
}
