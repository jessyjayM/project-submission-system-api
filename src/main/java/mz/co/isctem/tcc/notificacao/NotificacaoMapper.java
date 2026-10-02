package mz.co.isctem.tcc.notificacao;

import mz.co.isctem.tcc.dominio.modelo.Notificacao;
import mz.co.isctem.tcc.notificacao.dto.NotificacaoResposta;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificacaoMapper {

    NotificacaoResposta paraResposta(Notificacao notificacao);
}
