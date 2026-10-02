package mz.co.isctem.tcc.utilizador.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mz.co.isctem.tcc.dominio.enums.AreaCurso;
import mz.co.isctem.tcc.dominio.enums.PerfilUtilizador;

/**
 * Criacao de conta pelo Administrador (RF01).
 *
 * <p>Para o perfil ESTUDANTE sao obrigatorios {@code codigo}, {@code curso} e {@code area}; para os
 * restantes perfis e obrigatorio o {@code email} institucional, e {@code departamento} no caso do
 * DOCENTE. A validacao cruzada e feita no servico.
 */
public record CriarUtilizadorPedido(
        @NotBlank(message = "O nome e obrigatorio.")
                @Size(max = 150, message = "O nome nao pode exceder 150 caracteres.")
                String nome,
        @NotNull(message = "O perfil e obrigatorio.") PerfilUtilizador tipo,
        @Email(message = "Email institucional invalido.")
                @Size(max = 150, message = "O email nao pode exceder 150 caracteres.")
                String email,
        @Size(max = 50, message = "O codigo nao pode exceder 50 caracteres.") String codigo,
        @Size(max = 120) String curso,
        AreaCurso area,
        @Size(max = 120) String departamento) {}
