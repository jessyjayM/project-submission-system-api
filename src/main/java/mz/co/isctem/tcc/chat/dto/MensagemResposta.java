package mz.co.isctem.tcc.chat.dto;

import java.time.Instant;
import mz.co.isctem.tcc.dominio.enums.PerfilUtilizador;

/** Mensagem do canal privado estudante-supervisor (RF07). */
public record MensagemResposta(
        Long id,
        Long remetenteId,
        String remetenteNome,
        PerfilUtilizador remetentePerfil,
        String conteudo,
        String anexoNome,
        Long anexoTamanho,
        Instant enviadaEm,
        Instant lidaEm) {

    public boolean temAnexo() {
        return anexoNome != null;
    }
}
