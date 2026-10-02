package mz.co.isctem.tcc.utilizador;

import mz.co.isctem.tcc.dominio.modelo.Docente;
import mz.co.isctem.tcc.dominio.modelo.Estudante;
import mz.co.isctem.tcc.dominio.modelo.Utilizador;
import mz.co.isctem.tcc.utilizador.dto.UtilizadorResposta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/** Conversao das entidades de conta para os DTOs expostos pela API. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UtilizadorMapper {

    @Mapping(target = "curso", ignore = true)
    @Mapping(target = "area", ignore = true)
    @Mapping(target = "departamento", ignore = true)
    UtilizadorResposta paraResposta(Utilizador utilizador);

    /** Variante que junta os dados especificos de Estudante ou de Docente, quando existam. */
    @Mapping(target = "id", source = "utilizador.id")
    @Mapping(target = "nome", source = "utilizador.nome")
    @Mapping(target = "tipo", source = "utilizador.tipo")
    @Mapping(target = "email", source = "utilizador.email")
    @Mapping(target = "codigo", source = "utilizador.codigo")
    @Mapping(target = "estado", source = "utilizador.estado")
    @Mapping(target = "deveAlterarSenha", source = "utilizador.deveAlterarSenha")
    @Mapping(target = "ultimoAcesso", source = "utilizador.ultimoAcesso")
    @Mapping(target = "criadoEm", source = "utilizador.criadoEm")
    @Mapping(target = "curso", source = "estudante.curso")
    @Mapping(target = "area", source = "estudante.area")
    @Mapping(target = "departamento", source = "docente.departamento")
    UtilizadorResposta paraResposta(Utilizador utilizador, Estudante estudante, Docente docente);
}
