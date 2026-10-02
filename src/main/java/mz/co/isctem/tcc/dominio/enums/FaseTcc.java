package mz.co.isctem.tcc.dominio.enums;

import java.util.Arrays;
import java.util.List;

/**
 * As cinco fases do workflow do TCC (RF05).
 *
 * <p>A Fase 5 e apresentada apenas para cursos da area de Informatica; para os restantes cursos o
 * workflow termina na Fase 4, momento em que o estudante fica apto para defesa.
 */
public enum FaseTcc {
    TEMA_E_PROTOCOLO(1, "Tema e Protocolo", false),
    CAPITULOS_INICIAIS(2, "Capitulos Iniciais", false),
    DESENVOLVIMENTO(3, "Desenvolvimento", false),
    VERSAO_FINAL(4, "Versao Final", false),
    PRONTIDAO_SISTEMA(5, "Prontidao do Sistema", true);

    private final int numero;
    private final String nome;
    private final boolean exclusivaInformatica;

    FaseTcc(int numero, String nome, boolean exclusivaInformatica) {
        this.numero = numero;
        this.nome = nome;
        this.exclusivaInformatica = exclusivaInformatica;
    }

    public int numero() {
        return numero;
    }

    public String nomeApresentacao() {
        return nome;
    }

    public boolean exclusivaInformatica() {
        return exclusivaInformatica;
    }

    public static FaseTcc porNumero(int numero) {
        return Arrays.stream(values())
                .filter(f -> f.numero == numero)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Numero de fase invalido: " + numero));
    }

    /** Fases aplicaveis a um curso, de acordo com a sua area. */
    public static List<FaseTcc> aplicaveis(AreaCurso area) {
        return Arrays.stream(values())
                .filter(f -> !f.exclusivaInformatica || area.temFaseProntidaoSistema())
                .toList();
    }

    /** Numero da ultima fase do workflow para a area indicada. */
    public static int ultimaFase(AreaCurso area) {
        return area.temFaseProntidaoSistema() ? PRONTIDAO_SISTEMA.numero : VERSAO_FINAL.numero;
    }
}
