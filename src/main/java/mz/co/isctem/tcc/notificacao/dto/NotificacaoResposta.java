package mz.co.isctem.tcc.notificacao.dto;

import java.time.Instant;
import mz.co.isctem.tcc.dominio.enums.TipoNotificacao;

/** Notificacao in-app apresentada no sino do frontend (RF09). */
public record NotificacaoResposta(
        Long id,
        TipoNotificacao tipo,
        String titulo,
        String conteudo,
        String ligacao,
        boolean lida,
        Instant criadaEm) {}
