package mz.co.isctem.tcc.utilizador.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mz.co.isctem.tcc.dominio.enums.AreaCurso;
import mz.co.isctem.tcc.dominio.enums.PerfilUtilizador;

/** Alteracao de dados e de perfil de uma conta pelo Administrador (RF01). */
public record ActualizarUtilizadorPedido(
        @NotBlank(message = "O nome e obrigatorio.") @Size(max = 150) String nome,
        @NotNull(message = "O perfil e obrigatorio.") PerfilUtilizador tipo,
        @Email(message = "Email institucional invalido.") @Size(max = 150) String email,
        @Size(max = 50) String codigo,
        @Size(max = 120) String curso,
        AreaCurso area,
        @Size(max = 120) String departamento) {}
