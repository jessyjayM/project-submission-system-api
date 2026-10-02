package mz.co.isctem.tcc.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Versao imutavel de um documento submetido (RF06).
 *
 * <p>O ficheiro fica no sistema de ficheiros do servidor e apenas a referencia e guardada na base
 * de dados. O texto extraido alimenta a analise de similaridade (RF08).
 */
@Entity
@Table(name = "versao_documento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersaoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submissao_id", nullable = false)
    private Submissao submissao;

    @Column(name = "numero_versao", nullable = false)
    private int numeroVersao;

    @Column(name = "nome_original", nullable = false, length = 255)
    private String nomeOriginal;

    /** Caminho relativo a raiz de armazenamento configurada. */
    @Column(nullable = false, length = 500)
    private String caminho;

    @Column(name = "tipo_conteudo", nullable = false, length = 120)
    private String tipoConteudo;

    @Column(nullable = false, length = 10)
    private String extensao;

    @Column(name = "tamanho_bytes", nullable = false)
    private long tamanhoBytes;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "texto_extraido")
    private String textoExtraido;

    @Column(name = "data_upload", nullable = false)
    @Builder.Default
    private Instant dataUpload = Instant.now();

    /** RF08: relatorio de similaridade gerado automaticamente para esta versao. */
    @OneToOne(mappedBy = "versaoDocumento", fetch = FetchType.LAZY)
    private RelatorioPlagio relatorioPlagio;
}
