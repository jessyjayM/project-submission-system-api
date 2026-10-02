package mz.co.isctem.tcc.plagio.dto;

import java.math.BigDecimal;

/** Documento semelhante listado no relatorio de plagio (RF08). */
public record PlagioCorrespondenciaResposta(
        Long versaoSemelhanteId, String descricao, BigDecimal percentagemSimilaridade) {}
