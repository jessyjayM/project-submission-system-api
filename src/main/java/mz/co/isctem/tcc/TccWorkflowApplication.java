package mz.co.isctem.tcc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sistema de Gestao de Workflow para Submissao e Aprovacao de Trabalhos de Conclusao de Curso -
 * ISCTEM.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class TccWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TccWorkflowApplication.class, args);
    }
}
