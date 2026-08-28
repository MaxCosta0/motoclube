package br.com.max.motoclube.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI motoclubeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API do Motoclube")
                        .version("0.0.1-SNAPSHOT")
                        .description("""
                                API do sistema de gestão do motoclube. Cobre hoje o módulo \
                                financeiro: categorias, lançamentos (entradas e saídas), \
                                compras parceladas e relatórios de fluxo de caixa.

                                Erros seguem um contrato único, tratado por um \
                                @RestControllerAdvice central: 400 para payload inválido \
                                (corpo com `campos[]` apontando cada problema), 404 para \
                                recurso inexistente e 422 para violação de regra de negócio \
                                (ex.: pagar um lançamento já pago).""".stripIndent()));
    }
}
